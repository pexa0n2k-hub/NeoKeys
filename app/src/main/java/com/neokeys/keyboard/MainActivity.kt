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
    private lateinit var accentInfo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 32, 28, 32)
            setBackgroundColor(0xFF101722.toInt())
        }

        fun label(value: String) = TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFF8FD7FF.toInt())
            setPadding(0, 18, 0, 8)
        }

        fun info(value: String) = TextView(this).apply {
            text = value
            textSize = 14f
            setTextColor(0xFFEAF2FF.toInt())
            setPadding(0, 3, 0, 7)
        }

        fun buttonRow(values: List<Pair<String, () -> Unit>>) =
            LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                values.forEach { (caption, action) ->
                    addView(Button(this@MainActivity).apply {
                        text = caption
                        setOnClickListener { action() }
                    }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                }
            }

        root.addView(TextView(this).apply {
            text = "NeoKeys"
            textSize = 32f
            setTextColor(0xFFEAF2FF.toInt())
        })
        root.addView(TextView(this).apply {
            text = "LIQUID GLASS • V1.5\nPersonaliza tu teclado a tu estilo"
            textSize = 15f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 10, 0, 18)
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

        root.addView(label("FONDO PERSONALIZADO"))
        backgroundInfo = info(if (prefs.getString("keyboard_background_uri", null) == null)
            "Sin fotografía seleccionada" else "Fotografía personalizada activa")
        root.addView(backgroundInfo)
        root.addView(buttonRow(listOf(
            "Elegir foto" to { openImagePicker() },
            "Quitar foto" to {
                prefs.edit().remove("keyboard_background_uri").apply()
                backgroundInfo.text = "Sin fotografía seleccionada"
            }
        )))

        root.addView(label("TEMAS"))
        val themeInfo = info("")
        fun updateThemeInfo() {
            themeInfo.text = "Tema actual: " + when (prefs.getString("theme", "ocean")) {
                "midnight" -> "Midnight"
                "rose" -> "Rose"
                "emerald" -> "Emerald"
                else -> "Ocean"
            }
        }
        updateThemeInfo()
        root.addView(themeInfo)
        root.addView(buttonRow(listOf(
            "Ocean" to { setTheme("ocean"); updateThemeInfo() },
            "Midnight" to { setTheme("midnight"); updateThemeInfo() }
        )))
        root.addView(buttonRow(listOf(
            "Rose" to { setTheme("rose"); updateThemeInfo() },
            "Emerald" to { setTheme("emerald"); updateThemeInfo() }
        )))

        root.addView(label("COLOR DE ACENTO"))
        val accents = listOf(
            "Azul" to 0xFF8FD7FF.toInt(),
            "Morado" to 0xFFB69CFF.toInt(),
            "Rosa" to 0xFFFF8FCB.toInt(),
            "Verde" to 0xFF75E6B1.toInt(),
            "Naranja" to 0xFFFFB36B.toInt()
        )
        accentInfo = info("")
        fun updateAccent() {
            val selected = prefs.getInt("accent_color", 0xFF8FD7FF.toInt())
            accentInfo.text = "Color seleccionado: " +
                (accents.firstOrNull { it.second == selected }?.first ?: "Personalizado")
        }
        updateAccent()
        root.addView(accentInfo)
        root.addView(buttonRow(accents.take(3).map { (name, color) ->
            name to { prefs.edit().putInt("accent_color", color).apply(); updateAccent() }
        }))
        root.addView(buttonRow(accents.drop(3).map { (name, color) ->
            name to { prefs.edit().putInt("accent_color", color).apply(); updateAccent() }
        }))

        root.addView(label("INTENSIDAD LIQUID GLASS"))
        val glassInfo = info("")
        fun updateGlass() {
            glassInfo.text = "Intensidad del cristal: ${prefs.getInt("glass_intensity", 70)}%"
        }
        updateGlass()
        root.addView(glassInfo)
        root.addView(buttonRow(listOf(
            "Suave 40%" to 40,
            "Medio 70%" to 70,
            "Intenso 100%" to 100
        ).map { (name, value) ->
            name to { prefs.edit().putInt("glass_intensity", value).apply(); updateGlass() }
        }))

        root.addView(label("TRANSPARENCIA DE TECLAS"))
        val opacityInfo = info("")
        fun updateOpacity() { opacityInfo.text = "Opacidad: ${prefs.getInt("key_opacity", 90)}%" }
        updateOpacity()
        root.addView(opacityInfo)
        root.addView(buttonRow(listOf(60, 75, 90, 100).map { value ->
            "$value%" to { prefs.edit().putInt("key_opacity", value).apply(); updateOpacity() }
        }))

        root.addView(label("ALTURA DEL TECLADO"))
        val heightInfo = info("")
        fun updateHeight() { heightInfo.text = "Altura: ${prefs.getInt("keyboard_height_dp", 278)} dp" }
        updateHeight()
        root.addView(heightInfo)
        root.addView(buttonRow(listOf(250, 278, 310, 340).map { value ->
            value.toString() to { prefs.edit().putInt("keyboard_height_dp", value).apply(); updateHeight() }
        }))

        root.addView(Button(this).apply {
            text = if (prefs.getBoolean("haptics_enabled", true))
                "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            setOnClickListener {
                val enabled = !prefs.getBoolean("haptics_enabled", true)
                prefs.edit().putBoolean("haptics_enabled", enabled).apply()
                text = if (enabled) "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            }
        })
        root.addView(TextView(this).apply {
            text = "Los cambios se aplican al volver a abrir el teclado. Tus idiomas, símbolos, animación y borrado continuo se mantienen."
            textSize = 13f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 18, 0, 8)
        })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    private fun setTheme(value: String) {
        prefs.edit().putString("theme", value).apply()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        startActivityForResult(intent, 501)
    }

    @Deprecated("Deprecated in Android API; retained for compatibility with minSdk 23")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 501 && resultCode == RESULT_OK) {
            val uri: Uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) { }
            prefs.edit().putString("keyboard_background_uri", uri.toString()).apply()
            backgroundInfo.text = "Fotografía personalizada activa"
        }
    }
}
