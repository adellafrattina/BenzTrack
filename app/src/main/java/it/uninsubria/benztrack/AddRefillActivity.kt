package it.uninsubria.benztrack

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import java.util.Calendar

class AddRefillActivity : AppCompatActivity() {

    private lateinit var dateEdit: TextInputEditText
    private lateinit var positionEdit: TextInputEditText
    private lateinit var pplEdit: TextInputEditText
    private lateinit var mileageEdit: TextInputEditText
    private lateinit var amountEdit: TextInputEditText
    private lateinit var fuelEdit: TextInputEditText

    private lateinit var pplLayout: TextInputLayout
    private lateinit var dateLayout: TextInputLayout
    private lateinit var positionLayout: TextInputLayout
    private lateinit var mileageLayout: TextInputLayout
    private lateinit var amountLayout: TextInputLayout
    private lateinit var fuelLayout: TextInputLayout

    private lateinit var submitButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_refill)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val carPlate = intent.getStringExtra("car_plate")

        dateEdit = findViewById(R.id.edit_refill_date)
        positionEdit = findViewById(R.id.edit_refill_position)
        pplEdit = findViewById(R.id.edit_refill_ppl)
        mileageEdit = findViewById(R.id.edit_refill_mileage)
        amountEdit = findViewById(R.id.edit_refill_amount)
        fuelEdit = findViewById(R.id.edit_refill_fuel)

        pplLayout = findViewById(R.id.ppl_layout)
        dateLayout = findViewById(R.id.date_layout)
        positionLayout = findViewById(R.id.position_layout)
        mileageLayout = findViewById(R.id.mileage_layout)
        amountLayout = findViewById(R.id.amount_layout)
        fuelLayout = findViewById(R.id.fuel_layout)

        submitButton = findViewById(R.id.button_submit_refill)

        var refillDate: Timestamp = Timestamp.now()
        dateEdit.setText(android.text.format.DateFormat.format("dd-MM-yyyy", refillDate.toDate()))
        dateEdit.setOnClickListener {

            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->

                cal.set(year, month, dayOfMonth)
                refillDate = Timestamp(cal.time)
                dateEdit.setText(android.text.format.DateFormat.format("dd-MM-yyyy", cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.show()
        }

        submitButton.setOnClickListener {

            clearErrors()

            val refill = Refill()
            refill.ppl = pplEdit.text.toString().toFloatOrNull() ?: Float.NaN
            refill.date = refillDate
            refill.amount = amountEdit.text.toString().toFloatOrNull() ?: Float.NaN
            refill.mileage = mileageEdit.text.toString().toFloatOrNull() ?: Float.NaN
            refill.position = positionEdit.text.toString()
            refill.currentfuelamount = fuelEdit.text.toString().toFloatOrNull() ?: Float.NaN

            Handler.database.setNewRefill(Handler.loggedUser!!.username, carPlate!!, refill)
                .addOnSuccessListener {

                    ToastManager.show(this, "Refill added!", Toast.LENGTH_SHORT)
                    finish()
                }

                .addOnFailureListener { e ->

                    when (e) {

                        is RefillException -> {

                            if (e.position.isNotEmpty()) {

                                showError(positionLayout, e.position)
                            }

                            if (e.ppl.isNotEmpty()) {

                                showError(pplLayout, e.ppl)
                            }

                            if (e.amount.isNotEmpty()) {

                                showError(amountLayout, e.amount)
                            }

                            if (e.mileage.isNotEmpty()) {

                                showError(mileageLayout, e.mileage)
                            }

                            if (e.fuelcapacity.isNotEmpty()) {

                                showError(fuelLayout, e.fuelcapacity)
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

        pplLayout.isErrorEnabled = false
        positionLayout.isErrorEnabled = false
        mileageLayout.isErrorEnabled = false
        amountLayout.isErrorEnabled = false
        fuelLayout.isErrorEnabled = false
    }

    override fun onSupportNavigateUp(): Boolean {

        finish()
        return true
    }
} 