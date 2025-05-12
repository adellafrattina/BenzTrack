package it.uninsubria.benztrack

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.DocumentReference

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val car = Car()
        car.name = "Macchina di mia mamma"
        car.plate = "EG575AB"
        car.maintenancedate = null
        car.insurancedate = null
        car.taxdate = null

        val button: Button = findViewById(R.id.button_greeting)
        button.setOnClickListener {

            db
                .searchCarModelByName("opel")
                .addOnSuccessListener { models ->

                    if (models.isNotEmpty()) {

                        db.getCarModelDocumentReference(models[0])
                            .addOnSuccessListener { document ->

                                car.model = document

                                db.deleteCar("admin", "123456")
                                    .addOnSuccessListener {

                                        ToastManager.show(this, "Car deleted successfully", Toast.LENGTH_SHORT)
                                    }
                                    .addOnFailureListener { e ->

                                        ToastManager.show(this, e.message, Toast.LENGTH_SHORT)

                                        e as CarException
                                        if (e.plate.isNotEmpty())
                                            Log.e("test", e.plate)
                                        if (e.name.isNotEmpty())
                                            Log.e("test", e.name)
                                    }
                            }
                            .addOnFailureListener { e ->

                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }
                    }
                }
                .addOnFailureListener { e ->

                    ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                }
        }
    }

    private val db = Database()
}
