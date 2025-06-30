package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.core.graphics.toColorInt

class AddCarActivity : AppCompatActivity() {

    private lateinit var carNameEdit: TextInputEditText
    private lateinit var plateEdit: TextInputEditText
    private lateinit var modelEdit: TextInputEditText

    private lateinit var carNameLayout: TextInputLayout
    private lateinit var plateLayout: TextInputLayout
    private lateinit var modelLayout: TextInputLayout

    private lateinit var modelsRecyclerView: RecyclerView

    private lateinit var confirmButton: Button
    private lateinit var addModelButton: Button

    private var selectedModel: CarModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        carNameEdit = findViewById(R.id.edit_car_name)
        plateEdit = findViewById(R.id.edit_plate)
        modelEdit = findViewById(R.id.edit_model)

        carNameLayout = findViewById(R.id.car_name_layout)
        plateLayout = findViewById(R.id.plate_layout)
        modelLayout = findViewById(R.id.model_layout)

        modelsRecyclerView = findViewById(R.id.recycler_models)

        confirmButton = findViewById(R.id.button_confirm)
        addModelButton = findViewById(R.id.button_add_model)

        val modelInputLayout = findViewById<TextInputLayout>(R.id.model_layout)

        // Set up RecyclerView
        modelsRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = CarModelAdapter { model ->

            selectedModel = model
            modelEdit.setText(model.name)
            modelsRecyclerView.visibility = View.GONE
        }
        modelsRecyclerView.adapter = adapter

        // Set up confirm button
        confirmButton.setOnClickListener {

            clearErrors()

            val car = Car()
            car.name = carNameEdit.text.toString()
            car.plate = plateEdit.text.toString()

            if (selectedModel != null) {

                Handler.database.getCarModelDocumentReference(selectedModel!!)
                    .addOnSuccessListener { model ->

                        car.model = model
                    }
                    .addOnFailureListener { e ->

                        ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                    }

                Handler.database.addNewUserCar(Handler.loggedUser!!.username, car)
                    .addOnSuccessListener {

                        ToastManager.show(this, "Car added successfully", Toast.LENGTH_SHORT)
                        finish()
                    }

                    .addOnFailureListener { e ->

                        when (e) {

                            is CarException -> {

                                if (e.name.isNotEmpty()) {

                                    showError(carNameLayout, e.name)
                                }

                                if (e.plate.isNotEmpty()) {

                                    showError(plateLayout, e.plate)
                                }
                            }
                        }

                        ToastManager.show(this, "Error adding car: ${e.message}", Toast.LENGTH_SHORT)
                    }
            }

            else {

                ToastManager.show(this, "Error adding car: some fields are left blank", Toast.LENGTH_SHORT)
            }
        }

        addModelButton.setOnClickListener {

            val intent = Intent(this, AddCarModelActivity::class.java)
            startActivity(intent)
        }

        modelInputLayout.setEndIconOnClickListener {

            val query = modelEdit.text.toString()

            if (query.isBlank()) {

                modelsRecyclerView.visibility = View.GONE
                return@setEndIconOnClickListener
            }

            Handler.database.searchCarModelByName(query)
                .addOnSuccessListener { models ->

                    if (models.isNotEmpty()) {

                        adapter.submitList(models)
                        modelsRecyclerView.visibility = View.VISIBLE
                    }

                    else {

                        modelsRecyclerView.visibility = View.GONE
                    }
                }

                .addOnFailureListener { e ->

                    ToastManager.show(this@AddCarActivity, "Error searching models: ${e.message}", Toast.LENGTH_SHORT)
                    modelsRecyclerView.visibility = View.GONE
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {

        onBackPressed()
        return true
    }

    private fun clearErrors() {

        carNameLayout.isErrorEnabled = false
        plateLayout.isErrorEnabled = false
    }

    private fun showError(layout: TextInputLayout, message: String) {

        layout.error = message
        layout.isErrorEnabled = true
    }
}

class CarModelAdapter(private val onModelSelected: (CarModel) -> Unit) :
    RecyclerView.Adapter<CarModelAdapter.ViewHolder>() {

    private var models: ArrayList<CarModel> = arrayListOf()

    fun submitList(newModels: ArrayList<CarModel>) {

        models = newModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {

        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_model, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val model = models[position]
        holder.bind(model)
    }

    override fun getItemCount() = models.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val modelNameText: TextView = itemView.findViewById(R.id.text_model_name)
        private val modelDimensionsText: TextView = itemView.findViewById(R.id.text_model_dimensions)
        private val modelCo2CapacityText: TextView = itemView.findViewById(R.id.text_model_co2_capacity)

        fun bind(model: CarModel) {

            // Use a slightly lighter dark gray color
            val gray = "#666666".toColorInt()

            // First line
            val fuelString = when (model.fuel) {

                FuelType.Petrol -> "Petrol"
                FuelType.Diesel -> "Diesel"
                FuelType.LPG -> "LPG"
            }

            "${model.name} (${model.year}, $fuelString)".also { modelNameText.text = it }

            // Second line
            val width = if (model.width.isNaN()) "?" else model.width.toInt().toString()
            val length = if (model.length.isNaN()) "?" else model.length.toInt().toString()
            val height = if (model.height.isNaN()) "?" else model.height.toInt().toString()
            val weight = if (model.weight.isNaN()) "?" else model.weight.toInt().toString()

            modelDimensionsText.text = buildColoredLine(
                listOf(
                    "W ", width, " cm | ",
                    "L ", length, " cm | ",
                    "H ", height, " cm | ",
                    "M ", weight, " kg"
                ),
                gray
            )

            // Third line
            val co2 = if (model.co2factor.isNaN()) "?" else model.co2factor.toInt().toString()
            val capacity = if (model.capacity == Int.MAX_VALUE) "?" else model.capacity.toString()

            modelCo2CapacityText.text = buildColoredLine(
                listOf(
                    "CO2 ", co2, " g/km | ",
                    "Capacity ", capacity, " cm3"
                ),
                gray
            )

            itemView.setOnClickListener {
                onModelSelected(model)
            }
        }

        // Helper function to color only the values
        private fun buildColoredLine(parts: List<String>, valueColor: Int): CharSequence {

            val builder = SpannableStringBuilder()
            val grayUnits = listOf("cm", "kg", "g/km", "cm3")

            for (part in parts) {

                val trimmed = part.trim().removePrefix("|").removeSuffix("|")
                val isValueOrUnit = part.trim().all { it.isDigit() || it == '?' } ||
                    grayUnits.any { trimmed.startsWith(it) || trimmed.endsWith(it) || trimmed == it }
                val start = builder.length
                builder.append(part)

                if (isValueOrUnit) {

                    builder.setSpan(
                        ForegroundColorSpan(valueColor),
                        start,
                        builder.length,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            return builder
        }
    }
} 