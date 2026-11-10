package com.neokeys.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE) }
    private lateinit var status: TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()

    private fun label(s:String,size:Float,color:Int=0xFFF3F6FF.toInt()): TextView {
        val view = TextView(this)
        view.text = s
        view.textSize = size
        view.setTextColor(color)
        return view
    }

    private fun card(title:String,subtitle:String):LinearLayout {
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=android.graphics.drawable.GradientDrawable().apply{setColor(0xFF151D30.toInt());cornerRadius=dp(22).toFloat();setStroke(dp(1),0xFF354967.toInt())}}
        box.addView(label(title,18f));box.addView(label(subtitle,12f,0xFF9FB2D1.toInt()).apply{setPadding(0,dp(5),0,dp(8))});return box
    }
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=12f;setOnClickListener{action()}}
    private fun addChoices(box:LinearLayout,items:List<Pair<String,()->Unit>>) {
        items.chunked(2).forEach{group->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};group.forEach{(name,fn)->row.addView(button(name,fn),LinearLayout.LayoutParams(0,dp(48),1f))};box.addView(row)}
    }
    private fun margin()=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)}
    private fun save(key:String,value:String,name:String)=prefs.edit().putString(key,value).apply().also{status.text="Aplicado: $name"}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);render()}
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(28),dp(20),dp(32));setBackgroundColor(0xFF0B1020.toInt())}
        val scroll=ScrollView(this).apply{addView(root)}
        root.addView(label("NeoKeys",36f))
        root.addView(label("PREMIUM STUDIO • V2.1",12f,0xFF6F7DFF.toInt()).apply{setPadding(0,dp(4),0,dp(18))})
        val hero=card("Tu teclado. Tu identidad.","Un centro de control para personalizar tu experiencia.")
        hero.addView(button("ACTIVAR NEOKEYS"){startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))})
        hero.addView(button("ELEGIR TECLADO"){(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()})
        root.addView(hero,margin())
        status=label("Tus preferencias se guardan en este dispositivo.",12f,0xFF8FBDDB.toInt());root.addView(status)

        val writing=card("Escritura inteligente","Autocorrección en español (México)")
        val autocorrectButton = button("") { }
        fun refreshAutocorrectButton() {
            autocorrectButton.text = if(prefs.getBoolean("autocorrect_enabled",true)) "AUTOCORRECTOR ACTIVADO" else "AUTOCORRECTOR DESACTIVADO"
        }
        refreshAutocorrectButton()
        autocorrectButton.setOnClickListener {
            val enabled = !prefs.getBoolean("autocorrect_enabled",true)
            prefs.edit().putBoolean("autocorrect_enabled",enabled).apply()
            refreshAutocorrectButton()
            status.text = if(enabled) "Autocorrector en español activado" else "Autocorrector desactivado"
        }
        writing.addView(autocorrectButton)
        writing.addView(label("Corrige palabras al pulsar espacio. Requiere que Android tenga un corrector de español disponible.",11f,0xFF9FB2D1.toInt()))
        root.addView(writing,margin())

        val themes=listOf("Violet Glass" to "violet","Cyber Pulse" to "cyber","Ivory Minimal" to "ivory","Emerald OS" to "emerald","Crimson" to "crimson","Sakura" to "sakura","Holographic" to "holo","Titanium" to "titanium")
        val theme=card("Colección de temas","Ocho identidades visuales")
        addChoices(theme,themes.map{(n,id)->n to {save("theme",id,n)}});root.addView(theme,margin())
        val shapes=listOf("Cuadrada" to "square","Redondeada" to "rounded","Píldora" to "pill","Super redonda" to "super","Hexagonal" to "hex")
        val shape=card("Formas de teclas","Cambia la silueta de cada tecla")
        addChoices(shape,shapes.map{(n,id)->n to {save("key_shape",id,n)}});root.addView(shape,margin())
        val effects=listOf("Pulso" to "pulse","Onda" to "wave","Brillo" to "glow","Expansión" to "expand")
        val effect=card("Efectos al presionar","Respuesta visual")
        addChoices(effect,effects.map{(n,id)->n to {save("press_effect",id,n)}});root.addView(effect,margin())
        val photo=card("Fondos personalizados","Elige una fotografía de tu galería")
        photo.addView(button("ELEGIR FOTOGRAFÍA"){val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*"};startActivityForResult(i,101)})
        photo.addView(button("QUITAR FOTOGRAFÍA"){prefs.edit().remove("background_uri").apply();status.text="Fondo eliminado"})
        root.addView(photo,margin())
        val accent=card("Color de acento","Iluminación y detalles")
        val colors=listOf("Cian" to "#25D5FF","Violeta" to "#A78BFA","Rosa" to "#FF6B9A","Verde" to "#75F5A1","Dorado" to "#F5C76E")
        addChoices(accent,colors.map{(n,c)->n to {save("accent_color",c,n)}});root.addView(accent,margin())
        val settings=card("Sensación del teclado","Transparencia, cristal y altura")
        listOf("keyboard_opacity" to listOf(60,75,90,100),"glass_intensity" to listOf(40,70,100),"keyboard_height_dp" to listOf(250,278,310,340)).forEach{(key,values)->settings.addView(label(key.replace('_',' '),13f));addChoices(settings,values.map{v->"$v" to {prefs.edit().putInt(key,v).apply();status.text="Aplicado: $key $v"}})}
        root.addView(settings,margin())
        settings.addView(button(if(prefs.getBoolean("haptics_enabled",true))"Vibración ACTIVADA" else "Vibración DESACTIVADA"){val b=!prefs.getBoolean("haptics_enabled",true);prefs.edit().putBoolean("haptics_enabled",b).apply();status.text=if(b)"Vibración activada" else "Vibración desactivada"})
        root.addView(label("NEOKEYS • ESCRIBE A TU MANERA",10f,0xFF7185A8.toInt()))
        setContentView(scroll)
    }
    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==101&&resultCode==RESULT_OK){data?.data?.let{uri->contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);prefs.edit().putString("background_uri",uri.toString()).apply();status.text="Fondo actualizado"}}}
}
