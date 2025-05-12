package it.uninsubria.benztrack

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val model = CarModel()
        model.name = "Alfa Romeo Tonale"
        model.fuel = FuelType.Petrol
        model.year = 2023
        model.capacity = 1332
        model.co2factor = 130.0f
        model.weight = 1600.0f
        model.length = 4.528f
        model.height = 1.614f

        val button: Button = findViewById(R.id.button_greeting)
        button.setOnClickListener {

            db
                .createCarModel(model)
                .addOnSuccessListener {

                    ToastManager.show(this, "Creation was successful", Toast.LENGTH_SHORT)
                }
                .addOnFailureListener { e ->

                    e as CarModelException
                    if (e.name.isNotBlank())
                        Log.e("test", e.name)
                    if (e.year.isNotBlank())
                        Log.e("test", e.year)
                    if (e.capacity.isNotBlank())
                        Log.e("test", e.capacity)
                    if (e.co2factor.isNotBlank())
                        Log.e("test", e.co2factor)
                    if (e.weight.isNotBlank())
                        Log.e("test", e.weight)
                    if (e.length.isNotBlank())
                        Log.e("test", e.length)
                    if (e.height.isNotBlank())
                        Log.e("test", e.height)

                    ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                }
        }
    }

    private val db = Database()
}
