package com.neokeys.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

class NeoKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var english = false
    private var shifted = false
    private var navigationInsetPx = 0

    fun setNavigationInsetPx(inset: Int) {
        val value = max(0, inset)
        if (navigationInsetPx != value) {
            navigationInsetPx = value
            requestLayout()
            invalidate()
        }
    }
    private data class Key(val label: String, val value: String, val rect: RectF)
    private val keys = mutableListOf<Key>()
    private val rowsEs = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l","ñ"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","🌐",",","ES","·",".","↵")
    )
    private val rowsEn = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫"),
        listOf("?123","🌐",",","EN","·",".","↵")
    )
    private val numbers = listOf("1","2","3","4","5","6","7","8","9","0")

    fun toggleLanguage() { english = !english; invalidate() }
    fun toggleShift() { shifted = !shifted; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (resources.displayMetrics.density * 278).toInt() + navigationInsetPx
        setMeasuredDimension(w, resolveSize(desired, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        canvas.drawColor(0xFF101722.toInt())
        paint.color = 0xFF182435.toInt()
        canvas.drawRoundRect(RectF(0f,0f,width.toFloat(),height.toFloat()), 20*d,20*d,paint)
        keys.clear()
        val gap = 5*d
        val pad = 5*d
        val contentHeight = (height - navigationInsetPx).coerceAtLeast(1).toFloat()
        val rowH = contentHeight / 5f
        fun addRow(labels: List<String>, row: Int, numeric: Boolean = false) {
            val total = width - 2*pad
            val unit = (total - gap*(labels.size-1))/labels.size
            labels.forEachIndexed { i, label ->
                val x = pad + i*(unit+gap)
                val y = row*rowH + 4*d
                val rect = RectF(x,y,x+unit,y+rowH-7*d)
                val value = when(label) {
                    "⇧" -> "SHIFT"; "⌫" -> "DELETE"; " " -> "SPACE"; "↵" -> "ENTER"
                    "?123" -> "?123"; "🌐" -> "LANG"; "ES","EN" -> "LANG"
                    else -> label
                }
                keys.add(Key(label,value,rect))
                paint.color = if (label in listOf("⇧","⌫","?123","🌐","↵","ES","EN")) 0xFF34465E.toInt() else 0xCC526984.toInt()
                canvas.drawRoundRect(rect, 9*d,9*d,paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1*d
                paint.color = 0x668FD7FF
                canvas.drawRoundRect(RectF(rect.left+1*d,rect.top+1*d,rect.right-1*d,rect.bottom-1*d),9*d,9*d,paint)
                paint.style = Paint.Style.FILL
                paint.color = 0xFFF3F7FF.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = if (label.length>3) 12*d else 18*d
                val shown = if (shifted && label.length == 1) label.uppercase() else label
                canvas.drawText(shown, rect.centerX(), rect.centerY()-(paint.ascent()+paint.descent())/2, paint)
            }
        }
        addRow(numbers,0,true)
        val rows = if (english) rowsEn else rowsEs
        rows.take(3).forEachIndexed { index, row -> addRow(row,index+1) }
        // Dedicated bottom row with a wide spacebar.
        val bottomY = 4*rowH + 4*d
        val bottomH = rowH - 7*d
        val specs = listOf(
            Triple("?123", 0.15f, "?123"),
            Triple("🌐", 0.11f, "LANG"),
            Triple(",", 0.08f, ","),
            Triple("space", 0.40f, "SPACE"),
            Triple(".", 0.08f, "."),
            Triple("↵", 0.15f, "ENTER")
        )
        val usable = width - 2*pad - gap*(specs.size-1)
        var x = pad
        specs.forEach { (label, fraction, value) ->
            val rectW = usable * fraction
            val rect = RectF(x,bottomY,x+rectW,bottomY+bottomH)
            keys.add(Key(label,value,rect))
            paint.color = if (value == "SPACE") 0xCC526984.toInt() else 0xFF34465E.toInt()
            canvas.drawRoundRect(rect,9*d,9*d,paint)
            paint.color = 0xFFF3F7FF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (label == "space") 11*d else 16*d
            val shown = if (label == "space") if (english) "English" else "Español" else label
            canvas.drawText(shown,rect.centerX(),rect.centerY()-(paint.ascent()+paint.descent())/2,paint)
            x += rectW + gap
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_UP) {
            val key = keys.asReversed().firstOrNull { it.rect.contains(event.x,event.y) }
            if (key != null) {
                when (key.value) {
                    "SHIFT" -> { shifted = !shifted; invalidate() }
                    "LANG" -> { english = !english; invalidate() }
                    "SPACE" -> onKey("SPACE")
                    "DELETE" -> onKey("DELETE")
                    "ENTER" -> onKey("ENTER")
                    else -> {
                        val out = if (shifted) key.value.uppercase() else key.value
                        onKey(out)
                        if (shifted) { shifted = false; invalidate() }
                    }
                }
            }
        }
        return true
    }
}
