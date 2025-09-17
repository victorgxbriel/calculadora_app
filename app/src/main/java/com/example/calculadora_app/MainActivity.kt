package com.example.calculadora_app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    private lateinit var tvDisplay: TextView
    private lateinit var tvOperation: TextView
    private var btnMC: Button? = null
    private var btnMR: Button? = null

    private var currentInput: String = ""
    private var operand: Double? = null
    private var pendingOp: String? = null
    private var isAfterEquals = false
    private var historyList = ArrayList<String>()
    private var memoryValue: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvDisplay = findViewById(R.id.txtResultado)
        tvOperation = findViewById(R.id.txtOperacao)

        btnMC = findViewById(R.id.btnMC)
        btnMR = findViewById(R.id.btnMR)

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

        // Botão elevar ao quadrado
        findViewById<Button>(R.id.btnSquare)?.setOnClickListener { onSquare() }

        // Botão de inverter
        findViewById<Button>(R.id.btnReverse)?.setOnClickListener { onReverse() }

        val mems = listOf(
            "MC" to R.id.btnMC,
            "MR" to R.id.btnMR,
            "M+" to R.id.btnMAdd,
            "M-" to R.id.btnMSub,
            "MS" to R.id.btnMS
        )
        mems.forEach { (mem, id) ->
            findViewById<Button>(id)?.setOnClickListener { memoryOp(mem) }
        }

        updateMemoryButtonsState()
    }

    private fun memoryOp(mem: String) {
        val displayValue = getDisplayValue() ?: return

        when (mem) {
            "MS" -> { // Memory Store
                memoryValue = displayValue
            }
            "MC" -> { // Memory Clear
                memoryValue = null
            }
            "MR" -> { // Memory Recall
                if (memoryValue != null) {
                    currentInput = formatDouble(memoryValue!!)
                    operand = null
                    pendingOp = null
                    updateDisplay()
                }
            }
            "M+" -> { // Memory Add
                if (memoryValue != null) {
                    memoryValue = memoryValue!! + displayValue
                } else {
                    // Se a memória está vazia, M+ se comporta como MS
                    memoryValue = displayValue
                }
                isAfterEquals = true
            }
            "M-" -> { // Memory Subtract
                if (memoryValue != null) {
                    memoryValue = memoryValue!! - displayValue
                } else {
                    // Se a memória está vazia, M- armazena o valor negativo
                    memoryValue = -displayValue
                }
                isAfterEquals = true
            }
        }
        updateMemoryButtonsState()
    }

    private fun onSquare() {
        performUnaryOperation("sqr") { it * it }
    }

    private fun onReverse() {
        performUnaryOperation("1/") {
            if (it == 0.0) {
                Toast.makeText(this, "Não é possível dividir por zero", Toast.LENGTH_SHORT).show()
                it
            } else {
                1 / it
            }
        }
    }

    private fun performUnaryOperation(symbol: String, operation: (Double) -> Double) {
        if (currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: return

            val result = operation(value)

            val expression = "$symbol(${formatDouble(value)})"
            val resultString = formatDouble(result)

            historyList.add("$expression = $resultString")

            tvOperation.text = expression
            tvDisplay.text = resultString

            currentInput = resultString
            operand = null
            pendingOp = null
            isAfterEquals = true
        }
    }

    private fun onOperator(op: String) {
        if(isAfterEquals) {
            operand = currentInput.toDoubleOrNull()
            isAfterEquals = false
        }

        if(currentInput.isNotEmpty()) {
            val value = currentInput.toDoubleOrNull() ?: return
            operand = if(operand == null) value
            else performOperation(operand!!, value, pendingOp)
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
        tvDisplay.text = currentInput.ifEmpty { (operand?.toString() ?: "0") }

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

    private fun getDisplayValue(): Double? {
        return tvDisplay.text.toString().toDoubleOrNull()
    }

    private fun updateMemoryButtonsState() {
        val isMemorySet = memoryValue != null
        btnMC?.isEnabled = isMemorySet
        btnMR?.isEnabled = isMemorySet
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentInput", currentInput)
        outState.putDouble("operand", operand ?: Double.NaN)
        outState.putString("pendingOp", pendingOp)
        outState.putBoolean("isAfterEquals", isAfterEquals)
        outState.putStringArrayList("historyList", historyList)
        outState.putDouble("memoryValue", memoryValue ?: Double.NaN)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentInput = savedInstanceState.getString("currentInput", "")
        val opnd = savedInstanceState.getDouble("operand", Double.NaN)
        operand = if (opnd.isNaN()) null else opnd
        pendingOp = savedInstanceState.getString("pendingOp")
        isAfterEquals = savedInstanceState.getBoolean("isAfterEquals", false)
        historyList = savedInstanceState.getStringArrayList("historyList")!!
        val mem = savedInstanceState.getDouble("memoryValue", Double.NaN)
        memoryValue = if (mem.isNaN()) null else mem
        updateMemoryButtonsState()
        if(isAfterEquals) updateDisplay(true, historyList[historyList.size - 1])
        else updateDisplay(true)
    }
}