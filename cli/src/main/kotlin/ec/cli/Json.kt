package ec.cli

/**
 * JSON as the site speaks it, parsed into values the compiler can check.
 * Small on purpose: the whole API is three flat objects, which doesn't justify a library.
 */
internal sealed interface Json {
    data class Str(val value: String) : Json

    /** Kept exactly as written, so nothing is lost until a caller asks for a type. */
    data class Num(val text: String) : Json
    data class Bool(val value: Boolean) : Json
    data object Null : Json
    data class Arr(val items: List<Json>) : Json
    data class Obj(val fields: Map<String, Json>) : Json {
        /** A text field. Numbers come back as written, since answers can be either. */
        fun text(name: String): String? = when (val v = fields[name]) {
            is Str -> v.value
            is Num -> v.text
            else   -> null
        }

        fun long(name: String): Long? = (fields[name] as? Num)?.text?.toBigDecimalOrNull()?.toLong()
        fun bool(name: String): Boolean? = (fields[name] as? Bool)?.value
    }

    companion object {
        fun parse(text: String): Json = Parser(text).document()

        fun parseObject(text: String): Obj =
            parse(text) as? Obj ?: throw IllegalArgumentException("expected a JSON object")

        /** [value] as a JSON string literal, quotes included. */
        fun quote(value: String): String = buildString {
            append('"')
            for (c in value) when (c) {
                '"'  -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
            }
            append('"')
        }
    }
}

private class Parser(private val s: String) {
    private var i = 0

    fun document(): Json = value().also { whitespace(); if (i != s.length) error("unexpected text after the value") }

    private fun value(): Json {
        whitespace()
        return when (peek()) {
            '{'  -> obj()
            '['  -> arr()
            '"'  -> Json.Str(string())
            't'  -> literal("true", Json.Bool(true))
            'f'  -> literal("false", Json.Bool(false))
            'n'  -> literal("null", Json.Null)
            else -> num()
        }
    }

    private fun obj(): Json.Obj {
        expect('{')
        val fields = LinkedHashMap<String, Json>()
        whitespace()
        if (peek() == '}') {
            i++; return Json.Obj(fields)
        }
        while (true) {
            whitespace()
            val name = string()
            whitespace()
            expect(':')
            fields[name] = value()
            whitespace()
            when (next()) {
                ','  -> continue
                '}'  -> return Json.Obj(fields)
                else -> error("expected ',' or '}'")
            }
        }
    }

    private fun arr(): Json.Arr {
        expect('[')
        val items = mutableListOf<Json>()
        whitespace()
        if (peek() == ']') {
            i++; return Json.Arr(items)
        }
        while (true) {
            items += value()
            whitespace()
            when (next()) {
                ','  -> continue
                ']'  -> return Json.Arr(items)
                else -> error("expected ',' or ']'")
            }
        }
    }

    private fun string(): String {
        expect('"')
        val out = StringBuilder()
        while (true) {
            val c = next()
            when {
                c == '"'  -> return out.toString()
                c == '\\' -> out.append(
                    when (val e = next()) {
                        '"', '\\', '/' -> e
                        'b'            -> '\b'
                        'f'            -> '\u000C'
                        'n'            -> '\n'
                        'r'            -> '\r'
                        't'            -> '\t'
                        'u'            -> unicode()
                        else           -> error("unknown escape \\$e")
                    }
                )

                c < ' '   -> error("control character inside a string")
                else      -> out.append(c)
            }
        }
    }

    private fun unicode(): Char {
        if (i + 4 > s.length) error("incomplete \\u escape")
        val code = s.substring(i, i + 4).toIntOrNull(16) ?: error("bad \\u escape")
        i += 4
        return code.toChar()
    }

    private fun num(): Json.Num {
        val start = i
        if (i < s.length && s[i] == '-') i++
        digits()
        if (i < s.length && s[i] == '.') {
            i++; digits()
        }
        if (i < s.length && (s[i] == 'e' || s[i] == 'E')) {
            i++
            if (i < s.length && (s[i] == '+' || s[i] == '-')) i++
            digits()
        }
        return Json.Num(s.substring(start, i))
    }

    private fun digits() {
        val start = i
        while (i < s.length && s[i] in '0'..'9') i++
        if (i == start) error("expected a digit")
    }

    private fun literal(word: String, value: Json): Json {
        if (!s.startsWith(word, i)) error("expected '$word'")
        i += word.length
        return value
    }

    private fun whitespace() {
        while (i < s.length && s[i] in " \t\n\r") i++
    }

    private fun peek(): Char = if (i < s.length) s[i] else error("unexpected end of input")
    private fun next(): Char = peek().also { i++ }
    private fun expect(c: Char) {
        if (next() != c) error("expected '$c'")
    }

    private fun error(message: String): Nothing =
        throw IllegalArgumentException("invalid JSON at character $i: $message")
}
