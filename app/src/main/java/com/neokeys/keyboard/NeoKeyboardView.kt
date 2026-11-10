package com.neokeys.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class NeoKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())

    private var english = false
    private var shifted = false
    private var symbolMode = false
    private var pressedValue: String? = null
    private var pressedRect: RectF? = null
    private var deleteRepeating = false

    private data class Key(val label: String, val value: String, val rect: RectF)
    private val keys = mutableListOf<Key>()

    private val rowsEs = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l","ñ"),
        listOf("⇧","z","x","c","v","b","n","m","⌫")
    )
    private val rowsEn = listOf(
        listOf("q","w","e","r","t","y","u","i","o","p"),
        listOf("a","s","d","f","g","h","j","k","l"),
        listOf("⇧","z","x","c","v","b","n","m","⌫")
    )
    private val symbolRows = listOf(
        listOf("@","#","$","%","&","-","+","(",")","/"),
        listOf("*","\"","'",":",";","!","?","¿","¡","_"),
        listOf("⇧","=","[","]","{","}","\\","~","⌫")
    )
    private val numbers = listOf("1","2","3","4","5","6","7","8","9","0")

    private val keyboardHeightDp: Int
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE)
            .getInt("keyboard_height_dp", 278)

    private val hapticsEnabled: Boolean
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE)
            .getBoolean("haptics_enabled", true)

    private val deleteRepeat = object : Runnable {
        override fun run() {
            if (pressedValue == "DELETE") {
                onKey("DELETE")
                deleteRepeating = true
                handler.postDelayed(this, 65L)
            }
        }
    }

    fun toggleLanguage() { english = !english; invalidate() }
    fun toggleShift() { shifted = !shifted; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (resources.displayMetrics.density * keyboardHeightDp).toInt()
        setMeasuredDimension(w, resolveSize(desired, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        canvas.drawColor(0xFF101722.toInt())
        paint.color = 0xFF182435.toInt()
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 20*d, 20*d, paint)

        keys.clear()
        val gap = 5*d
        val pad = 5*d
        val rowH = height / 5f

        fun addRow(labels: List<String>, row: Int) {
            val total = width - 2*pad
            val unit = (total - gap*(labels.size-1))/labels.size
            labels.forEachIndexed { i, label ->
                val x = pad + i*(unit+gap)
                val y = row*rowH + 4*d
                val rect = RectF(x, y, x+unit, y+rowH-7*d)
                val value = when (label) {
                    "⇧" -> "SHIFT"
                    "⌫" -> "DELETE"
                    "↵" -> "ENTER"
                    else -> label
                }
                keys.add(Key(label, value, rect))

                val isPressed = pressedValue == value && pressedRect?.let {
                    it.left == rect.left && it.top == rect.top
                } == true
                paint.color = when {
                    isPressed -> 0xFF83BCEB.toInt()
                    label in listOf("⇧","⌫","↵") -> 0xFF34465E.toInt()
                    else -> 0xCC526984.toInt()
                }
                canvas.drawRoundRect(rect, 9*d, 9*d, paint)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = if (isPressed) 2*d else 1*d
                paint.color = if (isPressed) 0xFFB9E5FF.toInt() else 0x668FD7FF
                canvas.drawRoundRect(
                    RectF(rect.left+d, rect.top+d, rect.right-d, rect.bottom-d),
                    9*d, 9*d, paint
                )
                paint.style = Paint.Style.FILL
                paint.color = 0xFFF3F7FF.toInt()
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = if (label.length > 3) 12*d else 18*d
                val shown = if (shifted && label.length == 1 && !symbolMode) label.uppercase() else label
                canvas.drawText(shown, rect.centerX(),
                    rect.centerY() - (paint.ascent()+paint.descent())/2, paint)
            }
        }

        addRow(numbers, 0)
        val activeRows = when {
            symbolMode -> symbolRows
            english -> rowsEn
            else -> rowsEs
        }
        activeRows.forEachIndexed { i, row -> addRow(row, i+1) }

        val bottomY = 4*rowH + 4*d
        val bottomH = rowH - 7*d
        val modeLabel = if (symbolMode) "ABC" else "?123"
        val specs = listOf(
            Triple(modeLabel, 0.15f, "MODE"), Triple("🌐", 0.11f, "LANG"),
            Triple(",", 0.08f, ","), Triple("space", 0.40f, "SPACE"),
            Triple(".", 0.08f, "."), Triple("↵", 0.15f, "ENTER")
        )
        val usable = width - 2*pad - gap*(specs.size-1)
        var x = pad
        specs.forEach { (label, fraction, value) ->
            val rectW = usable*fraction
            val rect = RectF(x, bottomY, x+rectW, bottomY+bottomH)
            keys.add(Key(label, value, rect))
            val isPressed = pressedValue == value && pressedRect?.let {
                it.left == rect.left && it.top == rect.top
            } == true
            paint.color = when {
                isPressed -> 0xFF83BCEB.toInt()
                value == "SPACE" -> 0xCC526984.toInt()
                else -> 0xFF34465E.toInt()
            }
            canvas.drawRoundRect(rect, 9*d, 9*d, paint)
            if (isPressed) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2*d
                paint.color = 0xFFB9E5FF.toInt()
                canvas.drawRoundRect(rect, 9*d, 9*d, paint)
                paint.style = Paint.Style.FILL
            }
            paint.color = 0xFFF3F7FF.toInt()
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (label == "space") 11*d else 16*d
            val shown = if (label == "space") { if (english) "English" else "Español" } else label
            canvas.drawText(shown, rect.centerX(),
                rect.centerY() - (paint.ascent()+paint.descent())/2, paint)
            x += rectW + gap
        }
    }

    private fun startDeleteRepeat() {
        handler.removeCallbacks(deleteRepeat)
        deleteRepeating = false
        handler.postDelayed(deleteRepeat, 400L)
    }

    private fun stopDeleteRepeat() {
        handler.removeCallbacks(deleteRepeat)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val key = keys.asReversed().firstOrNull { it.rect.contains(event.x, event.y) }
                pressedValue = key?.value
                pressedRect = key?.rect?.let { RectF(it) }
                if (key != null && hapticsEnabled) {
                    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                }
                if (key?.value == "DELETE") startDeleteRepeat()
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val key = keys.asReversed().firstOrNull { it.rect.contains(event.x, event.y) }
                stopDeleteRepeat()
                val wasDelete = pressedValue == "DELETE"
                pressedValue = null
                pressedRect = null
                invalidate()
                if (key != null) when (key.value) {
                    "SHIFT" -> { shifted = !shifted; invalidate() }
                    "LANG" -> { english = !english; invalidate() }
                    "MODE" -> { symbolMode = !symbolMode; shifted = false; invalidate() }
                    "DELETE" -> if (!deleteRepeating) onKey("DELETE")
                    "SPACE", "ENTER" -> onKey(key.value)
                    else -> {
                        onKey(if (shifted && !symbolMode) key.value.uppercase() else key.value.trim())
                        if (shifted) { shifted = false; invalidate() }
                    }
                }
                deleteRepeating = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                stopDeleteRepeat()
                pressedValue = null
                pressedRect = null
                deleteRepeating = false
                invalidate()
                return true
            }
        }
        return true
    }

    override fun onDetachedFromWindow() {
        stopDeleteRepeat()
        super.onDetachedFromWindow()
    }
}
