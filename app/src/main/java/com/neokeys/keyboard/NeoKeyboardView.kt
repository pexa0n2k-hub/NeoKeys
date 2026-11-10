package com.neokeys.keyboard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View

class NeoKeyboardView(context: Context, private val onKey: (String) -> Unit) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val handler = Handler(Looper.getMainLooper())

    private var backgroundBitmap: Bitmap? = null
    private var loadedBackgroundUri: String? = null
    private var english = false
    private var shifted = false
    private var symbolMode = false
    private var pressedValue: String? = null
    private var pressedRect: RectF? = null
    private var deleteRepeating = false

    private data class Key(val label: String, val value: String, val rect: RectF)
    private val keys = mutableListOf<Key>()

    private data class Theme(
        val background: Int, val panel: Int, val key: Int,
        val special: Int, val accent: Int, val text: Int
    )

    private fun currentTheme(): Theme {
        val prefs = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE)
        val base = when (prefs.getString("theme", "violet")) {
            "violet" -> Theme(0xFF10091D.toInt(), 0xCC25133F.toInt(), 0xCC572B91.toInt(), 0xFF713BC0.toInt(), 0xFFB45CFF.toInt(), 0xFFFFFFFF.toInt())
            "cyber" -> Theme(0xFF050D18.toInt(), 0xCC0B1C2B.toInt(), 0xCC123C55.toInt(), 0xFF174E68.toInt(), 0xFF32E8FF.toInt(), 0xFFF5FFFF.toInt())
            "ivory" -> Theme(0xFFF0EAE0.toInt(), 0xFFE6DED2.toInt(), 0xFFF9F5ED.toInt(), 0xFFD9C2A5.toInt(), 0xFFB88B5B.toInt(), 0xFF24211D.toInt())
            "emerald" -> Theme(0xFF061812.toInt(), 0xCC0B2B20.toInt(), 0xCC12634B.toInt(), 0xFF087B61.toInt(), 0xFF35E6B0.toInt(), 0xFFEFFFF7.toInt())
            "crimson" -> Theme(0xFF160609.toInt(), 0xCC2B0B10.toInt(), 0xCC5A111B.toInt(), 0xFF8A1725.toInt(), 0xFFFF3B4E.toInt(), 0xFFFFF4F4.toInt())
            "sakura" -> Theme(0xFF2A1427.toInt(), 0xCCF4C8E7.toInt(), 0xCCF7D9EE.toInt(), 0xFFEBA9D5.toInt(), 0xFFE66BC2.toInt(), 0xFF512A49.toInt())
            "holographic" -> Theme(0xFF06112B.toInt(), 0xCC102B70.toInt(), 0xCC1259B8.toInt(), 0xFF244ED0.toInt(), 0xFF31D8FF.toInt(), 0xFFFFFFFF.toInt())
            "titanium" -> Theme(0xFF080B10.toInt(), 0xCC1B222D.toInt(), 0xCC414B5A.toInt(), 0xFF596577.toInt(), 0xFFBFC9D8.toInt(), 0xFFF5F7FA.toInt())
            else -> Theme(0xFF10091D.toInt(), 0xCC25133F.toInt(), 0xCC572B91.toInt(), 0xFF713BC0.toInt(), 0xFFB45CFF.toInt(), 0xFFFFFFFF.toInt())
        }
        return base.copy(accent = prefs.getInt("accent_color", base.accent))
    }

    private val keyboardHeightDp: Int
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getInt("keyboard_height_dp", 278)
    private val hapticsEnabled: Boolean
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getBoolean("haptics_enabled", true)
    private val keyOpacity: Int
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getInt("key_opacity", 90).coerceIn(40, 100)
    private val glassIntensity: Int
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getInt("glass_intensity", 70).coerceIn(0, 100)

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
    fun refreshAppearance() { backgroundBitmap = null; loadedBackgroundUri = null; invalidate(); requestLayout() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val desired = (resources.displayMetrics.density * keyboardHeightDp).toInt()
        setMeasuredDimension(w, resolveSize(desired, heightMeasureSpec))
    }

    private fun loadBackground(): Bitmap? {
        val prefs = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE)
        val uriString = prefs.getString("keyboard_background_uri", null)
        if (uriString != loadedBackgroundUri) {
            backgroundBitmap = null
            loadedBackgroundUri = uriString
            if (uriString != null) {
                try {
                    context.contentResolver.openInputStream(android.net.Uri.parse(uriString))?.use {
                        backgroundBitmap = BitmapFactory.decodeStream(it)
                    }
                } catch (_: Exception) { backgroundBitmap = null }
            }
        }
        return backgroundBitmap
    }

    private fun drawPhotoBackground(canvas: Canvas) {
        val bitmap = loadBackground() ?: return
        val sourceRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetRatio = width.toFloat() / height.coerceAtLeast(1).toFloat()
        val src = if (sourceRatio > targetRatio) {
            val cropWidth = (bitmap.height * targetRatio).toInt().coerceAtLeast(1)
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / targetRatio).toInt().coerceAtLeast(1)
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }
        canvas.drawBitmap(bitmap, src, RectF(0f, 0f, width.toFloat(), height.toFloat()), imagePaint)
        paint.style = Paint.Style.FILL
        paint.color = 0xFF07111F.toInt()
        paint.alpha = (155 - glassIntensity * 0.85f).toInt().coerceIn(35, 155)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.alpha = 255
    }

    private val keyShape: String
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getString("key_shape", "rounded") ?: "rounded"
    private val pressEffect: String
        get() = context.getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE).getString("press_effect", "glow") ?: "glow"

    private fun drawPremiumKey(canvas: Canvas, rect: RectF, radius: Float, baseColor: Int, pressed: Boolean) {
        val top = if (pressed) currentTheme().accent else lighten(baseColor, 0.22f)
        val bottom = darken(baseColor, 0.18f)
        paint.shader = android.graphics.LinearGradient(
            rect.left, rect.top, rect.left, rect.bottom, top, bottom,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        paint.alpha = 255
        drawKey(canvas, rect, radius)
        paint.shader = null
        // Refined highlight line along the upper edge.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = resources.displayMetrics.density
        paint.color = 0xBFFFFFFF.toInt()
        paint.alpha = 70
        canvas.drawRoundRect(RectF(rect.left+1f, rect.top+1f, rect.right-1f, rect.bottom-1f),
            radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = 255
    }

    private fun lighten(color: Int, amount: Float): Int {
        fun ch(shift: Int) = (((color shr shift) and 0xFF) + (255 - ((color shr shift) and 0xFF))*amount).toInt().coerceIn(0,255)
        return android.graphics.Color.argb(255, ch(16), ch(8), ch(0))
    }

    private fun darken(color: Int, amount: Float): Int {
        fun ch(shift: Int) = (((color shr shift) and 0xFF)*(1f-amount)).toInt().coerceIn(0,255)
        return android.graphics.Color.argb(255, ch(16), ch(8), ch(0))
    }

    private fun drawKey(canvas: Canvas, rect: RectF, radius: Float) {
        val path = android.graphics.Path()
        when (keyShape) {
            "square" -> canvas.drawRect(rect, paint)
            "pill" -> canvas.drawRoundRect(rect, rect.height()/2f, rect.height()/2f, paint)
            "super" -> canvas.drawRoundRect(rect, radius*2.2f, radius*2.2f, paint)
            "hexagon" -> {
                val cut = rect.width()*0.16f
                path.moveTo(rect.left+cut, rect.top); path.lineTo(rect.right-cut, rect.top)
                path.lineTo(rect.right, rect.centerY()); path.lineTo(rect.right-cut, rect.bottom)
                path.lineTo(rect.left+cut, rect.bottom); path.lineTo(rect.left, rect.centerY())
                path.close(); canvas.drawPath(path, paint)
            }
            "diagonal", "gamer" -> {
                val cut = if (keyShape == "gamer") rect.width()*0.12f else rect.width()*0.07f
                path.moveTo(rect.left+cut, rect.top); path.lineTo(rect.right, rect.top)
                path.lineTo(rect.right-cut, rect.bottom); path.lineTo(rect.left, rect.bottom)
                path.close(); canvas.drawPath(path, paint)
            }
            else -> canvas.drawRoundRect(rect, if (keyShape == "neon") radius*1.5f else radius, if (keyShape == "neon") radius*1.5f else radius, paint)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val d = resources.displayMetrics.density
        val theme = currentTheme()
        val opacity = (keyOpacity * 255 / 100).coerceIn(0, 255)

        canvas.drawColor(theme.background)
        drawPhotoBackground(canvas)
        paint.style = Paint.Style.FILL
        paint.color = theme.panel
        paint.alpha = (110 + glassIntensity * 1.35f).toInt().coerceIn(110, 245)
        canvas.drawRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), 20*d, 20*d, paint)
        paint.alpha = 255

        keys.clear()
        val gap = 5*d
        val pad = 5*d
        val toolbarH = 34*d
        val contentTop = toolbarH
        val rowH = (height - toolbarH) / 5f

        // Barra de acceso rápido integrada en el teclado.
        val toolbarRect = RectF(pad, 3*d, width-pad, toolbarH-2*d)
        paint.style = Paint.Style.FILL
        paint.color = theme.panel
        paint.alpha = 235
        canvas.drawRoundRect(toolbarRect, 14*d, 14*d, paint)
        paint.alpha = 255
        val toolbarItems = listOf(
            Triple("⚙", 0.16f, "SETTINGS"),
            Triple("🎨", 0.16f, "SETTINGS"),
            Triple("▦", 0.16f, "SETTINGS"),
            Triple("🌐", 0.16f, "LANG"),
            Triple("⌫", 0.16f, "DELETE")
        )
        val itemW = toolbarRect.width() / toolbarItems.size
        toolbarItems.forEachIndexed { index, item ->
            val (label, _, value) = item
            val rect = RectF(toolbarRect.left + index*itemW, toolbarRect.top,
                toolbarRect.left + (index+1)*itemW, toolbarRect.bottom)
            keys.add(Key(label, value, rect))
            paint.color = if (pressedValue == value && pressedRect?.left == rect.left) theme.accent else theme.text
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 18*d
            canvas.drawText(label, rect.centerX(), rect.centerY() - (paint.ascent()+paint.descent())/2, paint)
        }

        fun addRow(labels: List<String>, row: Int) {
            val total = width - 2*pad
            val unit = (total - gap*(labels.size-1))/labels.size
            labels.forEachIndexed { i, label ->
                val x = pad + i*(unit+gap)
                val y = contentTop + row*rowH + 4*d
                val rect = RectF(x, y, x+unit, y+rowH-7*d)
                val value = when (label) {
                    "⇧" -> "SHIFT"
                    "⌫" -> "DELETE"
                    "↵" -> "ENTER"
                    else -> label
                }
                keys.add(Key(label, value, rect))
                val isPressed = pressedValue == value && pressedRect?.let { it.left == rect.left && it.top == rect.top } == true
                paint.color = when {
                    isPressed -> theme.accent
                    label in listOf("⇧","⌫","↵") -> theme.special
                    else -> theme.key
                }
                paint.alpha = if (isPressed) 255 else opacity
                drawPremiumKey(canvas, rect, 9*d, paint.color, isPressed)
                paint.alpha = 255
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = (1f + glassIntensity / 100f)*d
                paint.color = theme.accent
                paint.alpha = if (isPressed) 230 else 35 + glassIntensity
                drawKey(canvas, RectF(rect.left+d, rect.top+d, rect.right-d, rect.bottom-d), 9*d)
                if (isPressed) {
                    paint.style = Paint.Style.STROKE
                    paint.color = theme.accent
                    paint.strokeWidth = 2.5f*d
                    paint.alpha = 220
                    when (pressEffect) {
                        "pulse" -> canvas.drawRoundRect(RectF(rect.left-2*d, rect.top-2*d, rect.right+2*d, rect.bottom+2*d), 11*d, 11*d, paint)
                        "wave" -> canvas.drawCircle(rect.centerX(), rect.centerY(), rect.height()*0.42f, paint)
                        "expand" -> canvas.drawRoundRect(RectF(rect.left-4*d, rect.top-4*d, rect.right+4*d, rect.bottom+4*d), 13*d, 13*d, paint)
                        else -> {
                            paint.strokeWidth = 4*d
                            canvas.drawRoundRect(rect, 10*d, 10*d, paint)
                        }
                    }
                }
                paint.style = Paint.Style.FILL
                paint.alpha = 255
                paint.color = theme.text
                paint.textAlign = Paint.Align.CENTER
                paint.textSize = if (label.length > 3) 12*d else 18*d
                val shown = if (shifted && label.length == 1 && !symbolMode) label.uppercase() else label
                canvas.drawText(shown, rect.centerX(), rect.centerY() - (paint.ascent()+paint.descent())/2, paint)
            }
        }

        addRow(numbers, 0)
        val activeRows = when { symbolMode -> symbolRows; english -> rowsEn; else -> rowsEs }
        activeRows.forEachIndexed { i, row -> addRow(row, i+1) }

        val bottomY = contentTop + 4*rowH + 4*d
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
            val isPressed = pressedValue == value && pressedRect?.let { it.left == rect.left && it.top == rect.top } == true
            paint.color = when {
                isPressed -> theme.accent
                value == "SPACE" -> theme.key
                else -> theme.special
            }
            paint.alpha = if (isPressed) 255 else opacity
            drawPremiumKey(canvas, rect, 9*d, paint.color, isPressed)
            paint.alpha = 255
            if (isPressed) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2*d
                paint.color = theme.accent
                drawKey(canvas, rect, 9*d)
                paint.style = Paint.Style.FILL
            }
            paint.color = theme.text
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = if (label == "space") 11*d else 16*d
            val shown = if (label == "space") { if (english) "English" else "Español" } else label
            canvas.drawText(shown, rect.centerX(), rect.centerY() - (paint.ascent()+paint.descent())/2, paint)
            x += rectW + gap
        }
    }

    private fun startDeleteRepeat() {
        handler.removeCallbacks(deleteRepeat)
        deleteRepeating = false
        handler.postDelayed(deleteRepeat, 400L)
    }
    private fun stopDeleteRepeat() { handler.removeCallbacks(deleteRepeat) }

    private var downHandled = false

    private fun dispatchKey(value: String) {
        when (value) {
            "SHIFT" -> { shifted = !shifted; invalidate() }
            "LANG" -> { english = !english; invalidate() }
            "MODE" -> { symbolMode = !symbolMode; shifted = false; invalidate() }
            "DELETE" -> onKey("DELETE")
            "SPACE", "ENTER" -> onKey(value)
            "SETTINGS" -> onKey("SETTINGS")
            else -> {
                onKey(if (shifted && !symbolMode) value.uppercase() else value.trim())
                if (shifted) { shifted = false; invalidate() }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val key = keys.asReversed().firstOrNull { it.rect.contains(event.x, event.y) }
                pressedValue = key?.value
                pressedRect = key?.rect?.let { RectF(it) }
                if (key != null && hapticsEnabled) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                downHandled = false
                if (key?.value == "DELETE") {
                    startDeleteRepeat()
                } else if (key != null) {
                    dispatchKey(key.value)
                    downHandled = true
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                val key = keys.asReversed().firstOrNull { it.rect.contains(event.x, event.y) }
                stopDeleteRepeat()
                pressedValue = null
                pressedRect = null
                invalidate()
                if (key?.value == "DELETE" && !deleteRepeating) onKey("DELETE")
                deleteRepeating = false
                downHandled = false
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                stopDeleteRepeat()
                pressedValue = null
                pressedRect = null
                deleteRepeating = false
                downHandled = false
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
