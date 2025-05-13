package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textfield.TextInputEditText

/**
 * Activity responsible for handling user registration.
 */
class RegistrationActivity : AppCompatActivity() {

    private lateinit var usernameLayout: TextInputLayout
    private lateinit var nameLayout: TextInputLayout
    private lateinit var surnameLayout: TextInputLayout
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var nameEditText: TextInputEditText
    private lateinit var surnameEditText: TextInputEditText
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView

    /**
     * Initializes the activity and sets up the UI components and their listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        // Initialize views
        usernameLayout = findViewById(R.id.username_layout)
        nameLayout = findViewById(R.id.name_layout)
        surnameLayout = findViewById(R.id.surname_layout)
        emailLayout = findViewById(R.id.email_layout)
        passwordLayout = findViewById(R.id.password_layout)
        usernameEditText = findViewById(R.id.edit_reg_username)
        nameEditText = findViewById(R.id.edit_name)
        surnameEditText = findViewById(R.id.edit_surname)
        emailEditText = findViewById(R.id.edit_email)
        passwordEditText = findViewById(R.id.edit_reg_password)
        registerButton = findViewById(R.id.button_register)
        loginLink = findViewById(R.id.text_login_link)

        // Set up register button click listener
        registerButton.setOnClickListener {
            // Clear previous errors
            clearErrors()

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
                    when (e) {
                        is RegistrationException -> {
                            if (e.username.isNotEmpty()) {
                                showError(usernameLayout, e.username)
                            }
                            if (e.name.isNotEmpty()) {
                                showError(nameLayout, e.name)
                            }
                            if (e.surname.isNotEmpty()) {
                                showError(surnameLayout, e.surname)
                            }
                            if (e.email.isNotEmpty()) {
                                showError(emailLayout, e.email)
                            }
                            if (e.password.isNotEmpty()) {
                                showError(passwordLayout, e.password)
                            }
                            ToastManager.show(this, "Registration unsuccessful", Toast.LENGTH_SHORT)
                        }
                        else -> ToastManager.show(this, "An unexpected error occurred", Toast.LENGTH_LONG)
                    }
                }
        }

        // Set up login link click listener
        loginLink.setOnClickListener {
            clearFields()
            clearErrors()
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    /**
     * Clears all input fields in the registration form.
     */
    private fun clearFields() {
        usernameEditText.text?.clear()
        nameEditText.text?.clear()
        surnameEditText.text?.clear()
        emailEditText.text?.clear()
        passwordEditText.text?.clear()
    }

    /**
     * Clears all error states from the TextInputLayout components.
     * This removes error messages and collapses the error space.
     */
    private fun clearErrors() {
        usernameLayout.isErrorEnabled = false
        nameLayout.isErrorEnabled = false
        surnameLayout.isErrorEnabled = false
        emailLayout.isErrorEnabled = false
        passwordLayout.isErrorEnabled = false
    }

    /**
     * Displays an error message in the specified TextInputLayout.
     * 
     * @param layout The TextInputLayout to show the error in
     * @param message The error message to display
     */
    private fun showError(layout: TextInputLayout, message: String) {
        layout.error = message
        layout.isErrorEnabled = true
    }

    /**
     * Handles the back button press.
     * Clears all fields and errors before navigating back.
     */
    override fun onBackPressed() {
        clearFields()
        clearErrors()
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}