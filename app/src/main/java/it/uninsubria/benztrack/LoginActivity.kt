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
 * Activity responsible for handling user authentication.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout

    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText

    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView

    /**
     * Initializes the activity and sets up the UI components and their listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize views
        usernameLayout = findViewById(R.id.username_layout)
        passwordLayout = findViewById(R.id.password_layout)

        usernameEditText = findViewById(R.id.edit_username)
        passwordEditText = findViewById(R.id.edit_password)

        loginButton = findViewById(R.id.button_login)
        registerLink = findViewById(R.id.text_register_link)

        // Set up login button click listener
        loginButton.setOnClickListener {

            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            // Clear previous errors
            usernameLayout.error = null
            passwordLayout.error = null

            Handler.database.login(username, password)
                .addOnSuccessListener { user ->

                    ToastManager.show(this, "Login successful!", Toast.LENGTH_SHORT)
                    Handler.loggedUser = user
                    Handler.save()

                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                }

                .addOnFailureListener { e ->

                    when (e) {

                        is LoginException -> {

                            if (e.username.isNotEmpty())
                                usernameLayout.error = e.username

                            if (e.password.isNotEmpty())
                                passwordLayout.error = e.password

                            if (e.username.isEmpty() and e.password.isEmpty())
                                ToastManager.show(this, e.message, Toast.LENGTH_SHORT)

                            else
                                ToastManager.show(this, "Login failed", Toast.LENGTH_SHORT)
                        }

                        else -> ToastManager.show(this, "An unexpected error occurred", Toast.LENGTH_LONG)
                    }
                }
        }

        // Set up register link click listener
        registerLink.setOnClickListener {

            // Clear text fields
            usernameEditText.text?.clear()
            passwordEditText.text?.clear()
            usernameLayout.error = null
            passwordLayout.error = null

            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}