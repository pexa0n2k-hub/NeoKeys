package com.neokeys.keyboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
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
            text = "LIQUID GLASS • V1.0\nEspañol + English · Fila numérica"
            textSize = 15f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 12, 0, 28)
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
        val note = TextView(this).apply {
            text = "Después de activarlo, abre cualquier campo de texto y selecciona NeoKeys desde el selector de teclado."
            textSize = 14f
            setTextColor(0xFFB8C9E8.toInt())
            setPadding(0, 24, 0, 0)
        }
        root.addView(title); root.addView(subtitle); root.addView(enable); root.addView(choose); root.addView(note)
        setContentView(root)
    }
}
