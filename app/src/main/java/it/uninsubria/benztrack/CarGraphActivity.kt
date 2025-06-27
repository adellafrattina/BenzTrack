package it.uninsubria.benztrack

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.gms.tasks.Tasks

class CarGraphActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

                    text.text = "${model.name} (${model.year}, $fuelString)\nW, ${model.width} cm | L ${model.length} | H ${model.height} | M ${model.weight} kg\nCO2 ${model.co2factor} g/km | Capacity ${model.capacity} cm3"

                    setUpPieChart(refillData, maintenanceData, insuranceData, taxData)
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

    private fun showLoading(show: Boolean) {

        loadingOverlay.visibility = if (show) View.VISIBLE else View.GONE
    }

    private lateinit var loadingOverlay: View
}
