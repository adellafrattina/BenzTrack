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
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

class CarInfoActivity : AppCompatActivity() {

    private lateinit var userCar: Car

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_info)
        Handler.database.setContext(this)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val carPlate = intent.getStringExtra("car_plate")

        val addRefillButton = findViewById<Button>(R.id.button_add_refill)

        val setDateButton = findViewById<Button>(R.id.button_set_maintenance_date)
        val payButton = findViewById<Button>(R.id.button_pay_maintenance)
        val maintenancePlaceholder = findViewById<TextView>(R.id.text_maintenance_placeholder)

        val setInsuranceDateButton = findViewById<Button>(R.id.button_set_insurance_date)
        val payInsuranceButton = findViewById<Button>(R.id.button_pay_insurance)
        val insurancePlaceholder = findViewById<TextView>(R.id.text_insurance_placeholder)

        val setTaxDateButton = findViewById<Button>(R.id.button_set_tax_date)
        val payTaxButton = findViewById<Button>(R.id.button_pay_tax)
        val taxPlaceholder = findViewById<TextView>(R.id.text_tax_placeholder)

        val refillPlaceholder = findViewById<TextView>(R.id.text_refills_placeholder)

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
                    maintenancePlaceholder.text = getString(R.string.next_payment_on, sdf.format(car.maintenancedate!!.toDate()))
                }

                if (car.insurancedate == null) {

                    setInsuranceDateButton.isEnabled = true
                    setInsuranceDateButton.alpha = 1.0f
                    payInsuranceButton.isEnabled = false
                    payInsuranceButton.alpha = 0.5f
                }

                else {

                    setInsuranceDateButton.isEnabled = true
                    setInsuranceDateButton.alpha = 1.0f
                    payInsuranceButton.isEnabled = true
                    payInsuranceButton.alpha = 1.0f
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    insurancePlaceholder.text = getString(R.string.next_payment_on, sdf.format(car.insurancedate!!.toDate()))
                }

                if (car.taxdate == null) {

                    setTaxDateButton.isEnabled = true
                    setTaxDateButton.alpha = 1.0f
                    payTaxButton.isEnabled = false
                    payTaxButton.alpha = 0.5f
                }

                else {

                    setTaxDateButton.isEnabled = true
                    setTaxDateButton.alpha = 1.0f
                    payTaxButton.isEnabled = true
                    payTaxButton.alpha = 1.0f
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    taxPlaceholder.text = getString(R.string.next_payment_on, sdf.format(car.taxdate!!.toDate()))
                }

                Handler.database.getRefillData(Handler.loggedUser!!.username, carPlate)
                    .addOnSuccessListener { refills ->

                        if (refills.size >= 2) {

                            var totalLiters = 0f
                            var totalKm = 0f
                            var prev = refills[0]

                            for (i in 1 until refills.size) {

                                val curr = refills[i]
                                val consumedLiters = prev.currentfuelamount + prev.amount / prev.ppl - curr.currentfuelamount
                                val travelledKm = if (curr.mileage - prev.mileage > 0) curr.mileage - prev.mileage else 0f
                                totalLiters += consumedLiters
                                totalKm += travelledKm
                                prev = curr
                            }

                            val mean = if (totalKm > 0) totalLiters / totalKm * 100 else 0f

                            refillPlaceholder.text = getString(R.string.average_consumption, DecimalFormat("#.##").format(mean))

                        }

                        else
                            refillPlaceholder.setText(R.string.refill_placeholder)
                    }

                    .addOnFailureListener {

                        refillPlaceholder.setText(R.string.refill_placeholder)
                    }
            }

            .addOnFailureListener { e ->

                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
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

                        maintenancePlaceholder.text = getString(R.string.next_payment_on, formatted)
                        ToastManager.show(this, getString(R.string.date_set_to, getString(R.string.maintenance), formatted), Toast.LENGTH_SHORT)
                        payButton.isEnabled = true
                        payButton.alpha = 1.0f
                    }

                    .addOnFailureListener { e ->

                        ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                    }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.datePicker.minDate = System.currentTimeMillis()
            dialog.show()
        }

        payButton.setOnClickListener {

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = getString(R.string.enter_fee, getString(R.string.maintenance).lowercase()) //"Enter maintenance fee (€)"

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.fee, getString(R.string.maintenance)))
                .setView(input)
                .setPositiveButton(getString(R.string.submit)) { _, _ ->

                    val feeText = input.text.toString()
                    val maintenance = Maintenance()
                    maintenance.date = Timestamp.now()
                    maintenance.amount = feeText.toFloatOrNull()!!

                    if (maintenance.amount > 0) {

                        Handler.database.payMaintenance(Handler.loggedUser!!.username, carPlate, maintenance)
                            .addOnSuccessListener {

                                ToastManager.show(this,  getString(R.string.fee_of_submitted, getString(R.string.maintenance), maintenance.amount.toString()), Toast.LENGTH_SHORT)
                                payButton.isEnabled = false
                                payButton.alpha = 0.5f
                                maintenancePlaceholder.text = getString(R.string.maintenance_placeholder)
                            }

                            .addOnFailureListener { e ->

                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }
                    }

                    else {

                        ToastManager.show(this, getString(R.string.please_enter_valid_amount), Toast.LENGTH_SHORT)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        setInsuranceDateButton.setOnClickListener {

            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->

                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)

                Handler.database.setNewInsuranceDate(Handler.loggedUser!!.username, carPlate, Timestamp(cal.time))
                    .addOnSuccessListener {

                        insurancePlaceholder.text = getString(R.string.next_payment_on, formatted)
                        ToastManager.show(this, getString(R.string.enter_fee, getString(R.string.insurance).lowercase()), Toast.LENGTH_SHORT)
                        payInsuranceButton.isEnabled = true
                        payInsuranceButton.alpha = 1.0f
                    }

                    .addOnFailureListener { e ->

                        ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                    }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.datePicker.minDate = System.currentTimeMillis()
            dialog.show()
        }

        payInsuranceButton.setOnClickListener {

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = getString(R.string.enter_fee, getString(R.string.insurance).lowercase())

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.fee, getString(R.string.insurance)))
                .setView(input)
                .setPositiveButton(R.string.submit) { _, _ ->

                    val feeText = input.text.toString()
                    val insurance = Insurance()
                    insurance.amount = feeText.toFloatOrNull()!!
                    insurance.date = Timestamp.now()

                    if (insurance.amount > 0) {

                        Handler.database.payInsurance(Handler.loggedUser!!.username, carPlate, insurance)
                            .addOnSuccessListener {

                                ToastManager.show(this, getString(R.string.fee_of_submitted, getString(R.string.insurance), insurance.amount.toString()), Toast.LENGTH_SHORT)
                                payInsuranceButton.isEnabled = false
                                payInsuranceButton.alpha = 0.5f
                                insurancePlaceholder.text = getString(R.string.insurance_placeholder)
                            }

                            .addOnFailureListener { e ->

                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }

                    }

                    else {

                        ToastManager.show(this, getString(R.string.please_enter_valid_amount), Toast.LENGTH_SHORT)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        setTaxDateButton.setOnClickListener {

            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->

                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)

                Handler.database.setNewTaxDate(Handler.loggedUser!!.username, carPlate, Timestamp(cal.time))
                    .addOnSuccessListener {

                        taxPlaceholder.text = getString(R.string.next_payment_on, formatted)
                        ToastManager.show(this, getString(R.string.date_set_to, getString(R.string.tax), formatted), Toast.LENGTH_SHORT)
                        payTaxButton.isEnabled = true
                        payTaxButton.alpha = 1.0f
                    }

                    .addOnFailureListener { e ->

                        ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                    }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))

            dialog.datePicker.minDate = System.currentTimeMillis()
            dialog.show()
        }

        payTaxButton.setOnClickListener {

            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            input.hint = getString(R.string.enter_fee, getString(R.string.tax).lowercase())

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.fee, getString(R.string.tax)))
                .setView(input)
                .setPositiveButton(getString(R.string.submit)) { _, _ ->

                    val feeText = input.text.toString()
                    val tax = Tax()
                    tax.amount = feeText.toFloatOrNull()!!
                    tax.date = Timestamp.now()

                    if (tax.amount > 0) {

                        Handler.database.payTax(Handler.loggedUser!!.username, carPlate, tax)
                            .addOnSuccessListener {

                                ToastManager.show(this, getString(R.string.fee_of_submitted, getString(R.string.tax), tax.amount.toString()), Toast.LENGTH_SHORT)
                                payTaxButton.isEnabled = false
                                payTaxButton.alpha = 0.5f
                                taxPlaceholder.text = getString(R.string.tax_placeholder)
                            }

                            .addOnFailureListener { e ->

                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }
                    }

                    else {

                        ToastManager.show(this, getString(R.string.please_enter_valid_amount), Toast.LENGTH_SHORT)
                    }
                }

                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }
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
