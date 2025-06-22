package it.uninsubria.benztrack

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class AddCarModelActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car_model)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val nameEdit = findViewById<TextInputEditText>(R.id.edit_model_name)
        val yearEdit = findViewById<TextInputEditText>(R.id.edit_model_year)
        val capacityEdit = findViewById<TextInputEditText>(R.id.edit_model_capacity)
        val fuelSpinner = findViewById<Spinner>(R.id.spinner_fuel)
        val co2Edit = findViewById<TextInputEditText>(R.id.edit_model_co2)
        val weightEdit = findViewById<TextInputEditText>(R.id.edit_model_weight)
        val lengthEdit = findViewById<TextInputEditText>(R.id.edit_model_length)
        val heightEdit = findViewById<TextInputEditText>(R.id.edit_model_height)
        val widthEdit = findViewById<TextInputEditText>(R.id.edit_model_width)
        val submitButton = findViewById<Button>(R.id.button_submit_model)
        val fuelTypes = FuelType.entries.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, fuelTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        fuelSpinner.adapter = adapter

        submitButton.setOnClickListener {
            val name = nameEdit.text.toString()
            val year = yearEdit.text.toString().toIntOrNull() ?: 0
            val capacity = capacityEdit.text.toString().toIntOrNull() ?: 0
            val fuel = FuelType.valueOf(fuelSpinner.selectedItem.toString())
            val co2 = co2Edit.text.toString().toFloatOrNull() ?: 0f
            val weight = weightEdit.text.toString().toFloatOrNull() ?: 0f
            val length = lengthEdit.text.toString().toFloatOrNull() ?: 0f
            val height = heightEdit.text.toString().toFloatOrNull() ?: 0f
            val width = widthEdit.text.toString().toFloatOrNull() ?: 0f

            val model = CarModel()
            model.name = name
            model.year = year
            model.capacity = capacity
            model.fuel = fuel
            model.co2factor = co2
            model.weight = weight
            model.length = length
            model.height = height
            model.width = width

            Database().createCarModel(model)
                .addOnSuccessListener {
                    ToastManager.show(this, "Model added!", android.widget.Toast.LENGTH_SHORT)
                    finish()
                }
                .addOnFailureListener { e ->
                    ToastManager.show(this, "Error: ${e.message}", android.widget.Toast.LENGTH_SHORT)
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 