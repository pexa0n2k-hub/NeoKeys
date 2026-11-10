package com.neokeys.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE) }
    private lateinit var backgroundInfo: TextView
    private lateinit var themeInfo: TextView
    private lateinit var shapeInfo: TextView
    private lateinit var effectInfo: TextView
    private lateinit var accentInfo: TextView

    private val themes = listOf(
        "Violet Glass" to "violet", "Cyber Pulse" to "cyber",
        "Ivory Minimal" to "ivory", "Emerald OS" to "emerald",
        "Crimson" to "crimson", "Sakura" to "sakura",
        "Holographic" to "holographic", "Titanium" to "titanium"
    )
    private val shapes = listOf(
        "Cuadrada" to "square", "Redondeada" to "rounded",
        "Píldora" to "pill", "Súper redonda" to "super",
        "Hexagonal" to "hexagon", "Diagonal" to "diagonal",
        "Corte gamer" to "gamer", "Neón" to "neon"
    )
    private val effects = listOf(
        "Pulso" to "pulse", "Onda" to "wave",
        "Brillo" to "glow", "Expansión" to "expand"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFF090D18.toInt())
        }
        fun label(value: String) = TextView(this).apply {
            text = value; textSize = 14f
            setTextColor(0xFF69DFFF.toInt()); setPadding(0, 18, 0, 8)
        }
        fun info(value: String) = TextView(this).apply {
            text = value; textSize = 14f
            setTextColor(0xFFEAF2FF.toInt()); setPadding(0, 3, 0, 7)
        }
        fun row(values: List<Pair<String, () -> Unit>>) = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            values.forEach { (caption, action) ->
                addView(Button(this@MainActivity).apply {
                    text = caption
                    textSize = 11f
                    setOnClickListener { action() }
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            }
        }
        fun addChoices(items: List<Pair<String, String>>, selectedKey: String, target: TextView, title: String) {
            items.chunked(2).forEach { group ->
                root.addView(row(group.map { (caption, value) ->
                    caption to {
                        prefs.edit().putString(selectedKey, value).apply()
                        target.text = "$title: $caption"
                    }
                }))
            }
        }

        root.addView(TextView(this).apply {
            text = "NeoKeys"; textSize = 32f; setTextColor(0xFFF2F5FF.toInt())
        })
        root.addView(TextView(this).apply {
            text = "FUTURISTIC COLLECTION • V1.6\n8 temas, formas y efectos para tu teclado"
            textSize = 15f; setTextColor(0xFFB8C9E8.toInt()); setPadding(0, 10, 0, 18)
        })
        root.addView(Button(this).apply {
            text = "1. Activar NeoKeys"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        })
        root.addView(Button(this).apply {
            text = "2. Seleccionar NeoKeys"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        })

        root.addView(label("COLECCIÓN DE TEMAS"))
        themeInfo = info("")
        fun updateTheme() {
            themeInfo.text = "Tema actual: " + (themes.firstOrNull { it.second == prefs.getString("theme", "violet") }?.first ?: "Violet Glass")
        }
        updateTheme(); root.addView(themeInfo)
        addChoices(themes, "theme", themeInfo, "Tema")

        root.addView(label("FORMAS DE TECLAS"))
        shapeInfo = info("")
        fun updateShape() {
            shapeInfo.text = "Forma: " + (shapes.firstOrNull { it.second == prefs.getString("key_shape", "rounded") }?.first ?: "Redondeada")
        }
        updateShape(); root.addView(shapeInfo)
        addChoices(shapes, "key_shape", shapeInfo, "Forma")

        root.addView(label("EFECTOS AL PRESIONAR"))
        effectInfo = info("")
        fun updateEffect() {
            effectInfo.text = "Efecto: " + (effects.firstOrNull { it.second == prefs.getString("press_effect", "glow") }?.first ?: "Brillo")
        }
        updateEffect(); root.addView(effectInfo)
        addChoices(effects, "press_effect", effectInfo, "Efecto")

        root.addView(label("FONDO PERSONALIZADO"))
        backgroundInfo = info(if (prefs.getString("keyboard_background_uri", null) == null) "Sin fotografía seleccionada" else "Fotografía personalizada activa")
        root.addView(backgroundInfo)
        root.addView(row(listOf(
            "Elegir foto" to { openImagePicker() },
            "Quitar foto" to {
                prefs.edit().remove("keyboard_background_uri").apply()
                backgroundInfo.text = "Sin fotografía seleccionada"
            }
        )))

        root.addView(label("COLOR DE ACENTO"))
        val accents = listOf(
            "Cian" to 0xFF32E8FF.toInt(), "Morado" to 0xFFB45CFF.toInt(),
            "Rosa" to 0xFFFF55D8.toInt(), "Verde" to 0xFF35E6B0.toInt(),
            "Rojo" to 0xFFFF3B4E.toInt(), "Dorado" to 0xFFFFC98A.toInt()
        )
        accentInfo = info("")
        fun updateAccent() {
            accentInfo.text = "Acento: " + (accents.firstOrNull { it.second == prefs.getInt("accent_color", 0xFFB45CFF.toInt()) }?.first ?: "Personalizado")
        }
        updateAccent(); root.addView(accentInfo)
        accents.chunked(3).forEach { group ->
            root.addView(row(group.map { (name, color) ->
                name to { prefs.edit().putInt("accent_color", color).apply(); updateAccent() }
            }))
        }

        root.addView(label("INTENSIDAD LIQUID GLASS"))
        val glassInfo = info("")
        fun updateGlass() { glassInfo.text = "Intensidad: ${prefs.getInt("glass_intensity", 70)}%" }
        updateGlass(); root.addView(glassInfo)
        root.addView(row(listOf(40, 70, 100).map { v ->
            "$v%" to { prefs.edit().putInt("glass_intensity", v).apply(); updateGlass() }
        }))

        root.addView(label("TRANSPARENCIA"))
        val opacityInfo = info("")
        fun updateOpacity() { opacityInfo.text = "Opacidad: ${prefs.getInt("key_opacity", 90)}%" }
        updateOpacity(); root.addView(opacityInfo)
        root.addView(row(listOf(60, 75, 90, 100).map { v ->
            "$v%" to { prefs.edit().putInt("key_opacity", v).apply(); updateOpacity() }
        }))

        root.addView(label("ALTURA"))
        val heightInfo = info("")
        fun updateHeight() { heightInfo.text = "Altura: ${prefs.getInt("keyboard_height_dp", 278)} dp" }
        updateHeight(); root.addView(heightInfo)
        root.addView(row(listOf(250, 278, 310, 340).map { v ->
            "$v dp" to { prefs.edit().putInt("keyboard_height_dp", v).apply(); updateHeight() }
        }))

        root.addView(Button(this).apply {
            text = if (prefs.getBoolean("haptics_enabled", true)) "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            setOnClickListener {
                val enabled = !prefs.getBoolean("haptics_enabled", true)
                prefs.edit().putBoolean("haptics_enabled", enabled).apply()
                text = if (enabled) "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            }
        })
        root.addView(TextView(this).apply {
            text = "Los cambios se aplican al volver a abrir el teclado. Se conservan idiomas, símbolos y borrado continuo."
            textSize = 13f; setTextColor(0xFFB8C9E8.toInt()); setPadding(0, 18, 0, 8)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 501)
    }

    @Deprecated("Deprecated in Android API; retained for compatibility with minSdk 23")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 501 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { }
            prefs.edit().putString("keyboard_background_uri", uri.toString()).apply()
            backgroundInfo.text = "Fotografía personalizada activa"
        }
    }
}
