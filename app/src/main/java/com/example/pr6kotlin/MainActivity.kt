package com.example.pr6kotlin

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.text.Html
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

    private var editGrindingPn: EditText? = null // Шліфувальний верстат
    private var editPolishingKv: EditText? = null // Полірувальний верстат
    private var editSawTgPhi: EditText? = null   // Циркулярна пила

    private var textResult: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        scrollView.setFillViewport(true)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(40, 40, 40, 40)
        scrollView.addView(layout)

        val title = TextView(this)
        title.text = "Практ. Роб №6 - Метод впорядкованих діаграм"
        title.textSize = 20f
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 30)
        title.setTextColor(Color.BLACK)
        layout.addView(title)

        editGrindingPn = addInputField(layout, "Шліфувальний верстат Pн (кВт):", "24")
        editPolishingKv = addInputField(layout, "Полірувальний верстат Кв:", "0.25")
        editSawTgPhi = addInputField(layout, "Циркулярна пила tgφ:", "1.59")

        val btnCalculate = Button(this)
        btnCalculate.text = "РОЗРАХУВАТИ"
        btnCalculate.setBackgroundColor(Color.parseColor("#6200EE"))
        btnCalculate.setTextColor(Color.WHITE)

        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        btnParams.setMargins(0, 40, 0, 40)
        layout.addView(btnCalculate, btnParams)

        textResult = TextView(this)
        textResult!!.textSize = 14f
        textResult!!.setBackgroundColor(Color.parseColor("#EEEEEE"))
        textResult!!.setPadding(30, 30, 30, 30)
        textResult!!.setTextColor(Color.BLACK)
        layout.addView(textResult)

        setContentView(scrollView)

        btnCalculate.setOnClickListener { calculate() }
    }

    private fun addInputField(parent: LinearLayout, labelText: String, defaultValue: String): EditText {
        val label = TextView(this)
        label.text = labelText
        label.setTextColor(Color.DKGRAY)
        parent.addView(label)

        val input = EditText(this)
        input.setText(defaultValue)
        input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        parent.addView(input)
        return input
    }


    data class EpData(
        val name: String,
        val efficiency: Double, // ККД
        val cosPhi: Double,
        val uNom: Double, // Напруга
        val amount: Int,  // Кількість
        val pNom: Double, // Pн
        val kv: Double,   // Коеф. використання
        val tgPhi: Double
    )

    private fun calculate() {
        try {

            val grindingPn = editGrindingPn!!.text.toString().toDouble()
            val polishingKv = editPolishingKv!!.text.toString().toDouble()
            val sawTgPhi = editSawTgPhi!!.text.toString().toDouble()

            val listOfEp = listOf(
                EpData("Шліфувальний", 0.92, 0.9, 0.38, 4, grindingPn, 0.15, 1.33),
                EpData("Свердлильний", 0.92, 0.9, 0.38, 2, 14.0, 0.12, 1.0),
                EpData("Фугувальний", 0.92, 0.9, 0.38, 4, 42.0, 0.15, 1.33),
                EpData("Циркулярна пила", 0.92, 0.9, 0.38, 1, 36.0, 0.3, sawTgPhi),
                EpData("Прес", 0.92, 0.9, 0.38, 1, 20.0, 0.5, 0.75),
                EpData("Полірувальний", 0.92, 0.9, 0.38, 1, 40.0, polishingKv, 1.0),
                EpData("Фрезерний", 0.92, 0.9, 0.38, 2, 32.0, 0.2, 1.0),
                EpData("Вентилятор", 0.92, 0.9, 0.38, 1, 20.0, 0.65, 0.75)
            )

            // Суми для ШР
            var sumNPn = 0.0
            var sumNPnKv = 0.0
            var sumNPnKvTg = 0.0
            var sumNPn2 = 0.0

            for (ep in listOfEp) {
                val totalP = ep.amount * ep.pNom
                sumNPn += totalP
                sumNPnKv += totalP * ep.kv
                sumNPnKvTg += totalP * ep.kv * ep.tgPhi
                sumNPn2 += ep.amount * ep.pNom.pow(2)
            }

            val kTv_SR = sumNPnKv / sumNPn

            val n_e_SR = (sumNPn.pow(2)) / sumNPn2

            val kp_SR = getKpTable(n_e_SR.toInt(), kTv_SR)

            val pp_SR = kp_SR * sumNPnKv
            val qp_SR = sumNPnKvTg
            val sp_SR = sqrt(pp_SR.pow(2) + qp_SR.pow(2)) // Повна потужність
            val ip_SR = pp_SR / 0.38 // Розрахунковий струм


            val largeEpPn = 2 * 100 + 2 * 120
            val largeEpPc = (2 * 100 * 0.2) + (2 * 120 * 0.8)

            val qLarge = (2 * 100 * 0.2 * 3.0) + (2 * 120 * 0.8 * 0.5)

            val totalPn_Shop = (3 * sumNPn) + largeEpPn
            val totalPc_Shop = (3 * sumNPnKv) + largeEpPc
            val totalQc_Shop = (3 * sumNPnKvTg) + 216.0

            // Ефективна кількість для цеху
            val sumNPn2_Large = (2 * 100.0.pow(2)) + (2 * 120.0.pow(2))
            val totalSumNPn2_Shop = (3 * sumNPn2) + sumNPn2_Large

            val n_e_Shop = (totalPn_Shop.pow(2)) / totalSumNPn2_Shop
            val kTv_Shop = totalPc_Shop / totalPn_Shop

            // Kp для цеху
            val kp_Shop = getKpShop(n_e_Shop.toInt(), kTv_Shop)

            val pp_Shop = kp_Shop * totalPc_Shop
            val qp_Shop = kp_Shop * totalQc_Shop
            val sp_Shop = sqrt(pp_Shop.pow(2) + qp_Shop.pow(2))
            val ip_Shop = pp_Shop / 0.38

            // Форматування виводу
            val resultHtml = """
                <h3 align="center">РЕЗУЛЬТАТИ</h3>
                <br>
                <b>1. РОЗРАХУНОК ГРУПИ (ШР1):</b><br>
                Коеф. використання (Кв): <b>${String.format(Locale.US, "%.4f", kTv_SR)}</b><br>
                Ефективна кількість ЕП (n_e): <b>${String.format(Locale.US, "%.2f", n_e_SR)}</b><br>
                Коеф. розрахунковий (Кр): <b>${String.format(Locale.US, "%.2f", kp_SR)}</b><br>
                Навантаження Pр: <b>${String.format(Locale.US, "%.2f", pp_SR)} кВт</b><br>
                Навантаження Qр: <b>${String.format(Locale.US, "%.2f", qp_SR)} квар</b><br>
                Повна потужність Sр: <b>${String.format(Locale.US, "%.2f", sp_SR)} кВ·А</b><br>
                Струм Iр: <b>${String.format(Locale.US, "%.2f", ip_SR)} А</b><br>
                <br>
                <b>2. РОЗРАХУНОК ЦЕХУ (ВСЬОГО):</b><br>
                <i>(3 ШР + Зварювальні + Сушильні)</i><br>
                Коеф. використання цеху: <b>${String.format(Locale.US, "%.4f", kTv_Shop)}</b><br>
                Ефективна кількість (n_e): <b>${String.format(Locale.US, "%.2f", n_e_Shop)}</b><br>
                Коеф. розрахунковий цеху (Кр): <b>${String.format(Locale.US, "%.2f", kp_Shop)}</b><br>
                <b>Активне нав. (Pр): ${String.format(Locale.US, "%.2f", pp_Shop)} кВт</b><br>
                <b>Реактивне нав. (Qр): ${String.format(Locale.US, "%.2f", qp_Shop)} квар</b><br>
                <b>Повна потужність (Sр): ${String.format(Locale.US, "%.2f", sp_Shop)} кВ·А</b><br>
                <b>Струм шин 0.38кВ (Iр): ${String.format(Locale.US, "%.2f", ip_Shop)} А</b>
            """.trimIndent()

            textResult!!.text = Html.fromHtml(resultHtml, Html.FROM_HTML_MODE_LEGACY)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Помилка! Перевірте дані.", Toast.LENGTH_SHORT).show()
        }
    }

    // Спрощена функція вибору Кр для ШР
    private fun getKpTable(ne: Int, kv: Double): Double {
        if (ne > 100) return 1.0
        if (kv < 0.2) return 1.3
        if (kv < 0.4) return 1.15
        return 1.0
    }

    // Спрощена функція вибору Кр для Цеху
    private fun getKpShop(ne: Int, kv: Double): Double {
        if (ne > 50 && kv > 0.3) return 0.7
        return 0.75
    }
}