package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.app.DatePickerDialog
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.Timestamp
import java.util.Calendar
import android.app.AlertDialog
import android.widget.EditText
import android.text.InputType
import java.text.SimpleDateFormat
import java.util.Locale

class CarInfoActivity : AppCompatActivity() {

    private lateinit var userCar: Car

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_info)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addRefillButton = findViewById<Button>(R.id.button_add_refill)
        val carPlate = intent.getStringExtra("car_plate")
        val setDateButton = findViewById<Button>(R.id.button_set_maintenance_date)
        val payButton = findViewById<Button>(R.id.button_pay_maintenance)
        val maintenancePlaceholder = findViewById<TextView>(R.id.text_maintenance_placeholder)
        Handler.database.getUserCar(Handler.loggedUser!!.username, carPlate!!)
            .addOnSuccessListener { car ->
                userCar = car
                if (car.maintenancedate == null) {

                    setDateButton.isEnabled = true
                    setDateButton.alpha = 1.0f

                    payButton.isEnabled = false
                    payButton.alpha = 0.5f
                }
                else {

                    setDateButton.isEnabled = true
                    setDateButton.alpha = 1.0f

                    payButton.isEnabled = true
                    payButton.alpha = 1.0f

                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    maintenancePlaceholder.text = "Next maintenance on: " + sdf.format(car.maintenancedate!!.toDate())
                }
            }

        addRefillButton.setOnClickListener {
            val intent = Intent(this, AddRefillActivity::class.java)
            intent.putExtra("car_plate", carPlate)
            startActivity(intent)
        }


        setDateButton.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)

                Handler.database.setNewMaintenanceDate(Handler.loggedUser!!.username, carPlate, Timestamp(cal.time))
                    .addOnSuccessListener {
                        maintenancePlaceholder.text = "Next maintenance on: " + formatted
                        ToastManager.show(this, "Maintenance date set to " + formatted, Toast.LENGTH_SHORT)
                        payButton.isEnabled = true
                        payButton.alpha = 1.0f
                    }
                    .addOnFailureListener {
                        ToastManager.show(this, "Error while setting maintenance date", Toast.LENGTH_SHORT)
                    }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dialog.datePicker.minDate = System.currentTimeMillis()
            dialog.show()
        }

        payButton.setOnClickListener {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = "Enter maintenance fee (€)"

            AlertDialog.Builder(this)
                .setTitle("Maintenance Fee")
                .setView(input)
                .setPositiveButton("Submit") { _, _ ->
                    val feeText = input.text.toString()
                    val maintenance = Maintenance()
                    maintenance.date = Timestamp.now()
                    maintenance.amount = feeText.toFloatOrNull()!!
                    if (maintenance.amount > 0) {
                        Handler.database.payMaintenance(Handler.loggedUser!!.username, carPlate, maintenance)
                            .addOnSuccessListener {
                                ToastManager.show(this, "Maintenance fee of €${maintenance.amount} submitted!", Toast.LENGTH_SHORT)
                                payButton.isEnabled = false
                                payButton.alpha = 0.5f
                                maintenancePlaceholder.text = getString(R.string.maintenance_placeholder)
                            }
                            .addOnFailureListener {
                                ToastManager.show(this, "Maintenance fee failed to submit", Toast.LENGTH_SHORT)
                            }
                    } else {
                        ToastManager.show(this, "Please enter a valid amount", Toast.LENGTH_SHORT)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
