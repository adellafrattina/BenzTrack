package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

/**
 * Global database instance for the application.
 */
val database = Database()

/**
 * Global variable to store the currently logged-in user.
 * Null when no user is logged in.
 */
var loggedUser: User? = null

/**
 * The main entry point of the application.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    /**
     * Initializes the activity and sets up the UI components and their listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton = findViewById(R.id.button_login)
        registerButton = findViewById(R.id.button_register)

        NotificationHandler.init(this)

        if (!BackgroundService.isRunning) {

            val intent = Intent(this, BackgroundService::class.java)
            startService(intent)
        }

        loginButton.setOnClickListener {

            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }

        registerButton.setOnClickListener {
            val intent = Intent(this, RegistrationActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }
}
