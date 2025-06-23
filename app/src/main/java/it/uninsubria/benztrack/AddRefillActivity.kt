package it.uninsubria.benztrack

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import java.util.Calendar

class AddRefillActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_refill)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val carPlate = intent.getStringExtra("car_plate")
        val dateEdit = findViewById<TextInputEditText>(R.id.edit_refill_date)
        val positionEdit = findViewById<TextInputEditText>(R.id.edit_refill_position)
        val pplEdit = findViewById<TextInputEditText>(R.id.edit_refill_ppl)
        val mileageEdit = findViewById<TextInputEditText>(R.id.edit_refill_mileage)
        val amountEdit = findViewById<TextInputEditText>(R.id.edit_refill_amount)
        val submitButton = findViewById<Button>(R.id.button_submit_refill)

        var refillDate: Timestamp = Timestamp.now()
        dateEdit.setText(android.text.format.DateFormat.format("yyyy-MM-dd", refillDate.toDate()))
        dateEdit.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                refillDate = Timestamp(cal.time)
                dateEdit.setText(android.text.format.DateFormat.format("yyyy-MM-dd", cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
            dialog.show()
        }

        submitButton.setOnClickListener {
            val position = positionEdit.text.toString()
            val ppl = pplEdit.text.toString().toFloatOrNull() ?: Float.NaN
            val mileage = mileageEdit.text.toString().toFloatOrNull() ?: Float.NaN
            val amount = amountEdit.text.toString().toFloatOrNull() ?: Float.NaN

            if (carPlate.isNullOrBlank() || position.isBlank() || ppl.isNaN() || mileage.isNaN() || amount.isNaN()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val refill = Refill()
            refill.ppl = ppl
            refill.date = refillDate
            refill.amount = amount
            refill.mileage = mileage
            refill.position = position

            Database().setNewRefill(Handler.loggedUser!!.username, carPlate, refill)
                .addOnSuccessListener {
                    Toast.makeText(this, "Refill added!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
} 