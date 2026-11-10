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
        "numero" to "número", "mexico" to "México"
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
