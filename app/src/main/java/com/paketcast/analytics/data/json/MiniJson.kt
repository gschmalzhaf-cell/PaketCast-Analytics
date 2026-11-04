package com.paketcast.analytics.data.json

/**
 * Minimaler JSON-Parser (pure Kotlin, unit-testbar – org.json steht auf der JVM
 * in Unit-Tests nicht zur Verfügung). Liefert Map<String, Any?>, List<Any?>,
 * String, Double, Boolean oder null.
 */
object MiniJson {

    fun parse(text: String): Any? {
        val p = Parser(text)
        p.skipWs()
        val value = p.parseValue()
        p.skipWs()
        if (!p.atEnd()) throw IllegalArgumentException("Unerwartetes Zeichen an Position ${p.pos}")
        return value
    }

    fun parseObject(text: String): Map<String, Any?> =
        parse(text) as? Map<String, Any?> ?: emptyMap()

    private class Parser(val s: String) {
        var pos = 0

        fun atEnd() = pos >= s.length

        fun skipWs() {
            while (pos < s.length && s[pos].isWhitespace()) pos++
        }

        fun parseValue(): Any? {
            skipWs()
            if (atEnd()) throw IllegalArgumentException("Unerwartetes Ende")
            return when (val c = s[pos]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> parseLiteral("true", true)
                'f' -> parseLiteral("false", false)
                'n' -> parseLiteral("null", null)
                else -> parseNumber()
            }
        }

        private fun parseObject(): Map<String, Any?> {
            expect('{')
            val map = mutableMapOf<String, Any?>()
            skipWs()
            if (peek() == '}') { pos++; return map }
            while (true) {
                skipWs()
                val key = parseString()
                skipWs()
                expect(':')
                map[key] = parseValue()
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    '}' -> { pos++; return map }
                    else -> throw IllegalArgumentException("Erwartet , oder } an Position $pos")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            expect('[')
            val list = mutableListOf<Any?>()
            skipWs()
            if (peek() == ']') { pos++; return list }
            while (true) {
                list.add(parseValue())
                skipWs()
                when (peek()) {
                    ',' -> pos++
                    ']' -> { pos++; return list }
                    else -> throw IllegalArgumentException("Erwartet , oder ] an Position $pos")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw IllegalArgumentException("Unbeendeter String")
                val c = s[pos++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        val e = s[pos++]
                        when (e) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                sb.append(s.substring(pos, pos + 4).toInt(16).toChar())
                                pos += 4
                            }
                            else -> throw IllegalArgumentException("Ungültiges Escape \\$e")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun parseNumber(): Double {
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] in "+-.eE")) pos++
            return s.substring(start, pos).toDoubleOrNull()
                ?: throw IllegalArgumentException("Ungültige Zahl an Position $start")
        }

        private fun parseLiteral(lit: String, value: Any?): Any? {
            if (s.startsWith(lit, pos)) {
                pos += lit.length
                return value
            }
            throw IllegalArgumentException("Erwartet $lit an Position $pos")
        }

        private fun peek(): Char = if (atEnd()) throw IllegalArgumentException("Unerwartetes Ende") else s[pos]

        private fun expect(c: Char) {
            if (peek() != c) throw IllegalArgumentException("Erwartet '$c' an Position $pos")
            pos++
        }
    }
}
