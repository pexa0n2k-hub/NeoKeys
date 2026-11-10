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

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 48, 32, 32)
            setBackgroundColor(0xFF101722.toInt())
        }
        val title = TextView(this).apply {
            text = "NeoKeys"
            textSize = 32f
            setTextColor(0xFFEAF2FF.toInt())
        }
        val subtitle = TextView(this).apply {
            text = "LIQUID GLASS • V1.3\nEspañol + English · Símbolos · Respuesta háptica"
            textSize = 15f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 12, 0, 24)
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
        val settingsTitle = TextView(this).apply {
            text = "PERSONALIZACIÓN DEL TECLADO"
            textSize = 14f
            setTextColor(0xFF8FD7FF.toInt())
            setPadding(0, 28, 0, 8)
        }
        val heightLabel = TextView(this).apply {
            textSize = 15f
            setTextColor(0xFFEAF2FF.toInt())
        }
        fun refreshHeightLabel() {
            heightLabel.text = "Altura actual: ${prefs.getInt("keyboard_height_dp", 278)} dp"
        }
        refreshHeightLabel()

        val heightButtons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        listOf(250, 278, 310, 340).forEach { value ->
            heightButtons.addView(Button(this).apply {
                text = value.toString()
                setOnClickListener {
                    prefs.edit().putInt("keyboard_height_dp", value).apply()
                    refreshHeightLabel()
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
            text = "Los cambios de altura se aplican al volver a abrir el teclado. La vibración se puede activar o desactivar aquí."
            textSize = 14f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 20, 0, 0)
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(enable)
        root.addView(choose)
        root.addView(settingsTitle)
        root.addView(heightLabel)
        root.addView(heightButtons)
        root.addView(haptics)
        root.addView(note)
        setContentView(root)
    }
}
