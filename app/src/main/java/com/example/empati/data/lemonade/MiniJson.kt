package com.example.empati.data.lemonade

import kotlin.math.floor

/**
 * Parser dan penulis JSON kecil tanpa dependensi. Dipakai lapisan Lemonade karena
 * payload yang diterima hanya berbentuk objek, array, string, angka, dan boolean
 * sederhana. Menghindari `org.json` supaya kode ini bisa diuji di JVM tanpa Android.
 */
object MiniJson {

    fun parse(text: String): Any? = Parser(text).parseDocument()

    fun write(value: Any?): String = StringBuilder().also { writeValue(it, value) }.toString()

    fun quote(value: String): String = StringBuilder().also { writeString(it, value) }.toString()

    private class Parser(private val source: String) {
        private var index = 0

        fun parseDocument(): Any? {
            val value = parseValue()
            skipWhitespace()
            if (index != source.length) {
                throw IllegalArgumentException("trailing content at $index")
            }
            return value
        }

        private fun parseValue(): Any? {
            skipWhitespace()
            if (index >= source.length) throw IllegalArgumentException("unexpected end of input")
            return when (val c = source[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> parseString()
                't' -> literal("true", true)
                'f' -> literal("false", false)
                'n' -> literal("null", null)
                '-', in '0'..'9' -> parseNumber()
                else -> throw IllegalArgumentException("unexpected character '$c' at $index")
            }
        }

        private fun parseObject(): Map<String, Any?> {
            index++
            val result = LinkedHashMap<String, Any?>()
            skipWhitespace()
            if (peek() == '}') {
                index++
                return result
            }
            while (true) {
                skipWhitespace()
                val key = parseString()
                skipWhitespace()
                expect(':')
                result[key] = parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    '}' -> { index++; return result }
                    else -> throw IllegalArgumentException("expected ',' or '}' at $index")
                }
            }
        }

        private fun parseArray(): List<Any?> {
            index++
            val result = ArrayList<Any?>()
            skipWhitespace()
            if (peek() == ']') {
                index++
                return result
            }
            while (true) {
                result += parseValue()
                skipWhitespace()
                when (peek()) {
                    ',' -> index++
                    ']' -> { index++; return result }
                    else -> throw IllegalArgumentException("expected ',' or ']' at $index")
                }
            }
        }

        private fun parseString(): String {
            expect('"')
            val builder = StringBuilder()
            while (true) {
                if (index >= source.length) throw IllegalArgumentException("unterminated string")
                when (val c = source[index++]) {
                    '"' -> return builder.toString()
                    '\\' -> {
                        if (index >= source.length) throw IllegalArgumentException("unterminated escape")
                        when (val escaped = source[index++]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 4 > source.length) {
                                    throw IllegalArgumentException("truncated unicode escape")
                                }
                                val hex = source.substring(index, index + 4)
                                builder.append(hex.toInt(16).toChar())
                                index += 4
                            }
                            else -> throw IllegalArgumentException("bad escape '\\$escaped'")
                        }
                    }
                    else -> builder.append(c)
                }
            }
        }

        private fun parseNumber(): Double {
            val start = index
            if (peek() == '-') index++
            while (index < source.length && source[index].isDigit()) index++
            if (index < source.length && source[index] == '.') {
                index++
                while (index < source.length && source[index].isDigit()) index++
            }
            if (index < source.length && (source[index] == 'e' || source[index] == 'E')) {
                index++
                if (index < source.length && (source[index] == '+' || source[index] == '-')) index++
                while (index < source.length && source[index].isDigit()) index++
            }
            return source.substring(start, index).toDouble()
        }

        private fun literal(word: String, value: Any?): Any? {
            if (!source.startsWith(word, index)) throw IllegalArgumentException("bad literal at $index")
            index += word.length
            return value
        }

        private fun skipWhitespace() {
            while (index < source.length && source[index].isWhitespace()) index++
        }

        private fun peek(): Char = if (index < source.length) source[index] else '\u0000'

        private fun expect(c: Char) {
            if (index >= source.length || source[index] != c) {
                throw IllegalArgumentException("expected '$c' at $index")
            }
            index++
        }
    }

    private fun writeValue(out: StringBuilder, value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> writeString(out, value)
            is Boolean -> out.append(value)
            is Int, is Long, is Short, is Byte -> out.append(value.toString())
            is Float -> {
                if (value.isNaN() || value.isInfinite()) out.append("null")
                else if (value.toDouble() == floor(value.toDouble()) && kotlin.math.abs(value.toDouble()) < 1e15) out.append(value.toLong().toString())
                else out.append(value.toString())
            }
            is Double -> {
                if (value.isNaN() || value.isInfinite()) out.append("null")
                else if (value == floor(value) && kotlin.math.abs(value) < 1e15) out.append(value.toLong().toString())
                else out.append(value.toString())
            }
            is Map<*, *> -> {
                out.append('{')
                var first = true
                for ((k, v) in value) {
                    if (!first) out.append(',')
                    first = false
                    writeString(out, k.toString())
                    out.append(':')
                    writeValue(out, v)
                }
                out.append('}')
            }
            is Iterable<*> -> {
                out.append('[')
                var first = true
                for (item in value) {
                    if (!first) out.append(',')
                    first = false
                    writeValue(out, item)
                }
                out.append(']')
            }
            is Array<*> -> writeValue(out, value.toList())
            is ShortArray -> writeValue(out, value.toList())
            is DoubleArray -> writeValue(out, value.toList())
            is FloatArray -> writeValue(out, value.toList())
            else -> writeString(out, value.toString())
        }
    }

    private fun writeString(out: StringBuilder, value: String) {
        out.append('"')
        for (c in value) {
            when (c) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else -> if (c < ' ') out.append("\\u").append(c.code.toString(16).padStart(4, '0')) else out.append(c)
            }
        }
        out.append('"')
    }
}
