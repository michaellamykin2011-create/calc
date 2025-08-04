package com.example.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var input: String = ""
    private var lastNumeric: Boolean = false
    private var lastDot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvResult = findViewById(R.id.tvResult)

        // Числовые кнопки
        val buttons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )
        for (id in buttons) {
            findViewById<Button>(id).setOnClickListener {
                onDigit((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnDot).setOnClickListener { onDecimalPoint() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { onClear() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { onBack() }

        // Операторы и равно
        findViewById<Button>(R.id.btnPlus).setOnClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnMinus).setOnClickListener { onOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setOnClickListener { onOperator("*") }
        findViewById<Button>(R.id.btnDivide).setOnClickListener { onOperator("/") }
        findViewById<Button>(R.id.btnEqual).setOnClickListener { onEqual() }
    }

    private fun onDigit(digit: String) {
        input += digit
        lastNumeric = true
        updateResult()
    }

    private fun onDecimalPoint() {
        if (lastNumeric && !lastDot) {
            input += "."
            lastNumeric = false
            lastDot = true
            updateResult()
        }
    }

    private fun onOperator(op: String) {
        // Допускаем только один оператор подряд и только после цифры
        if (input.isNotEmpty() && lastNumeric) {
            input += op
            lastNumeric = false
            lastDot = false
            updateResult()
        }
    }

    private fun onClear() {
        input = ""
        lastNumeric = false
        lastDot = false
        updateResult()
    }

    private fun onBack() {
        if (input.isNotEmpty()) {
            val lastChar = input.last()
            input = input.dropLast(1)
            // Проверяем новый последний символ
            lastNumeric = input.isNotEmpty() && input.last().isDigit()
            lastDot = input.contains('.') && input.takeLastWhile { it != '+' && it != '-' && it != '*' && it != '/' }.contains('.')
            updateResult()
        }
    }

    private fun onEqual() {
        try {
            val normalized = input
                .replace('÷', '/')
                .replace('×', '*')
                .replace('–', '-')
            val result = eval(normalized)
            tvResult.text = result
        } catch (e: Exception) {
            tvResult.text = "Ошибка"
        }
    }

    private fun updateResult() {
        tvResult.text = input.ifEmpty { "0" }
    }

    // Парсер (оставь как у тебя)
    private fun eval(expr: String): String {
        return try {
            val result = object : Any() {
                var pos = -1
                var ch = 0

                fun nextChar() {
                    ch = if (++pos < expr.length) expr[pos].code else -1
                }

                fun eat(charToEat: Int): Boolean {
                    while (ch == ' '.code) nextChar()
                    if (ch == charToEat) {
                        nextChar()
                        return true
                    }
                    return false
                }

                fun parse(): Double {
                    nextChar()
                    val x = parseExpression()
                    if (pos < expr.length) throw RuntimeException("Unexpected: " + expr[pos])
                    return x
                }

                fun parseExpression(): Double {
                    var x = parseTerm()
                    while (true) {
                        when {
                            eat('+'.code) -> x += parseTerm()
                            eat('-'.code) -> x -= parseTerm()
                            else -> return x
                        }
                    }
                }

                fun parseTerm(): Double {
                    var x = parseFactor()
                    while (true) {
                        when {
                            eat('*'.code) -> x *= parseFactor()
                            eat('/'.code) -> x /= parseFactor()
                            else -> return x
                        }
                    }
                }

                fun parseFactor(): Double {
                    if (eat('+'.code)) return parseFactor()
                    if (eat('-'.code)) return -parseFactor()
                    var x: Double
                    val startPos = pos
                    if (eat('('.code)) {
                        x = parseExpression()
                        eat(')'.code)
                    } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                        while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                        x = expr.substring(startPos, pos).toDouble()
                    } else {
                        throw RuntimeException("Unexpected: " + ch.toChar())
                    }
                    return x
                }
            }.parse()
            if (result % 1.0 == 0.0) result.toInt().toString() else result.toString()
        } catch (e: Exception) {
            "Ошибка"
        }
    }
}
