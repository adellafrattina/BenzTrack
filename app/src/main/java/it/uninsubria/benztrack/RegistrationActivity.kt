package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegistrationActivity : AppCompatActivity() {

    private lateinit var usernameEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var surnameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize views
        usernameEditText = findViewById(R.id.edit_reg_username)
        nameEditText = findViewById(R.id.edit_name)
        surnameEditText = findViewById(R.id.edit_surname)
        emailEditText = findViewById(R.id.edit_email)
        passwordEditText = findViewById(R.id.edit_reg_password)
        registerButton = findViewById(R.id.button_register)
        loginLink = findViewById(R.id.text_login_link)

        // Set up register button click listener
        registerButton.setOnClickListener {

            val user = User()

            user.username = usernameEditText.text.toString().trim()
            user.name = nameEditText.text.toString().trim()
            user.surname = surnameEditText.text.toString().trim()
            user.email = emailEditText.text.toString().trim()
            user.password = passwordEditText.text.toString().trim()

            database.registration(user)
                .addOnSuccessListener {
                    ToastManager.show(this, "Registration successful!", Toast.LENGTH_SHORT)
                    loggedUser = user

                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish() // Finish registration activity after successful registration
                }
                .addOnFailureListener { e ->
                    ToastManager.show(this, e.message, Toast.LENGTH_LONG)
                }
        }

        // Set up login link click listener
        loginLink.setOnClickListener {
            clearFields()
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun clearFields() {

        usernameEditText.text?.clear()
        nameEditText.text?.clear()
        surnameEditText.text?.clear()
        emailEditText.text?.clear()
        passwordEditText.text?.clear()
    }

    override fun onBackPressed() {

        clearFields()
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}