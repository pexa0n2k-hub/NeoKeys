package com.neokeys.keyboard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("neokeys_settings", Context.MODE_PRIVATE) }
    private val profiles by lazy { getSharedPreferences("neokeys_studio_profiles", Context.MODE_PRIVATE) }
    private val ink = 0xFFE9F1FC.toInt()
    private val muted = 0xFF9AAAC1.toInt()
    private val panel = 0xFF1A2435.toInt()
    private val accent = 0xFF7DCBFF.toInt()
    private var section = 0
    private lateinit var preview: NeoKeyboardView
    private lateinit var controls: LinearLayout
    private lateinit var status: TextView
    private lateinit var side: LinearLayout
    private val sections = listOf("Inicio", "Temas", "Teclas", "Fondos", "Efectos", "Avanzado", "Perfiles")
    private val themeNames = listOf("Violet Glass" to "violet", "Cyber Pulse" to "cyber", "Ivory Minimal" to "ivory", "Emerald OS" to "emerald", "Crimson" to "crimson", "Sakura" to "sakura", "Holographic" to "holographic", "Titanium" to "titanium")
    private val snapshotKeys = listOf("theme", "key_shape", "press_effect", "keyboard_background_uri", "accent_color", "key_opacity", "glass_intensity", "keyboard_height_dp", "haptics_enabled", "autocorrect_enabled", "background_zoom", "background_x", "background_y", "background_brightness", "background_saturation", "background_overlay_color", "background_overlay_alpha")
    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 14f, color: Int = ink) = TextView(this).apply { text=s; textSize=size; setTextColor(color) }
    private fun shape(color: Int, radius: Int = 16, stroke: Int? = null) = GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat(); if(stroke!=null) setStroke(dp(1),stroke) }
    private fun button(s: String, action: () -> Unit) = TextView(this).apply {
        text=s; textSize=13f; setTextColor(ink); gravity=Gravity.CENTER
        setPadding(dp(12),dp(12),dp(12),dp(12)); background=shape(0xFF293A52.toInt(),12,0xFF425B78.toInt())
        setOnClickListener { action() }
    }
    private fun block(title: String, subtitle: String? = null) = LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(14),dp(14),dp(14),dp(14)); background=shape(panel,16,0xFF35445B.toInt())
        addView(text(title,17f))
        if(subtitle!=null) addView(text(subtitle,12f,muted).apply{setPadding(0,dp(4),0,dp(10))})
    }
    private fun addBlock(v: View) { controls.addView(v,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(12)}) }
    private fun choiceGrid(host: LinearLayout, choices: List<Pair<String,String>>, current: String, onChoose: (String)->Unit) {
        choices.chunked(2).forEach { pair ->
            val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
            pair.forEach { (name,id) ->
                val b=button((if(current==id) "✓  " else "")+name){onChoose(id)}
                row.addView(b,LinearLayout.LayoutParams(0,-2,1f).apply{setMargins(dp(2),dp(3),dp(2),dp(3))})
            }
            host.addView(row)
        }
    }
    private fun saveString(key: String, value: String) { prefs.edit().putString(key,value).apply(); updated() }
    private fun saveInt(key: String, value: Int) { prefs.edit().putInt(key,value).apply(); updated(false) }
    private fun updated(redraw: Boolean = true) { preview.invalidate(); status.text="Vista previa actualizada · Los cambios se guardan automáticamente"; if(redraw) showSection() }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); section=savedInstanceState?.getInt("section")?:0; buildStudio() }
    override fun onSaveInstanceState(outState: Bundle) { outState.putInt("section",section); super.onSaveInstanceState(outState) }
    private fun buildStudio() {
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFF0C121D.toInt());setPadding(dp(12),dp(16),dp(12),dp(8)) }
        root.addView(text("NeoKeys  /  CREATIVE STUDIO",12f,accent))
        root.addView(text("Tu teclado. Tu diseño.",22f).apply{setPadding(0,dp(4),0,dp(8))})
        preview=NeoKeyboardView(this) { _ -> }
        // The studio preview is display-only: never send preview taps to an input field.
        preview.setOnTouchListener { _, _ -> true }
        root.addView(preview,LinearLayout.LayoutParams(-1,dp(192)))
        status=text("Vista previa interactiva de los ajustes visuales",11f,muted)
        root.addView(status,LinearLayout.LayoutParams(-1,-2).apply{topMargin=dp(6);bottomMargin=dp(12)})
        val workspace=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        root.addView(workspace,LinearLayout.LayoutParams(-1,0,1f))
        side=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        val sideScroll=ScrollView(this).apply{isFillViewport=false;addView(side)}
        workspace.addView(sideScroll,LinearLayout.LayoutParams(dp(94),-1))
        val contentScroll=ScrollView(this)
        controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,dp(2),dp(20))}
        contentScroll.addView(controls)
        workspace.addView(contentScroll,LinearLayout.LayoutParams(0,-1,1f))
        showSection()
        setContentView(root)
    }
    private fun showSection() {
        if(!::controls.isInitialized) return
        side.removeAllViews()
        sections.forEachIndexed { i, name ->
            val b=text(name,11f,if(i==section) 0xFF0B1622.toInt() else muted).apply {
                gravity=Gravity.CENTER; setPadding(dp(5),dp(15),dp(5),dp(15));background=shape(if(i==section) accent else panel,12)
                setOnClickListener { section=i;showSection() }
            }
            side.addView(b,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=dp(7)})
        }
        controls.removeAllViews()
        when(section) {0->home();1->themes();2->keys();3->backgrounds();4->effects();5->advanced();6->profilesSection()}
        preview.invalidate()
    }
    private fun home() {
        addBlock(block("Creative Studio", "Vista dividida: arriba tu teclado y aquí las herramientas. Cada cambio visual se refleja en la vista previa."))
        val b=block("Accesos del teclado")
        b.addView(button("Activar NeoKeys") {startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))})
        b.addView(button("Elegir teclado") {(getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager).showInputMethodPicker()})
        addBlock(b)
        val v=block("Dictado por voz", "Conservamos la configuración del micrófono de V2.3.")
        v.addView(button(if(Build.VERSION.SDK_INT<23 || checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED) "Micrófono permitido" else "Permitir micrófono") {
            if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED) requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO),202)
            else status.text="Micrófono disponible en el teclado"
        });addBlock(v)
    }
    private fun themes() {
        val b=block("Appearance Lab", "Elige una identidad visual. Se conservan los temas existentes.")
        choiceGrid(b,themeNames,prefs.getString("theme","violet")?:"violet"){saveString("theme",it)}
        addBlock(b)
        val c=block("Color de acento", "Selecciona un color de la paleta")
        val palette=listOf("Cian" to 0xFF25D5FF.toInt(),"Violeta" to 0xFFA78BFA.toInt(),"Rosa" to 0xFFFF6B9A.toInt(),"Verde" to 0xFF75F5A1.toInt(),"Dorado" to 0xFFF5C76E.toInt())
        palette.forEach { (name,color) -> c.addView(button(name){prefs.edit().putInt("accent_color",color).apply();updated()}) }
        addBlock(c)
    }
    private fun keys() {
        val b=block("Key Designer", "Cambia la silueta de todas las teclas")
        choiceGrid(b,listOf("Redondeadas" to "rounded","Cuadradas" to "square","Píldora" to "pill","Super redondas" to "super","Hexagonales" to "hexagon","Diagonal" to "diagonal","Gamer" to "gamer","Neón" to "neon"),prefs.getString("key_shape","rounded")?:"rounded"){saveString("key_shape",it)}
        addBlock(b)
        slider("Altura del teclado", "keyboard_height_dp",230,360,278)
    }
    private fun backgrounds() {
        val image = block("Imagen de fondo", "Selecciona una fotografía y ajusta cómo aparece detrás de las teclas")
        image.addView(button("Seleccionar fotografía") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, 101)
        })
        image.addView(button("Quitar fotografía") {
            prefs.edit().remove("keyboard_background_uri").apply()
            updated()
        })
        addBlock(image)

        slider("Zoom de imagen", "background_zoom", 100, 250, 100)
        slider("Posición horizontal", "background_x", -100, 100, 0)
        slider("Posición vertical", "background_y", -100, 100, 0)
        slider("Brillo", "background_brightness", 50, 150, 100)
        slider("Saturación", "background_saturation", 0, 200, 100)

        val overlay = block("Capa de color", "Añade un tinte sobre la fotografía para combinarla con tu tema")
        val colors = listOf(
            "Sin color" to 0x00000000,
            "Violeta" to 0xFF713BC0.toInt(),
            "Cian" to 0xFF087E9B.toInt(),
            "Rosa" to 0xFFB83F79.toInt(),
            "Negro" to 0xFF000000.toInt(),
            "Blanco" to 0xFFFFFFFF.toInt()
        )
        choiceGrid(
            overlay,
            colors.map { it.first to it.second.toUInt().toString() },
            prefs.getInt("background_overlay_color", 0).toUInt().toString()
        ) { selected ->
            val color = selected.toUInt().toInt()
            prefs.edit().putInt("background_overlay_color", color).apply()
            updated()
        }
        addBlock(overlay)
        slider("Intensidad de la capa", "background_overlay_alpha", 0, 180, 0)

        val reset = block("Restablecer fondo", "Restaura el encuadre y los ajustes de imagen sin eliminar tu fotografía")
        reset.addView(button("Restablecer ajustes de imagen") {
            prefs.edit()
                .putInt("background_zoom", 100)
                .putInt("background_x", 0)
                .putInt("background_y", 0)
                .putInt("background_brightness", 100)
                .putInt("background_saturation", 100)
                .putInt("background_overlay_color", 0)
                .putInt("background_overlay_alpha", 0)
                .apply()
            updated()
        })
        addBlock(reset)
    }
    private fun effects() {
        val b=block("Effects Engine", "Efecto visual al presionar")
        choiceGrid(b,listOf("Brillo" to "glow","Pulso" to "pulse","Onda" to "wave","Expansión" to "expand"),prefs.getString("press_effect","glow")?:"glow"){saveString("press_effect",it)}
        addBlock(b)
        val h=block("Respuesta táctil")
        h.addView(button(if(prefs.getBoolean("haptics_enabled",true)) "Vibración: activada" else "Vibración: desactivada") {
            prefs.edit().putBoolean("haptics_enabled",!prefs.getBoolean("haptics_enabled",true)).apply();updated()
        });addBlock(h)
    }
    private fun advanced() {
        slider("Opacidad de teclas", "key_opacity",40,100,90)
        slider("Intensidad de cristal", "glass_intensity",0,100,70)
        slider("Altura del teclado", "keyboard_height_dp",230,360,278)
        val b=block("Escritura inteligente", "No modifica el motor táctil ni las acciones de ENTER")
        b.addView(button(if(prefs.getBoolean("autocorrect_enabled",true)) "Autocorrector: activado" else "Autocorrector: desactivado") {
            prefs.edit().putBoolean("autocorrect_enabled",!prefs.getBoolean("autocorrect_enabled",true)).apply();updated()
        });addBlock(b)
    }
    private fun slider(title:String,key:String,min:Int,max:Int,default:Int) {
        val b=block(title)
        val current=prefs.getInt(key,default).coerceIn(min,max)
        val value=text("$current",13f,accent);b.addView(value)
        val seek=SeekBar(this).apply{this.max=max-min;progress=current-min}
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s:SeekBar?,p:Int,fromUser:Boolean) {if(fromUser){val n=p+min;value.text="$n";saveInt(key,n)}}
            override fun onStartTrackingTouch(s:SeekBar?){}
            override fun onStopTrackingTouch(s:SeekBar?){}
        })
        b.addView(seek);addBlock(b)
    }
    private fun snapshot(): JSONObject {
        val obj=JSONObject()
        snapshotKeys.forEach { k ->
            when(val v=prefs.all[k]) {is String->obj.put(k,v);is Int->obj.put(k,v);is Boolean->obj.put(k,v)}
        }
        return obj
    }
    private fun restore(obj: JSONObject) {
        val e=prefs.edit()
        snapshotKeys.forEach { key ->
            if (obj.has(key)) {
                when (val v = obj.get(key)) {
                    is String -> e.putString(key, v)
                    is Int -> e.putInt(key, v)
                    is Boolean -> e.putBoolean(key, v)
                }
            } else {
                e.remove(key)
            }
        }
        e.apply();updated()
    }
    private fun profilesSection() {
        val b=block("Mis perfiles", "Guarda combinaciones completas y vuelve a aplicarlas cuando quieras")
        b.addView(button("＋ Guardar perfil actual") {
            val input=EditText(this).apply{hint="Nombre del perfil";setSingleLine(true)}
            AlertDialog.Builder(this).setTitle("Guardar perfil").setView(input).setNegativeButton("Cancelar",null).setPositiveButton("Guardar") {_,_->
                val name=input.text.toString().trim()
                if(name.isNotEmpty()) {profiles.edit().putString(name,snapshot().toString()).apply();status.text="Perfil guardado: $name";showSection()}
            }.show()
        });addBlock(b)
        val names=profiles.all.keys.sorted()
        if(names.isEmpty()) addBlock(block("Biblioteca vacía", "Crea tu primer perfil para verlo aquí."))
        names.forEach { name ->
            val row=block(name)
            row.addView(button("Aplicar perfil") {
                try {restore(JSONObject(profiles.getString(name,"{}")?:"{}"));status.text="Perfil aplicado: $name"} catch(_:Exception){status.text="No se pudo cargar el perfil"}
            })
            row.addView(button("Eliminar perfil") {
                AlertDialog.Builder(this).setTitle("¿Eliminar $name?").setNegativeButton("Cancelar",null).setPositiveButton("Eliminar") {_,_->profiles.edit().remove(name).apply();showSection()}.show()
            });addBlock(row)
        }
    }
    @Deprecated("Compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==101 && resultCode==RESULT_OK) data?.data?.let { uri ->
            try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
            prefs.edit().putString("keyboard_background_uri",uri.toString()).apply();updated()
        }
    }
    override fun onRequestPermissionsResult(requestCode:Int,permissions:Array<out String>,grantResults:IntArray) {
        super.onRequestPermissionsResult(requestCode,permissions,grantResults)
        if(requestCode==202) status.text=if(grantResults.firstOrNull()==PackageManager.PERMISSION_GRANTED) "Micrófono permitido" else "Permiso de micrófono no concedido"
    }
}
