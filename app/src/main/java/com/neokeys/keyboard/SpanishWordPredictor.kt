package com.neokeys.keyboard

import java.text.Normalizer
import java.util.Locale

/** Offline Spanish starter dictionary. No network or typed text storage. */
object SpanishWordPredictor {
    private val words = """
        hola cómo como estás esta estoy está estamos están estaba estar este esto
        qué que quién quien cuándo cuando dónde donde cuál cual cuánto cuanto
        buenos buenas buen bueno buena días día tardes tarde noches noche
        gracias muchas mucho mucha muchos por favor para porque pero pues
        mañana ayer hoy ahora después antes luego siempre nunca también tampoco
        amor amigo amiga amigos amigas familia mamá papá casa trabajo trabajando
        trabajar ruta paquete paquetes entrega entregado entregados cliente
        clientes dirección colonia calle número teléfono mensaje mensajes
        listo lista bien mal excelente perfecto perfecto claro seguro
        tengo tienes tiene tenemos tienen tener hacer haces hace hacemos
        quiero quieres quiere queremos poder puedo puedes puede podemos
        voy vas va vamos van venir viene vienen llegando llegué llegar
        espero esperar esperando saludos saludo hasta pronto ver vemos
        sí si no nada todo todos todas toda otro otra otros otras
        el la los las un una unos unas de del al a en con sin
        me te se nos les lo le mi mis tu tus su sus
        yo tú usted ustedes nosotros ellos ellas él ella
        es son era fue fueron será ser sido hay había
        aquí allá ahí allí cerca lejos dentro fuera arriba abajo
        más menos muy poco algo alguien nadie siempre
        bonito bonita hermoso hermosa feliz contento contenta
        escribir escrito palabra palabras texto textos teclado
        español México mexicano mexicana ciudad tiempo fecha
        lunes martes miércoles jueves viernes sábado domingo
        enero febrero marzo abril mayo junio julio agosto septiembre octubre noviembre diciembre
        uno dos tres cuatro cinco seis siete ocho nueve diez
        correcto correcta corrección corregir error errores
        puedes puedo puede podemos podrían podría quiero quieres
        estamos estaban estará estaré hacemos hacemos hecho hice
        mensaje enviar enviaré enviado recibido recibiste recibieron
        paquete paquetes entrega entregas entregué entregaste
        dirección direcciones ubicación ubicaciónes referencia
        información informacion importante importante urgente
        necesito necesitamos necesitas necesita necesitar
        problema problemas solución soluciones pregunta preguntas
        respuesta respuestas atención servicio soporte sistema
        aplicación aplicaciones funcionamiento funciona funcionando
        gracias igualmente igualmente saludos excelente genial
        también además entonces mientras porque aunque cuando
        todavía aun aún quizá quizás tal vez probablemente
        bonito bonita buenos buenas tardes noches días
        estaba estabas estaban estamos estuvieron estuvimos
        tengo tienes tiene tenemos tienen tendrán tendría
        hago haces hace hacemos hacen hice hizo hicieron
        digo dices dice decimos dicen dije dijo dijeron
        voy vas va vamos van fui fue fueron iremos
        casa casas coche carro automóvil auto computadora
        celular teléfono pantalla teclado palabra palabras
        escribir escribiendo escribiste escribo escribes escribe
        rápido rápido rápidamente despacio correcto incorrecto
        México mexicano mexicana español españolito
        dónde cómo cuándo cuál quién qué
        podrías podría podríamos gustaría gracias disculpa disculpe
        recibí recibido recibir confirmar confirmado confirmación
        enviar enviado envío envíos entregar entregué entrega
        dinero pago pagos cobrar cobro pendiente pendientes
        ayuda ayudar necesito necesitas necesitamos
        buenos días buenas tardes buenas noches
    """.trimIndent().split(Regex("\\s+")).distinct()

    private val corrections = mapOf(
        "ola" to "hola", "grasias" to "gracias", "graciaz" to "gracias",
        "estaz" to "estás",
        "komo" to "cómo", "kmo" to "cómo",
        "qe" to "qué", "qeu" to "qué",
        "tambien" to "también", "manana" to "mañana",
        "miercoles" to "miércoles", "sabado" to "sábado",
        "telefono" to "teléfono", "direccion" to "dirección",
        "numero" to "número", "mexico" to "México",
        "aver" to "a ver",
        "aser" to "hacer", "aserlo" to "hacerlo",
        "acer" to "hacer", "haci" to "así",
        "asi" to "así", "aki" to "aquí", "aqui" to "aquí",
        "ay" to "hay", "ahy" to "ahí", "ahi" to "ahí",
        "buenosdias" to "buenos días", "buenastardes" to "buenas tardes",
        "buenasnoches" to "buenas noches", "porfavor" to "por favor",
        "xq" to "porque", "pq" to "porque", "porq" to "porque",
        "dnd" to "dónde", "q" to "que", "ke" to "que",
        "k" to "que", "ola" to "hola", "holaa" to "hola",
        "holaaa" to "hola", "grasias" to "gracias", "grax" to "gracias",
        "gracia" to "gracias", "gras" to "gracias",
        "eskribir" to "escribir", "escribir" to "escribir",
        "nesesito" to "necesito", "nececitas" to "necesitas",
        "necesitoo" to "necesito", "trabajndo" to "trabajando",
        "trabajandoo" to "trabajando", "paqeute" to "paquete",
        "paquetez" to "paquetes", "entregaa" to "entrega",
        "direcsion" to "dirección", "dirrecion" to "dirección",
        "informasion" to "información", "infromacion" to "información",
        "comunicacion" to "comunicación", "comunicazion" to "comunicación",
        "tambien" to "también", "mas" to "más", "despues" to "después",
        "dia" to "día", "dias" to "días", "facil" to "fácil",
        "rapido" to "rápido", "ultimamente" to "últimamente",
        "atencion" to "atención", "solucion" to "solución",
        "corazon" to "corazón", "cancion" to "canción",
        "miercoles" to "miércoles", "sabado" to "sábado"
    )

    private fun normalized(value: String): String = Normalizer
        .normalize(value.lowercase(Locale("es", "MX")), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")

    fun correction(word: String): String? = corrections[word.lowercase(Locale("es", "MX"))]

    fun suggestions(prefix: String): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase(Locale("es", "MX"))
        val direct = correction(prefix)
        val matches = words.asSequence()
            .filter { normalized(it).startsWith(normalized(lower)) && !it.equals(prefix, true) }
            .sortedWith(compareBy<String> { it.length }.thenBy { it })
            .take(10).toList()
        return (listOf(prefix) + listOfNotNull(direct) + matches)
            .distinct().take(3)
    }
}
