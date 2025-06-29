package it.uninsubria.benztrack

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class CarGraphActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        setContentView(R.layout.activity_car_graph)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val plate = intent.getStringExtra("car_plate")

        loadingOverlay = findViewById(R.id.loading_overlay)
        val button: Button = findViewById(R.id.button_car_info)
        button.setOnClickListener {

            val intent = Intent(this, CarInfoActivity::class.java)
            intent.putExtra("car_plate", plate)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        val title: TextView = findViewById(R.id.text_car_name)
        val text: TextView = findViewById(R.id.text_car_model)

        if (plate == null) {

            ToastManager.show(this, "Failed to identify car by plate", Toast.LENGTH_SHORT)
            finish()

            return
        }

        val refillDataTask = Handler.database.getRefillData(Handler.loggedUser!!.username, plate)
        val maintenanceDataTask = Handler.database.getMaintenanceData(Handler.loggedUser!!.username, plate)
        val insuranceDataTask = Handler.database.getInsuranceData(Handler.loggedUser!!.username, plate)
        val taxDataTask = Handler.database.getTaxData(Handler.loggedUser!!.username, plate)
        val carTask = Handler.database.getUserCar(Handler.loggedUser!!.username, plate)
        val modelTask = Handler.database.getUserCarModel(Handler.loggedUser!!.username, plate)

        showLoading(true)

        Tasks.whenAll(refillDataTask, maintenanceDataTask, insuranceDataTask, taxDataTask, carTask, modelTask)
            .addOnCompleteListener {

                if (it.isSuccessful) {

                    val refillData = refillDataTask.result
                    val maintenanceData = maintenanceDataTask.result
                    val insuranceData = insuranceDataTask.result
                    val taxData = taxDataTask.result
                    val car = carTask.result
                    val model = modelTask.result

                    title.text = car.name + " - " + car.plate

                    val fuelString = when (model.fuel) {
                        FuelType.Petrol -> "Petrol"
                        FuelType.Diesel -> "Diesel"
                        FuelType.Electric -> "Electric"
                    }

                    text.text = "${model.name} (${model.year}, $fuelString)\nW ${model.width} cm | L ${model.length} | H ${model.height} | M ${model.weight} kg\nCO2 ${model.co2factor} g/km | Capacity ${model.capacity} cm3"

                    setUpPieChart(refillData, maintenanceData, insuranceData, taxData)
                    setUpLineChart(refillData, model)
                }

                else {

                    ToastManager.show(this, it.exception?.message, Toast.LENGTH_SHORT)
                    finish()
                }

                showLoading(false)
            }
    }

    override fun onSupportNavigateUp(): Boolean {

        finish()
        return true
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
        pieChart.holeRadius = if (noAvailableData) 100f else 40f
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

    private fun setUpLineChart(refills: ArrayList<Refill>, model: CarModel) {

        val noAvailableData = refills.size < 3

        // Find the LineChart view from XML
        val lineChart: LineChart = findViewById(R.id.line_chart)

        val entries = mutableListOf<Entry>()

        val co2Emissions = ArrayList<Float>()

        if (noAvailableData) {

            entries.add(Entry(0.0f, 0.0f))
        }

        else {

            var prevRefill = refills[0]
            for (i in 1 until refills.size) {

                val currentRefill = refills[i]

                val consumedLiters =
                    prevRefill.currentfuelamount + prevRefill.amount / prevRefill.ppl - currentRefill.currentfuelamount
                val travelledKm =
                    if (currentRefill.mileage - prevRefill.mileage > 0) currentRefill.mileage - prevRefill.mileage else 1.0f
                val daysInterval =
                    if (daysBetweenDates(currentRefill.date, prevRefill.date) > 0) daysBetweenDates(
                        currentRefill.date,
                        prevRefill.date
                    ) else 1
                val emittedCO2 =
                    (((consumedLiters * model.fuel.value) / travelledKm) / daysInterval) * 1000.0f
                co2Emissions.add(emittedCO2)

                prevRefill = currentRefill
            }

            // Set up time data
            for (i in co2Emissions.indices)
                entries.add(Entry(refills[i + 1].date.seconds * 1000f, co2Emissions[i]))
        }

        // Set up x axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = object : ValueFormatter() {
            private val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
            override fun getFormattedValue(value: Float): String {
                return sdf.format(Date(value.toLong()))
            }
        }

        xAxis.setLabelCount(entries.size, true)
        xAxis.granularity = 1.0f

        // Create a LineDataSet (this holds the data and settings for the line)
        val dataSet = LineDataSet(entries, "CO2 emissions (g/km per day)")
        dataSet.color = resources.getColor(android.R.color.holo_blue_dark)
        dataSet.valueTextColor = resources.getColor(android.R.color.black)

        // Create LineData using the LineDataSet
        val lineData = LineData(dataSet)

        // Set the data to the LineChart
        lineChart.data = lineData

        // Customize chart appearance

        val description = Description()
        description.text = if(noAvailableData) "Not enough data" else "CO2 emission graph"
        description.textSize = 14f

        lineChart.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {

                val paint = Paint().apply {
                    textSize = description.textSize
                    typeface = Typeface.DEFAULT
                }

                val textWidth = paint.measureText(description.text)

                val centerX = lineChart.width / 2f + textWidth / 2f
                val topPadding = 30f

                description.setPosition(centerX, topPadding)
                lineChart.description = description

                lineChart.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })

        // Apply the description to the chart
        lineChart.description = description

        lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

            override fun onValueSelected(e: Entry?, h: Highlight?) {
                e?.let {

                    val sdf = SimpleDateFormat("HH:mm:ss - dd/MM/2025", Locale.getDefault())
                    val value = it.y
                    val xValue = sdf.format(it.x.toLong())

                    // Show a popup dialog
                    AlertDialog.Builder(this@CarGraphActivity)
                        .setTitle("Refill info")
                        .setMessage("Time and Date: $xValue\n\nCO2: $value g/km per day")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            override fun onNothingSelected() {}
        })

        // Refresh the chart
        lineChart.invalidate()
    }

    private fun daysBetweenDates(ts1: Timestamp, ts2: Timestamp): Long {

        val zoneId = ZoneId.systemDefault()

        val date1 = Instant.ofEpochSecond(ts1.seconds).atZone(zoneId).toLocalDate()
        val date2 = Instant.ofEpochSecond(ts2.seconds).atZone(zoneId).toLocalDate()

        return ChronoUnit.DAYS.between(date1, date2)
    }

    private fun showLoading(show: Boolean) {

        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private lateinit var loadingOverlay: View
}
