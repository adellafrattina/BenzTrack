package it.uninsubria.benztrack

import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
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
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.ChartTouchListener
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

class CarGraphActivity : AppCompatActivity() {

    override fun onResume() {
        super.onResume()
        setContentView(R.layout.activity_car_graph)
        Handler.database.setContext(this)

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

            ToastManager.show(this, getString(R.string.error), Toast.LENGTH_SHORT)
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

                        FuelType.Petrol -> getString(R.string.petrol)
                        FuelType.Diesel -> getString(R.string.diesel)
                        FuelType.LPG -> getString(R.string.lpg)
                    }

                    //text.text = "${model.name} (${model.year}, $fuelString)\nW ${model.width} cm | L ${model.length} | H ${model.height} | M ${model.weight} kg\nCO2 ${model.co2factor} g/km | Capacity ${model.capacity} cm3"
                    text.text = "${model.name} (${model.year}, $fuelString)"
                    text.setOnClickListener {

                        // Show a popup dialog
                        AlertDialog.Builder(this@CarGraphActivity)
                            .setTitle(text.text)
                            .setMessage(
                                getString(R.string.width_cm) + ": ${model.width}\n" +
                                getString(R.string.length_cm) + ": ${model.length}\n" +
                                getString(R.string.height_cm) + ": ${model.height}\n" +
                                getString(R.string.weight_kg) + ": ${model.weight}\n" +
                                getString(R.string.co2_factor_g_km) + ": ${model.co2factor}\n" +
                                getString(R.string.capacity_cm) + ": ${model.capacity}\n" +
                                getString(R.string.fuel_capacity) + ": ${model.fuelcapacity}")
                            .setPositiveButton("OK", null)
                            .show()
                    }

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

        val legendLabels = listOf(getString(R.string.refills), getString(R.string.maintenance), getString(R.string.insurance), getString(R.string.tax))
        val amounts = listOf(refillAmount, maintenanceAmount, insuranceAmount, taxAmount)
        val colors = listOf(
            resources.getColor(R.color.primary),
            resources.getColor(R.color.teal_700),
            resources.getColor(R.color.accent),
            resources.getColor(R.color.gray_dark)
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
        dataSet.valueTextColor = resources.getColor(R.color.black)
        dataSet.valueTextSize = 16f
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD
        dataSet.selectionShift = 8f

        val data = PieData(dataSet)
        pieChart.data = data
        pieChart.description.isEnabled = false
        pieChart.centerText = if (noAvailableData) getString(R.string.no_available_data) else getString(R.string.expenses)
        pieChart.setCenterTextSize(14f)
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD)
        pieChart.setDrawEntryLabels(false)
        pieChart.setEntryLabelColor(resources.getColor(R.color.primary_dark))
        pieChart.setEntryLabelTextSize(14f)
        pieChart.setUsePercentValues(false)
        pieChart.isDrawHoleEnabled = true
        pieChart.holeRadius = if (noAvailableData) 100f else 40f
        pieChart.transparentCircleRadius = 45f
        pieChart.animateXY(1200, 1200)

        val legend = pieChart.legend
        legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER
        legend.horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
        legend.orientation = Legend.LegendOrientation.VERTICAL
        legend.setDrawInside(false)
        legend.isEnabled = true
        legend.textSize = 14f
        legend.typeface = Typeface.DEFAULT_BOLD
        legend.xEntrySpace = 16f
        legend.yEntrySpace = 1f
        legend.setCustom(legendEntries)

        pieChart.invalidate()
    }

    private fun setUpLineChart(refills: ArrayList<Refill>, model: CarModel) {

        val noAvailableData = refills.size < 3

        // Find the LineChart view from XML
        val lineChart: LineChart = findViewById(R.id.line_chart)

        val entries = mutableListOf<Entry>()
        val refillMap = HashMap<Float, Refill>()
        val co2Emissions = ArrayList<Float>()

        if (noAvailableData) {

            entries.add(Entry(0.0f, 0.0f))
        }

        else {

            var prevRefill = refills[0]
            for (i in 1 until refills.size) {

                val currentRefill = refills[i]
                refillMap[currentRefill.date.seconds * 1000f] = currentRefill

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
            private val secondFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            private val minuteFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            private val dayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            private val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            private val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())

            override fun getFormattedValue(value: Float): String {
                val visibleRange = lineChart.visibleXRange
                val date = Date(value.toLong())

                return when {

                    visibleRange < 3_600_000 -> secondFormat.format(date) // < 1 hour
                    visibleRange < 86_400_000 -> minuteFormat.format(date) // < 1 day
                    visibleRange < 2_592_000_000L -> dayFormat.format(date) // < 1 month
                    visibleRange < 2_592_000_000L * 12 -> monthFormat.format(date) // < 1 year
                    else -> yearFormat.format(date) // > 1 year
                }
            }
        }

        lineChart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartScale(me: MotionEvent?, scaleX: Float, scaleY: Float) {
                lineChart.invalidate() // Force redraw
            }

            override fun onChartTranslate(me: MotionEvent?, dX: Float, dY: Float) {
                lineChart.invalidate()
            }

            // Other methods can be left empty
            override fun onChartGestureStart(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
            override fun onChartGestureEnd(me: MotionEvent?, lastPerformedGesture: ChartTouchListener.ChartGesture?) {}
            override fun onChartLongPressed(me: MotionEvent?) {}
            override fun onChartDoubleTapped(me: MotionEvent?) {}
            override fun onChartSingleTapped(me: MotionEvent?) {}
            override fun onChartFling(me1: MotionEvent?, me2: MotionEvent?, velocityX: Float, velocityY: Float) {}
        }

        xAxis.setLabelCount(entries.size, true)
        lineChart.setScaleEnabled(true)
        xAxis.granularity = 1.0f
        xAxis.isGranularityEnabled = true

        // Create a LineDataSet (this holds the data and settings for the line)
        val dataSet = LineDataSet(entries, getString(R.string.co2_emissions))
        dataSet.color = resources.getColor(R.color.primary)
        dataSet.valueTextColor = resources.getColor(R.color.primary_dark)
        dataSet.valueTextSize = 14f
        dataSet.valueTypeface = Typeface.DEFAULT_BOLD
        dataSet.lineWidth = 3f
        dataSet.setDrawCircles(true)
        dataSet.circleRadius = 6f
        dataSet.setCircleColor(resources.getColor(R.color.accent))
        dataSet.setDrawCircleHole(true)
        dataSet.circleHoleColor = resources.getColor(R.color.white)
        dataSet.setDrawFilled(true)
        dataSet.fillDrawable = resources.getDrawable(R.drawable.linechart_gradient, null)
        dataSet.mode = LineDataSet.Mode.LINEAR
        dataSet.setDrawValues(true)
        dataSet.setDrawHighlightIndicators(true)
        dataSet.highLightColor = resources.getColor(R.color.accent)

        // Create LineData using the LineDataSet
        val lineData = LineData(dataSet)

        // Set the data to the LineChart
        lineChart.data = lineData

        // Customize chart appearance
        lineChart.axisLeft.gridColor = resources.getColor(R.color.gray_light)
        lineChart.axisLeft.gridLineWidth = 1f
        lineChart.axisRight.isEnabled = false
        xAxis.gridColor = resources.getColor(R.color.gray_light)
        xAxis.gridLineWidth = 1f
        lineChart.setDrawGridBackground(false)
        lineChart.setDrawBorders(false)
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        lineChart.animateXY(1200, 1200)

        val description = Description()
        description.text = if(noAvailableData) getString(R.string.not_enough_data) else getString(R.string.co2_emission_graph)
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

        if (!noAvailableData) {

            var lastTouchX = 0f
            var lastTouchY = 0f

            lineChart.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
                false
            }

            lineChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {

                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {

                        if (h != null) {

                            val transformer = lineChart.getTransformer(h.axis)
                            val point = floatArrayOf(e.x, e.y)
                            transformer.pointValuesToPixel(point)
                            val entryX = point[0]
                            val entryY = point[1]
                            val dx = lastTouchX - entryX
                            val dy = lastTouchY - entryY
                            val distance = sqrt(dx * dx + dy * dy)

                            if (distance < 50f)
                                println()
                            else
                                return
                        }

                        val sdf = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault())
                        val refill = refillMap[it.x]
                        val value = it.y
                        val xValue = sdf.format(it.x.toLong())
                        var position: String
                        if (refill != null) {

                            Map.getAddressBasedOnGeoPoint(refill.position.latitude, refill.position.longitude)
                                .addOnCompleteListener { result ->

                                    position = when (result) {

                                        is ReverseGeocodeTaskResult.Success -> {

                                            result.address.displayName ?: "Unknown"
                                        }

                                        is ReverseGeocodeTaskResult.Failure -> {

                                            result.exception.message!!
                                        }
                                    }

                                    // Show a popup dialog
                                    AlertDialog.Builder(this@CarGraphActivity)
                                        .setTitle(xValue)
                                        .setMessage(
                                            "CO2: $value " + getString(R.string.g_km_per_day) + "\n" +
                                            getString(R.string.mileage) + ": ${refill.mileage}\n" +
                                            getString(R.string.amount) + ": ${refill.amount}\n" +
                                            getString(R.string.ppl) + ": ${refill.ppl}\n" +
                                            getString(R.string.position) + ": $position")
                                        .setPositiveButton("OK", null)
                                        .show()
                                }
                        }

                        else {

                            ToastManager.show(this@CarGraphActivity, getString(R.string.error), Toast.LENGTH_SHORT)
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
        }

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
