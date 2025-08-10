package com.example.calculator

import android.graphics.drawable.AnimationDrawable
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private var input: String = ""
    private var lastNumeric: Boolean = false
    private var lastDot: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- ИСПРАВЛЕННЫЙ КОД ДЛЯ ЗАПУСКА АНИМАЦИИ ---
        val rootLayout = findViewById<LinearLayout>(R.id.root_layout) // Находим наш LinearLayout по новому id
        val animDrawable = rootLayout.background as AnimationDrawable
        animDrawable.setEnterFadeDuration(10)
        animDrawable.setExitFadeDuration(5000)
        animDrawable.start()
        // --- Конец исправления ---

        tvResult = findViewById(R.id.tvResult)
        setupClickListeners()
    }

    private fun setupClickListeners() {
        val numericButtons = listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5,
            R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        )

        for (id in numericButtons) {
            findViewById<Button>(id).setHapticClickListener {
                onDigit((it as Button).text.toString())
            }
        }

        findViewById<Button>(R.id.btnDot).setHapticClickListener { onDecimalPoint() }
        findViewById<Button>(R.id.btnClear).setHapticClickListener { onClear() }
        findViewById<Button>(R.id.btnBack).setHapticClickListener { onBack() }
        findViewById<Button>(R.id.btnPercent).setHapticClickListener { onOperator("%") }
        findViewById<Button>(R.id.btnPlus).setHapticClickListener { onOperator("+") }
        findViewById<Button>(R.id.btnMinus).setHapticClickListener { onOperator("-") }
        findViewById<Button>(R.id.btnMultiply).setHapticClickListener { onOperator("×") }
        findViewById<Button>(R.id.btnDivide).setHapticClickListener { onOperator("÷") }
        findViewById<Button>(R.id.btnEqual).setHapticClickListener { onEqual() }
        findViewById<Button>(R.id.btnSign).setHapticClickListener { /* Логика для смены знака */ }
    }

    private fun View.setHapticClickListener(action: (View) -> Unit) {
        this.setOnClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            action(view)
        }
    }

    private fun onDigit(digit: String) {
        if (input == "Ошибка") {
            input = ""
        }
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
            input = input.dropLast(1)
            lastNumeric = input.isNotEmpty() && input.last().isDigit()
            lastDot = input.contains('.') && input.takeLastWhile { it != '+' && it != '-' && it != '×' && it != '÷' && it != '%' }.contains('.')
            updateResult()
        }
    }

    private fun onEqual() {
        if (lastNumeric) {
            try {
                val normalized = input
                    .replace("÷", "/")
                    .replace("×", "*")

                val result = if (normalized.contains("%")) {
                    val parts = normalized.split(Regex("(?=[+\\-*/])"))
                    val number = parts.first().toDouble()
                    val percent = parts.last().removePrefix("%").toDouble()
                    number * (percent / 100)
                } else {
                    eval(normalized)
                }

                input = formatResult(result)
                tvResult.text = input
                lastNumeric = true
                lastDot = input.contains('.')

            } catch (e: Exception) {
                tvResult.text = "Ошибка"
                input = "Ошибка"
            }
        }
    }

    private fun formatResult(result: Double): String {
        val formatted = String.format(Locale.US, "%.8f", result)
            .trimEnd('0')
            .trimEnd('.')
        return if (formatted == "-0") "0" else formatted
    }

    private fun updateResult() {
        tvResult.text = input.ifEmpty { "0" }
    }

    private fun eval(expr: String): Double {
        return object : Any() {
            var pos = -1
            var ch = 0
            fun nextChar() { ch = if (++pos < expr.length) expr[pos].code else -1 }
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
    }
}