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
        val setInsuranceDateButton = findViewById<Button>(R.id.button_set_insurance_date)
        val payInsuranceButton = findViewById<Button>(R.id.button_pay_insurance)
        val insurancePlaceholder = findViewById<TextView>(R.id.text_insurance_placeholder)
        val setTaxDateButton = findViewById<Button>(R.id.button_set_tax_date)
        val payTaxButton = findViewById<Button>(R.id.button_pay_tax)
        val taxPlaceholder = findViewById<TextView>(R.id.text_tax_placeholder)
        Handler.database.getUserCar(Handler.loggedUser!!.username, carPlate!!)
            .addOnSuccessListener { car ->
                userCar = car
                if (car.maintenancedate == null) {
                    setDateButton.isEnabled = true
                    setDateButton.alpha = 1.0f
                    payButton.isEnabled = false
                    payButton.alpha = 0.5f
                } else {
                    setDateButton.isEnabled = true
                    setDateButton.alpha = 1.0f
                    payButton.isEnabled = true
                    payButton.alpha = 1.0f
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    maintenancePlaceholder.text = "Next maintenance on: " + sdf.format(car.maintenancedate!!.toDate())
                }
                if (car.insurancedate == null) {
                    setInsuranceDateButton.isEnabled = true
                    setInsuranceDateButton.alpha = 1.0f
                    payInsuranceButton.isEnabled = false
                    payInsuranceButton.alpha = 0.5f
                } else {
                    setInsuranceDateButton.isEnabled = true
                    setInsuranceDateButton.alpha = 1.0f
                    payInsuranceButton.isEnabled = true
                    payInsuranceButton.alpha = 1.0f
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    insurancePlaceholder.text = "Next insurance on: " + sdf.format(car.insurancedate!!.toDate())
                }
                if (car.taxdate == null) {
                    setTaxDateButton.isEnabled = true
                    setTaxDateButton.alpha = 1.0f
                    payTaxButton.isEnabled = false
                    payTaxButton.alpha = 0.5f
                } else {
                    setTaxDateButton.isEnabled = true
                    setTaxDateButton.alpha = 1.0f
                    payTaxButton.isEnabled = true
                    payTaxButton.alpha = 1.0f
                    val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
                    taxPlaceholder.text = "Next tax on: " + sdf.format(car.taxdate!!.toDate())
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
                        maintenancePlaceholder.text = "Next maintenance on: $formatted"
                        ToastManager.show(this, "Maintenance date set to $formatted", Toast.LENGTH_SHORT)
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
                            .addOnFailureListener { e ->
                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }
                    } else {
                        ToastManager.show(this, "Please enter a valid amount", Toast.LENGTH_SHORT)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setInsuranceDateButton.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)
                Handler.database.setNewInsuranceDate(Handler.loggedUser!!.username, carPlate, Timestamp(cal.time))
                    .addOnSuccessListener {
                        insurancePlaceholder.text = "Next insurance on: $formatted"
                        ToastManager.show(this, "Insurance date set to $formatted", Toast.LENGTH_SHORT)
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
            input.hint = "Enter insurance fee (€)"
            AlertDialog.Builder(this)
                .setTitle("Insurance Fee")
                .setView(input)
                .setPositiveButton("Submit") { _, _ ->
                    val feeText = input.text.toString()
                    val insurance = Insurance()
                    insurance.amount = feeText.toFloatOrNull()!!
                    insurance.date = Timestamp.now()
                    if (insurance.amount > 0) {
                        Handler.database.payInsurance(Handler.loggedUser!!.username, carPlate, insurance)
                            .addOnSuccessListener {
                                ToastManager.show(this, "Insurance fee of €${insurance.amount} submitted!", Toast.LENGTH_SHORT)
                                payInsuranceButton.isEnabled = false
                                payInsuranceButton.alpha = 0.5f
                                insurancePlaceholder.text = getString(R.string.insurance_placeholder)
                            }
                            .addOnFailureListener { e ->
                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                            }

                    } else {
                        ToastManager.show(this, "Please enter a valid amount", Toast.LENGTH_SHORT)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setTaxDateButton.setOnClickListener {
            val cal = Calendar.getInstance()
            val dialog = DatePickerDialog(this, { _, year, month, dayOfMonth ->
                cal.set(year, month, dayOfMonth)
                val formatted = android.text.format.DateFormat.format("dd-MM-yyyy", cal.time)
                Handler.database.setNewTaxDate(Handler.loggedUser!!.username, carPlate, Timestamp(cal.time))
                    .addOnSuccessListener {
                        taxPlaceholder.text = "Next tax on: $formatted"
                        ToastManager.show(this, "Tax date set to $formatted", Toast.LENGTH_SHORT)
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
            input.hint = "Enter tax fee (€)"
            AlertDialog.Builder(this)
                .setTitle("Tax Fee")
                .setView(input)
                .setPositiveButton("Submit") { _, _ ->
                    val feeText = input.text.toString()
                    val tax = Tax()
                    tax.amount = feeText.toFloatOrNull()!!
                    tax.date = Timestamp.now()
                    if (tax.amount > 0) {
                        Handler.database.payTax(Handler.loggedUser!!.username, carPlate, tax)
                            .addOnSuccessListener {
                                ToastManager.show(this, "Tax fee of €${tax.amount} submitted!", Toast.LENGTH_SHORT)
                                payTaxButton.isEnabled = false
                                payTaxButton.alpha = 0.5f
                                taxPlaceholder.text = getString(R.string.tax_placeholder)
                            }
                            .addOnFailureListener { e ->
                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
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
