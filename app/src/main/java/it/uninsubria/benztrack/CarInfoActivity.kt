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

class CarInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_info)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addRefillButton = findViewById<Button>(R.id.button_add_refill)
        val carPlate = intent.getStringExtra("car_plate")
        addRefillButton.setOnClickListener {
            val intent = Intent(this, AddRefillActivity::class.java)
            intent.putExtra("car_plate", carPlate)
            startActivity(intent)
        }

        val setDateButton = findViewById<Button>(R.id.button_set_maintenance_date)
        val payButton = findViewById<Button>(R.id.button_pay_maintenance)
        val maintenancePlaceholder = findViewById<TextView>(R.id.text_maintenance_placeholder)
        payButton.isEnabled = false
        payButton.alpha = 0.5f
        setDateButton.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)

                Handler.database.setNewMaintenanceDate(Handler.loggedUser!!.username, carPlate!!, Timestamp(cal.time))
                    .addOnSuccessListener {

                        maintenancePlaceholder.text = "Next maintenance on: " + formatted
                        ToastManager.show(this, "Maintenance date set to " + formatted, Toast.LENGTH_SHORT)
                    }
                    .addOnFailureListener {


                    }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dialog.show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
