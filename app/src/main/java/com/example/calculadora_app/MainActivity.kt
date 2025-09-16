package com.example.calculadora_app

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.DecimalFormat
import kotlin.math.exp

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var tvOperation: TextView

    private var currentInput: String = ""
    private var operand: Double? = null
    private var pendingOp: String? = null
    private var isAfterEquals = false
    private var historyList = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.txtResultado)
        tvOperation = findViewById(R.id.txtOperacao)

        // Botões de dígitos
        val digits = listOf(
            "0" to R.id.btn0,
            "1" to R.id.btn1,
            "2" to R.id.btn2,
            "3" to R.id.btn3,
            "4" to R.id.btn4,
            "5" to R.id.btn5,
            "6" to R.id.btn6,
            "7" to R.id.btn7,
            "8" to R.id.btn8,
            "9" to R.id.btn9,
            "." to R.id.btnPonto
        )
        digits.forEach { (digit, id) ->
            findViewById<Button>(id).setOnClickListener { appendDigit(digit) }
        }

        val ops = listOf("+" to R.id.btnSomar, "-" to R.id.btnSubtrair, "×" to R.id.btnMultiplicar, "÷" to R.id.btnDividir)
        ops.forEach { (op, id) ->
            findViewById<Button>(id).setOnClickListener { onOperator(op) }
        }


        // Botão igual
        findViewById<Button>(R.id.btnIgual).setOnClickListener { onEquals() }

        // Botão Clear
        findViewById<Button>(R.id.btnClear).setOnClickListener { clearAll() }

        // Botão backspace
        findViewById<Button>(R.id.btnBackspace).setOnClickListener { backspace() }

        // Botão Clear Entry
        findViewById<Button>(R.id.btnClearEntry).setOnClickListener { clearEntry() }

        // Botão Toggle sign
        findViewById<Button>(R.id.btnMaisMenos).setOnClickListener { toggleSign() }

        // Botão Histórico
        findViewById<Button>(R.id.btnHistory).setOnClickListener {
            if(historyList.isEmpty()) {
                Toast.makeText(this, "O histórico está vazio.", Toast.LENGTH_SHORT).show()
            } else {
                showHistoryDialog()
            }
        }

    }

    private fun onOperator(op: String) {
        if(isAfterEquals) {
            operand = currentInput.toDoubleOrNull()
            isAfterEquals = false
        }

        if(currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: return
            if(operand == null)  operand = value
            else operand = performOperation(operand!!, value, pendingOp)
        }
        pendingOp = op
        currentInput = ""
        updateDisplay()
    }

    private fun clearAll() {
        currentInput = ""
        operand = null
        pendingOp = null
        isAfterEquals = false
        updateDisplay()
    }

    private fun backspace() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun onEquals() {
        val currentOperand = operand
        val currentPendingOp = pendingOp
        if(currentOperand != null && currentPendingOp != null && currentInput.isNotEmpty()) {
            val secondValue = currentInput.toDoubleOrNull() ?: return
            val result = performOperation(currentOperand, secondValue, currentPendingOp)

            val expression = "${formatDouble(currentOperand)} $currentPendingOp ${formatDouble(secondValue)} ="
            val resultString = formatDouble(result)

            historyList.add("$expression $resultString")

            tvOperation.text = expression
            tvDisplay.text = resultString

            currentInput =  resultString
            operand = null
            pendingOp = null
            isAfterEquals = true
        }
    }

    private fun performOperation(a: Double,b: Double, op: String?): Double {
        return when (op) {
            "+" -> a + b
            "-" -> a - b
            "×" -> a * b
            "÷" -> if (b == 0.0) {
                Toast.makeText(this, "Divisão por zero", Toast.LENGTH_SHORT).show()
                a
            } else a / b
            else -> b
        }
    }

    private fun appendDigit(d: String) {
        if(isAfterEquals) {
            clearAll()
        }
        isAfterEquals = false

        if (d == "." && currentInput.contains(".")) return
        currentInput = if (currentInput == "0") d else currentInput + d
        updateDisplay()
    }

    private fun toggleSign() {
        if(currentInput.isNotEmpty()) {
            isAfterEquals = false

            val number = currentInput.toDoubleOrNull()
            if(number != null && number != 0.0) {
                currentInput = formatDouble(number * -1)
                updateDisplay()
            }
        }
    }
    private fun clearEntry() {
        currentInput = "0"
        isAfterEquals = false
        updateDisplay()
    }

    private fun formatDouble(number: Double): String {
        val formatter = DecimalFormat("0.##########")
        return formatter.format(number)
    }

    private fun updateDisplay(rotate: Boolean = false, expression: String = "") {
        if(isAfterEquals && !rotate) return
        tvDisplay.text = if (currentInput.isNotEmpty()) currentInput else (operand?.toString() ?: "0")

        val operationText = buildString {
            if(operand != null) {
                append(formatDouble(operand!!))
            }
            if(pendingOp != null) {
                append(" $pendingOp ")
            }
        }
        tvOperation.text = expression.ifEmpty { operationText }
    }

    private fun showHistoryDialog() {
        val historyArray = historyList.reversed().toTypedArray()

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Histórico de Cálculos")
            .setItems(historyArray, null)
            .setPositiveButton("Fechar") { dialog, _ ->
                dialog.dismiss()
            }

        val dialog = builder.create()
        dialog.show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("operand", operand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
        outState.putBoolean("isAfterEquals", isAfterEquals)
        outState.putStringArrayList("historyList", historyList)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput", "")
        val opnd = savedInstanceState.getDouble("operand", Double.NaN)
        operand = if (opnd.isNaN()) null else opnd
        pendingOp = savedInstanceState.getString("pendingOp")
        isAfterEquals = savedInstanceState.getBoolean("isAfterEquals", false)
        historyList = savedInstanceState.getStringArrayList("historyList")!!
        if(isAfterEquals) updateDisplay(true, historyList.get(historyList.size - 1))
        else updateDisplay(true)
    }
}