package com.neokeys.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
    }

    private fun buildScreen() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
            setBackgroundColor(0xFF101722.toInt())
        }
        val title = TextView(this).apply {
            text = "NeoKeys"
            textSize = 32f
            setTextColor(0xFFEAF2FF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "LIQUID GLASS • V1.4\nPersonaliza la apariencia de tu teclado"
            textSize = 15f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 12, 0, 20)
        }
        val enable = Button(this).apply {
            text = "1. Activar NeoKeys"
            setOnClickListener { startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)) }
        }
        val choose = Button(this).apply {
            text = "2. Seleccionar NeoKeys"
            setOnClickListener {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            }
        }
        val section = TextView(this).apply {
            text = "TEMAS"
            textSize = 14f
            setTextColor(0xFF8FD7FF.toInt())
            setPadding(0, 22, 0, 8)
        }
        val themeInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFEAF2FF.toInt())
        }
        fun updateThemeInfo() {
            themeInfo.text = "Tema actual: " + when (prefs.getString("theme", "ocean")) {
                "midnight" -> "Midnight"
                "rose" -> "Rose"
                "emerald" -> "Emerald"
                else -> "Ocean"
            }
        }
        updateThemeInfo()

        val themeButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("Ocean" to "ocean", "Midnight" to "midnight", "Rose" to "rose", "Emerald" to "emerald").forEach { (label, id) ->
            themeButtons.addView(Button(this).apply {
                text = label
                setOnClickListener {
                    prefs.edit().putString("theme", id).apply()
                    updateThemeInfo()
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val opacityTitle = TextView(this).apply {
            text = "TRANSPARENCIA DE TECLAS"
            textSize = 14f
            setTextColor(0xFF8FD7FF.toInt())
            setPadding(0, 18, 0, 8)
        }
        val opacityInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFEAF2FF.toInt())
        }
        fun updateOpacityInfo() {
            opacityInfo.text = "Opacidad: ${prefs.getInt("key_opacity", 90)}%"
        }
        updateOpacityInfo()
        val opacityButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(60, 75, 90, 100).forEach { value ->
            opacityButtons.addView(Button(this).apply {
                text = "$value%"
                setOnClickListener {
                    prefs.edit().putInt("key_opacity", value).apply()
                    updateOpacityInfo()
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val heightInfo = TextView(this).apply {
            textSize = 14f
            setTextColor(0xFFEAF2FF.toInt())
            setPadding(0, 18, 0, 4)
        }
        fun updateHeightInfo() {
            heightInfo.text = "Altura: ${prefs.getInt("keyboard_height_dp", 278)} dp"
        }
        updateHeightInfo()
        val heightButtons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(250, 278, 310, 340).forEach { value ->
            heightButtons.addView(Button(this).apply {
                text = value.toString()
                setOnClickListener {
                    prefs.edit().putInt("keyboard_height_dp", value).apply()
                    updateHeightInfo()
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }

        val haptics = Button(this).apply {
            text = if (prefs.getBoolean("haptics_enabled", true)) "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            setOnClickListener {
                val enabled = !prefs.getBoolean("haptics_enabled", true)
                prefs.edit().putBoolean("haptics_enabled", enabled).apply()
                text = if (enabled) "Vibración: ACTIVADA" else "Vibración: DESACTIVADA"
            }
        }
        val note = TextView(this).apply {
            text = "Los cambios visuales se aplican al abrir nuevamente el teclado. Se conservan los idiomas, símbolos, vibración, animación y borrado continuo."
            textSize = 13f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 18, 0, 0)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(enable)
        root.addView(choose)
        root.addView(section)
        root.addView(themeInfo)
        root.addView(themeButtons)
        root.addView(opacityTitle)
        root.addView(opacityInfo)
        root.addView(opacityButtons)
        root.addView(heightInfo)
        root.addView(heightButtons)
        root.addView(haptics)
        root.addView(note)
        setContentView(root)
    }
}
