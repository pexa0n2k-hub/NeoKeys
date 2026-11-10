package com.neokeys.keyboard

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import android.widget.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE) }
    private lateinit var status: TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun label(s:String,size:Float,color:Int=0xFFF3F6FF.toInt())=TextView(this).apply{text=s;textSize=size;setTextColor(color)}
    private fun card(title:String,subtitle:String):LinearLayout {
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(18),dp(16),dp(18),dp(16));background=android.graphics.drawable.GradientDrawable().apply{setColor(0xFF151D30.toInt());cornerRadius=dp(22).toFloat();setStroke(dp(1),0xFF354967.toInt())}}
        box.addView(label(title,18f));box.addView(label(subtitle,12f,0xFF9FB2D1.toInt()).apply{setPadding(0,dp(5),0,dp(8))});return box
    }
    private fun button(s:String,action:()->Unit)=Button(this).apply{text=s;isAllCaps=false;textSize=12f;setOnClickListener{action()}}
    private fun addChoices(box:LinearLayout,items:List<Pair<String,()->Unit>>) {
        items.chunked(2).forEach{group->val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};group.forEach{(name,fn)->row.addView(button(name,fn),LinearLayout.LayoutParams(0,dp(48),1f).apply{setMargins(dp(3),dp(3),dp(3),dp(3))})};box.addView(row)}
    }
    private fun margin()=LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(14)}
    private fun save(key:String,value:String,name:String){prefs.edit().putString(key,value).apply();status.text="Aplicado: $name"}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);render()}
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(20),dp(28),dp(20),dp(32));setBackgroundColor(0xFF080D18.toInt())}
        val scroll=ScrollView(this).apply{addView(root)}
        root.addView(label("NeoKeys",36f))
        root.addView(label("PREMIUM STUDIO  •  V2.0",12f,0xFF6DE7FF.toInt()).apply{setPadding(0,dp(4),0,dp(18))})
        val hero=card("Tu teclado. Tu identidad.","Un centro de control para personalizar tu experiencia.")
        hero.addView(button("ACTIVAR NEOKeys"){startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))})
        hero.addView(button("ELEGIR TECLADO"){(getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()})
        root.addView(hero,margin())
        status=label("Tus preferencias se guardan en este dispositivo.",12f,0xFF80DDBB.toInt());root.addView(status)
        val themes=listOf("Violet Glass" to "violet","Cyber Pulse" to "cyber","Ivory Minimal" to "ivory","Emerald OS" to "emerald","Crimson" to "crimson","Sakura" to "sakura","Holographic" to "holographic","Titanium" to "titanium")
        val theme=card("Colección de temas","Ocho identidades visuales")
        addChoices(theme,themes.map{(n,id)->n to {save("theme",id,n)}});root.addView(theme,margin())
        val shapes=listOf("Cuadrada" to "square","Redondeada" to "rounded","Píldora" to "pill","Súper redonda" to "super","Hexagonal" to "hexagon","Diagonal" to "diagonal","Corte gamer" to "gamer","Neón" to "neon")
        val shape=card("Formas de teclas","Cambia la silueta de cada tecla")
        addChoices(shape,shapes.map{(n,id)->n to {save("key_shape",id,n)}});root.addView(shape,margin())
        val effects=listOf("Pulso" to "pulse","Onda" to "wave","Brillo" to "glow","Expansión" to "expand")
        val effect=card("Efectos al presionar","Respuesta visual")
        addChoices(effect,effects.map{(n,id)->n to {save("press_effect",id,n)}});root.addView(effect,margin())
        val photo=card("Fondos personalizados","Elige una fotografía de tu galería")
        photo.addView(button("ELEGIR FOTOGRAFÍA"){val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*";addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)};startActivityForResult(i,501)})
        photo.addView(button("QUITAR FOTOGRAFÍA"){prefs.edit().remove("keyboard_background_uri").apply();status.text="Fondo eliminado"})
        root.addView(photo,margin())
        val accent=card("Color de acento","Iluminación y detalles")
        val colors=listOf("Cian" to 0xFF32E8FF.toInt(),"Violeta" to 0xFFB45CFF.toInt(),"Rosa" to 0xFFFF55D8.toInt(),"Verde" to 0xFF35E6B0.toInt(),"Rojo" to 0xFFFF3B4E.toInt(),"Dorado" to 0xFFFFC98A.toInt())
        addChoices(accent,colors.map{(n,c)->n to {prefs.edit().putInt("accent_color",c).apply();status.text="Acento: $n"}});root.addView(accent,margin())
        val settings=card("Sensación y respuesta","Ajusta la experiencia")
        listOf("key_opacity" to listOf(60,75,90,100),"glass_intensity" to listOf(40,70,100),"keyboard_height_dp" to listOf(250,278,310,340)).forEach{(key,values)->
            settings.addView(label(when(key){"key_opacity"->"Transparencia";"glass_intensity"->"Liquid Glass";else->"Altura"},13f,0xFFB9C9E5.toInt()))
            addChoices(settings,values.map{n->("$n"+if(key=="keyboard_height_dp")" dp" else "%") to {prefs.edit().putInt(key,n).apply();status.text="$key: $n"}})
        }
        settings.addView(button(if(prefs.getBoolean("haptics_enabled",true))"Vibración ACTIVADA" else "Vibración DESACTIVADA"){val b=!prefs.getBoolean("haptics_enabled",true);prefs.edit().putBoolean("haptics_enabled",b).apply();status.text=if(b)"Vibración activada" else "Vibración desactivada"})
        root.addView(settings,margin())
        root.addView(label("NEOKEYS • ESCRIBE A TU MANERA",10f,0xFF7185A8.toInt()))
        setContentView(scroll)
    }
    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==501&&resultCode==RESULT_OK){val uri:Uri=data?.data?:return;try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:SecurityException){};prefs.edit().putString("keyboard_background_uri",uri.toString()).apply();status.text="Fotografía guardada"}}
}
