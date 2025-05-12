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
            validateAndRegister()
        }

        // Set up login link click listener
        loginLink.setOnClickListener {
            // Navigate back to login screen
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    private fun validateAndRegister() {
        val username = usernameEditText.text.toString().trim()
        val name = nameEditText.text.toString().trim()
        val surname = surnameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        // Validate input fields
        if (username.isEmpty() || name.isEmpty() || surname.isEmpty() ||
            email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Simple email validation
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        // Password length validation
        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Registration successful, create intent to open ProfileActivity
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra("USERNAME", username)
            putExtra("PASSWORD", password)
            putExtra("NAME", name)
            putExtra("SURNAME", surname)
            putExtra("EMAIL", email)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

}