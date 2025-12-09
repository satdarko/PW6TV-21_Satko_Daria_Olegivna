package com.example.calc_pr6

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : Activity() {

    // Поля введення для Варіанту 6
    private lateinit var inputGrindingPn: EditText // Шліфувальний верстат (Pн)
    private lateinit var inputPolishingKv: EditText // Полірувальний верстат (Кв)
    private lateinit var inputSawTg: EditText      // Циркулярна пила (tg_phi)

    private lateinit var textResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        scrollView.isFillViewport = true
        scrollView.setBackgroundColor(Color.WHITE)

        val mainLayout = LinearLayout(this)
        mainLayout.orientation = LinearLayout.VERTICAL
        mainLayout.setPadding(40, 40, 40, 40)
        scrollView.addView(mainLayout)

        val title = TextView(this)
        title.text = "Калькулятор Електричних Навантажень"
        title.textSize = 22f
        title.typeface = Typeface.DEFAULT_BOLD
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.BLACK)
        title.setPadding(0, 0, 0, 40)
        mainLayout.addView(title)

        // Створення полів введення
        inputGrindingPn = createInput(mainLayout, "Шліфувальний верстат Pн (кВт)")
        inputPolishingKv = createInput(mainLayout, "Полірувальний верстат Kv")
        inputSawTg = createInput(mainLayout, "Циркулярна пила tgφ")
        // Дозволяємо десяткові числа для всіх полів
        inputPolishingKv.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        inputSawTg.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val btnAutofill = Button(this)
        btnAutofill.text = "Автозаповнення (Варіант 6)"
        btnAutofill.setBackgroundColor(Color.parseColor("#03DAC5")) // Бірюзовий
        btnAutofill.setTextColor(Color.WHITE)
        val paramsFill = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 140
        )
        paramsFill.setMargins(0, 30, 0, 20)
        btnAutofill.layoutParams = paramsFill
        mainLayout.addView(btnAutofill)

        val btnCalc = Button(this)
        btnCalc.text = "Розрахувати показники"
        btnCalc.setBackgroundColor(Color.parseColor("#6200EE"))
        btnCalc.setTextColor(Color.WHITE)
        val paramsCalc = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 140
        )
        paramsCalc.setMargins(0, 0, 0, 40)
        btnCalc.layoutParams = paramsCalc
        mainLayout.addView(btnCalc)

        textResult = TextView(this)
        textResult.textSize = 16f
        textResult.setTextColor(Color.BLACK)
        textResult.setBackgroundColor(Color.parseColor("#F5F5F5")) // Сірий фон
        textResult.setPadding(30, 30, 30, 30)
        mainLayout.addView(textResult)

        setContentView(scrollView)

        // Кнопка автозаповнення для Варіанту 6 (Таблиця 6.8)
        btnAutofill.setOnClickListener {
            inputGrindingPn.setText("25")   // Pн змінено
            inputPolishingKv.setText("0.26") // Kv змінено
            inputSawTg.setText("1.61")      // tg змінено
        }

        btnCalc.setOnClickListener {
            calculate()
        }
    }

    private fun createInput(parent: LinearLayout, hintText: String): EditText {
        val label = TextView(this)
        label.text = hintText
        label.setTextColor(Color.DKGRAY)
        label.textSize = 14f
        parent.addView(label)

        val editText = EditText(this)
        editText.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        editText.setTextColor(Color.BLACK)
        parent.addView(editText)

        val spacer = TextView(this)
        spacer.height = 20
        parent.addView(spacer)

        return editText
    }

    // Внутрішній клас для даних ЕП
    data class Ep(
        val name: String,
        val amount: Int,
        val pn: Double,
        val kv: Double,
        val tg: Double
    )

    private fun calculate() {
        try {
            // Отримання даних
            val pGrinding = inputGrindingPn.text.toString().toDouble()
            val kvPolishing = inputPolishingKv.text.toString().toDouble()
            val tgSaw = inputSawTg.text.toString().toDouble()

            // 1. Формування списку ЕП для одного ШР (дані з табл. 6.6 + Варіант)
            val equipment = listOf(
                Ep("Шліфувальний", 4, pGrinding, 0.15, 1.33),
                Ep("Свердлильний", 2, 14.0, 0.12, 1.0),
                Ep("Фугувальний", 4, 42.0, 0.15, 1.33),
                Ep("Циркулярна", 1, 36.0, 0.3, tgSaw),
                Ep("Прес", 1, 20.0, 0.5, 0.75),
                Ep("Полірувальний", 1, 40.0, kvPolishing, 1.0),
                Ep("Фрезерний", 2, 32.0, 0.2, 1.0),
                Ep("Вентилятор", 1, 20.0, 0.65, 0.75)
            )

            // --- РОЗРАХУНОК ШР1 (Розподільчий пункт) ---
            var sumPn = 0.0
            var sumPnKv = 0.0
            var sumPnKvTg = 0.0
            var sumPn2 = 0.0

            for (ep in equipment) {
                val totalP = ep.amount * ep.pn
                sumPn += totalP
                sumPnKv += totalP * ep.kv
                sumPnKvTg += totalP * ep.kv * ep.tg
                sumPn2 += ep.amount * ep.pn.pow(2)
            }

            val kvGroup = sumPnKv / sumPn
            val ne = sumPn.pow(2) / sumPn2

            // Визначення Kp за табл. 6.3 (спрощено)
            val kp = if (ne > 10) {
                if (kvGroup < 0.2) 1.25 else 1.0 // Як у прикладі
            } else {
                if (kvGroup < 0.2) 2.64 else 1.5 // Приклад для малих ne
            }

            val pp = kp * sumPnKv
            val qp = sumPnKvTg // Для ne > 10, Qp = Qc
            val sp = sqrt(pp.pow(2) + qp.pow(2))
            val ip = pp / 0.38

            // --- РОЗРАХУНОК ЦЕХУ (3 ШР + Великі ЕП) ---
            // Великі ЕП (Зварювальні + Сушильні)
            // Припускаємо стандартні дані з контрольного прикладу для великих ЕП
            // Зварювальний: 2 шт, Pn=100, Kv=0.2, tg=3.0
            // Сушильна шафа: 2 шт, Pn=120, Kv=0.8, tg=?? (розрахункова Qc з прикладу)

            val largePn = (2 * 100) + (2 * 120)
            val largePc = (2 * 100 * 0.2) + (2 * 120 * 0.8)
            // Qc великих (Зварка ~120 + Шафа ~100 = ~220)
            val largeQc = (2 * 100 * 0.2 * 3.0) + (2 * 120 * 0.8 * 0.5) // припустимо tg=0.5 для шафи

            val totalPn = (3 * sumPn) + largePn
            val totalPc = (3 * sumPnKv) + largePc
            val totalQc = (3 * sumPnKvTg) + largeQc

            // Ефективна кількість для цеху
            val largePn2 = (2 * 100.0.pow(2)) + (2 * 120.0.pow(2))
            val totalNe = totalPn.pow(2) / ((3 * sumPn2) + largePn2)

            val totalKv = totalPc / totalPn
            // Kp цеху (табл 6.4, магістральні шини) -> 0.7
            val totalKp = 0.7

            val totalPp = totalKp * totalPc
            val totalQp = totalKp * totalQc
            val totalSp = sqrt(totalPp.pow(2) + totalQp.pow(2))
            val totalIp = totalPp / 0.38

            val res = String.format(Locale.US,
                "РЕЗУЛЬТАТИ (ВАРІАНТ 6):\n\n" +
                        "1. ГРУПОВЕ НАВАНТАЖЕННЯ (ШР1):\n" +
                        "Коеф. використання (Kv): %.4f\n" +
                        "Еф. кількість ЕП (ne): %.2f\n" +
                        "Коеф. розрахунковий (Kp): %.2f\n" +
                        "Активне (Pp): %.2f кВт\n" +
                        "Реактивне (Qp): %.2f квар\n" +
                        "Повна (Sp): %.2f кВ·А\n" +
                        "Струм (Ip): %.2f А\n\n" +
                        "2. НАВАНТАЖЕННЯ ЦЕХУ:\n" +
                        "Коеф. використання: %.4f\n" +
                        "Еф. кількість (ne): %.2f\n" +
                        "Коеф. розрахунковий (Kp): %.2f\n" +
                        "Активне (Pp): %.2f кВт\n" +
                        "Реактивне (Qp): %.2f квар\n" +
                        "Повна (Sp): %.2f кВ·А\n" +
                        "Струм на шинах (Ip): %.2f А",
                kvGroup, ne, kp, pp, qp, sp, ip,
                totalKv, totalNe, totalKp, totalPp, totalQp, totalSp, totalIp
            )

            textResult.text = res

        } catch (e: Exception) {
            Toast.makeText(this, "Помилка! Перевірте дані.", Toast.LENGTH_SHORT).show()
        }
    }
}