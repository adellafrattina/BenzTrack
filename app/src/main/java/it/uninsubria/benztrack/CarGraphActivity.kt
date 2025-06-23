package it.uninsubria.benztrack

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.tasks.Tasks
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

class CarGraphActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_graph)
        loadingOverlay = findViewById(R.id.loading_overlay)

        val plate = intent.getStringExtra("plate")

        if (plate == null) {

            ToastManager.show(this, "Failed to identify car by plate", Toast.LENGTH_SHORT)
            finish()

            return
        }

        val refillDataTask = Handler.database.getRefillData(Handler.loggedUser!!.username, plate)
        val maintenanceDataTask = Handler.database.getMaintenanceData(Handler.loggedUser!!.username, plate)
        val insuranceDataTask = Handler.database.getInsuranceData(Handler.loggedUser!!.username, plate)
        val taxDataTask = Handler.database.getTaxData(Handler.loggedUser!!.username, plate)

        showLoading(true)

        Tasks.whenAll(refillDataTask, maintenanceDataTask, insuranceDataTask, taxDataTask)
            .addOnCompleteListener {

                showLoading(false)

                if (it.isSuccessful) {

                    val refillData = refillDataTask.result
                    val maintenanceData = maintenanceDataTask.result
                    val insuranceData = insuranceDataTask.result
                    val taxData = taxDataTask.result

                    setUpPieChart(refillData, maintenanceData, insuranceData, taxData)
                }

                else {

                    ToastManager.show(this, it.exception?.message, Toast.LENGTH_SHORT)
                    finish()
                }
            }
    }

    private fun setUpPieChart(refillData: ArrayList<Refill>, maintenanceData: ArrayList<Maintenance>, insuranceData: ArrayList<Insurance>, taxData: ArrayList<Tax>) {

        val noAvailableData = refillData.isEmpty() && maintenanceData.isEmpty() && insuranceData.isEmpty() && taxData.isEmpty()

        var refillAmount = 0f
        for (refill in refillData)
            refillAmount += refill.amount

        var maintenanceAmount = 0f
        for (maintenance in maintenanceData)
            maintenanceAmount += maintenance.amount

        var insuranceAmount = 0f
        for (insurance in insuranceData)
            insuranceAmount += insurance.amount

        var taxAmount = 0f
        for (tax in taxData)
            taxAmount += tax.amount

        val sum = refillAmount + maintenanceAmount + insuranceAmount + taxAmount
        val totalAmount = if (sum == 0.0f) 1f else sum

        val legendLabels = listOf("Refills", "Maintenance", "Insurance", "Tax")
        val amounts = listOf(refillAmount, maintenanceAmount, insuranceAmount, taxAmount)
        val colors = listOf(

            Color.parseColor("#FFA726"),
            Color.parseColor("#66BB6A"),
            Color.parseColor("#EF5350"),
            Color.parseColor("#29B6F6")
        )

        val legendEntries = legendLabels.mapIndexed { i, l ->

            val percent = amounts[i] / totalAmount * 100f
            LegendEntry().apply {
                formColor = colors[i]
                label = "$l - ${"%.1f".format(percent)}%"
            }
        }

        val entries = legendLabels.mapIndexed { i, l ->

            PieEntry(amounts[i], l)
        }

        val pieChart: PieChart = findViewById(R.id.pie_chart)

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = colors
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = if (noAvailableData) "No available data" else "Expenses"
        pieChart.setDrawEntryLabels(false)
        pieChart.setEntryLabelColor(Color.BLACK)
        pieChart.setUsePercentValues(false)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.animateY(1000)

        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.isEnabled = true
        legend.textSize = 14f
        legend.setCustom(legendEntries)

        pieChart.invalidate()
    }

    private fun showLoading(show: Boolean) {

        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private lateinit var loadingOverlay: View
}
