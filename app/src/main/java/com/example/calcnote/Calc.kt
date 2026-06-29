package com.example.calcnote

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Bitta qator ifodani ANIQ hisoblaydi (BigDecimal => kasrli xato yo'q).
 * Misol: "2360 * 16", "12500 * 1.6", "(100 + 50) / 2", "17 mod 5", "Ans + 100"
 * Amallar:  + - * / ( )  mod  %  Ans
 * Belgilar:  *  yoki  ×    /  yoki  ÷
 * Bo'sh / xato / izoh (# bilan boshlangan) => null  (Jami'ga qo'shilmaydi)
 */
object Calc {

    private val MC = MathContext(20, RoundingMode.HALF_UP)

    fun eval(line: String, ans: BigDecimal = BigDecimal.ZERO): BigDecimal? {
        val cleaned = line.replace(",", "").trim()
        if (cleaned.isEmpty()) return null
        if (cleaned.startsWith("#")) return null
        return try {
            Parser(cleaned, ans, MC).parse()
        } catch (e: Exception) {
            null
        }
    }
}

private class Parser(
    private val s: String,
    private val ans: BigDecimal,
    private val mc: MathContext
) {
    private var pos = 0

    private fun peek(): Char = if (pos < s.length) s[pos] else '\u0000'
    private fun skip() { while (pos < s.length && s[pos] == ' ') pos++ }

    fun parse(): BigDecimal {
        val v = expr()
        skip()
        if (pos < s.length) throw IllegalStateException("ortiqcha belgi")
        return v
    }

    // + va -
    private fun expr(): BigDecimal {
        var v = term()
        while (true) {
            skip()
            when (peek()) {
                '+' -> { pos++; v = v.add(term()) }
                '-' -> { pos++; v = v.subtract(term()) }
                else -> return v
            }
        }
    }

    // * / mod %
    private fun term(): BigDecimal {
        var v = factor()
        while (true) {
            skip()
            val c = peek()
            when {
                c == '*' || c == '×' || c == '·' -> { pos++; v = v.multiply(factor()) }
                c == '/' || c == '÷' || c == ':' -> { pos++; v = v.divide(factor(), mc) }
                c == '%' -> { pos++; v = v.remainder(factor()) }
                matchWord("mod") -> { v = v.remainder(factor()) }
                else -> return v
            }
        }
    }

    // son, qavs, unar +/-, Ans
    private fun factor(): BigDecimal {
        skip()
        when (peek()) {
            '+' -> { pos++; return factor() }
            '-' -> { pos++; return factor().negate() }
            '(' -> {
                pos++
                val v = expr()
                skip()
                if (peek() != ')') throw IllegalStateException("qavs yopilmagan")
                pos++
                return v
            }
        }
        if (matchWord("ans")) return ans
        return number()
    }

    private fun number(): BigDecimal {
        skip()
        val start = pos
        while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
        if (pos == start) throw IllegalStateException("son kutilgan")
        return BigDecimal(s.substring(start, pos))
    }

    private fun matchWord(w: String): Boolean {
        skip()
        val end = pos + w.length
        if (end <= s.length && s.substring(pos, end).lowercase() == w) {
            val next = if (end < s.length) s[end] else ' '
            if (!next.isLetter()) { pos = end; return true }
        }
        return false
    }
}

/** Natijani chiroyli ko'rsatadi: 37760 -> "37,760", 1.60 -> "1.6" */
fun formatBD(v: BigDecimal): String {
    val rounded = v.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros()
    val plain = rounded.toPlainString()
    val neg = plain.startsWith("-")
    val core = if (neg) plain.substring(1) else plain
    val dot = core.indexOf('.')
    val intPart = if (dot >= 0) core.substring(0, dot) else core
    val decPart = if (dot >= 0) core.substring(dot) else ""
    return (if (neg) "-" else "") + groupDigits(intPart) + decPart
}

private fun groupDigits(intPart: String): String {
    if (intPart.length <= 3) return intPart
    val sb = StringBuilder()
    val rem = intPart.length % 3
    var i = 0
    if (rem > 0) {
        sb.append(intPart.substring(0, rem))
        i = rem
        if (i < intPart.length) sb.append(",")
    }
    while (i < intPart.length) {
        sb.append(intPart.substring(i, i + 3))
        i += 3
        if (i < intPart.length) sb.append(",")
    }
    return sb.toString()
}
