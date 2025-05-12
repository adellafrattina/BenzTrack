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

        val button: Button = findViewById(R.id.button_greeting)
        button.setOnClickListener {

            db
                .searchCarModelByName("opel")
                .addOnSuccessListener { models ->

                    if (models.isNotEmpty())
                        ToastManager.show(this, models[0].name, Toast.LENGTH_SHORT)
                }
                .addOnFailureListener { e ->

                    ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                }
        }
    }

    private val db = Database()
}
