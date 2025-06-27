package it.uninsubria.benztrack

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class AddCarModelActivity : AppCompatActivity() {

    private lateinit var nameEdit: TextInputEditText
    private lateinit var yearEdit: TextInputEditText
    private lateinit var capacityEdit: TextInputEditText
    private lateinit var fuelSpinner: Spinner
    private lateinit var co2Edit: TextInputEditText
    private lateinit var weightEdit: TextInputEditText
    private lateinit var lengthEdit: TextInputEditText
    private lateinit var heightEdit: TextInputEditText
    private lateinit var widthEdit: TextInputEditText

    private lateinit var nameLayout: TextInputLayout
    private lateinit var yearLayout: TextInputLayout
    private lateinit var capacityLayout: TextInputLayout
    private lateinit var co2Layout: TextInputLayout
    private lateinit var weightLayout: TextInputLayout
    private lateinit var lengthLayout: TextInputLayout
    private lateinit var heightLayout: TextInputLayout
    private lateinit var widthLayout: TextInputLayout

    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car_model)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        nameEdit = findViewById(R.id.edit_model_name)
        yearEdit = findViewById(R.id.edit_model_year)
        capacityEdit = findViewById(R.id.edit_model_capacity)
        fuelSpinner = findViewById(R.id.spinner_fuel)
        co2Edit = findViewById(R.id.edit_model_co2)
        weightEdit = findViewById(R.id.edit_model_weight)
        lengthEdit = findViewById(R.id.edit_model_length)
        heightEdit = findViewById(R.id.edit_model_height)
        widthEdit = findViewById(R.id.edit_model_width)

        nameLayout = findViewById(R.id.name_layout)
        yearLayout = findViewById(R.id.year_layout)
        capacityLayout = findViewById(R.id.capacity_layout)
        co2Layout = findViewById(R.id.co2_layout)
        weightLayout = findViewById(R.id.weight_layout)
        lengthLayout = findViewById(R.id.length_layout)
        heightLayout = findViewById(R.id.height_layout)
        widthLayout = findViewById(R.id.width_layout)

        submitButton = findViewById(R.id.button_submit_model)

        val fuelTypes = FuelType.entries.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fuelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fuelSpinner.adapter = adapter

        submitButton.setOnClickListener {

            clearErrors()

            val model = CarModel()
            model.name = nameEdit.text.toString()
            model.year = yearEdit.text.toString().toIntOrNull() ?: 0
            model.capacity = capacityEdit.text.toString().toIntOrNull() ?: 0
            model.fuel = FuelType.valueOf(fuelSpinner.selectedItem.toString())
            model.co2factor = co2Edit.text.toString().toFloatOrNull() ?: Float.NaN
            model.weight = weightEdit.text.toString().toFloatOrNull() ?: 0f
            model.length = lengthEdit.text.toString().toFloatOrNull() ?: 0f
            model.height = heightEdit.text.toString().toFloatOrNull() ?: 0f
            model.width = widthEdit.text.toString().toFloatOrNull() ?: 0f

            Handler.database.createCarModel(model)
                .addOnSuccessListener {

                    ToastManager.show(this, "Model added!", android.widget.Toast.LENGTH_SHORT)
                    finish()
                }

                .addOnFailureListener { e ->

                    when (e) {

                        is CarModelException -> {

                            if (e.name.isNotEmpty()) {

                                showError(nameLayout, e.name)
                            }

                            if (e.year.isNotEmpty()) {

                                showError(yearLayout, e.year)
                            }

                            if (e.capacity.isNotEmpty()) {

                                showError(capacityLayout, e.capacity)
                            }

                            if (e.co2factor.isNotEmpty()) {

                                showError(co2Layout, e.co2factor)
                            }

                            if (e.weight.isNotEmpty()) {

                                showError(weightLayout, e.weight)
                            }

                            if (e.length.isNotEmpty()) {

                                showError(lengthLayout, e.length)
                            }

                            if (e.height.isNotEmpty()) {

                                showError(heightLayout, e.height)
                            }

                            if (e.width.isNotEmpty()) {

                                showError(widthLayout, e.width)
                            }
                        }
                    }
                }
        }
    }

    private fun showError(layout: TextInputLayout, message: String) {

        layout.error = message
        layout.isErrorEnabled = true
    }

    private fun clearErrors() {

        nameLayout.isErrorEnabled = false
        yearLayout.isErrorEnabled = false
        capacityLayout.isErrorEnabled = false
        co2Layout.isErrorEnabled = false
        weightLayout.isErrorEnabled = false
        lengthLayout.isErrorEnabled = false
        heightLayout.isErrorEnabled = false
        widthLayout.isErrorEnabled = false
    }

    override fun onSupportNavigateUp(): Boolean {

        finish()
        return true
    }
} 