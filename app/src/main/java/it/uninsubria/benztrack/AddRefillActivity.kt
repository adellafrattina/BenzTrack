package it.uninsubria.benztrack

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
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

    private var selectedPoint: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_refill)
        Handler.database.setContext(this)

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

        positionEdit.isFocusable = false
        positionEdit.isClickable = true
        positionEdit.setOnClickListener {

            val intent = Intent(this, PickLocationActivity::class.java)
            startActivityForResult(intent, 1001)
        }

        submitButton.setOnClickListener {

            clearErrors()

            val refill = Refill()
            refill.ppl = pplEdit.text.toString().toFloatOrNull() ?: Float.NaN
            refill.date = refillDate
            refill.amount = amountEdit.text.toString().toFloatOrNull() ?: Float.NaN
            refill.mileage = mileageEdit.text.toString().toFloatOrNull() ?: Float.NaN
            if (selectedPoint != null)
                refill.position = selectedPoint!!
            refill.currentfuelamount = fuelEdit.text.toString().toFloatOrNull() ?: Float.NaN

            Handler.database.setNewRefill(Handler.loggedUser!!.username, carPlate!!, refill)
                .addOnSuccessListener {

                    ToastManager.show(this, getString(R.string.refill_added), Toast.LENGTH_SHORT)
                    finish()
                }

                .addOnFailureListener { e ->

                    if (e.message != null)
                        ToastManager.show(this, e.message, Toast.LENGTH_LONG)

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

                            if (e.currentfuelamount.isNotEmpty()) {

                                showError(fuelLayout, e.currentfuelamount)
                            }
                        }
                    }
                }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            val lat = data.getDoubleExtra("latitude", 0.0)
            val lon = data.getDoubleExtra("longitude", 0.0)
            positionEdit.setText(getString(R.string.loading))
            selectedPoint = GeoPoint(lat, lon)
            Map.getAddressBasedOnGeoPoint(lat, lon)
                .addOnSuccessListener { address ->

                    positionEdit.setText(address?.displayName ?: getString(R.string.unknown))
                }
                .addOnFailureListener { e ->

                    positionEdit.setText(e.message)
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

    override fun onResume() {
        super.onResume()
        Handler.database.setContext(this)
    }
}
