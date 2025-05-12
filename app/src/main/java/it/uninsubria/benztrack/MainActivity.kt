package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

val database = Database()
var loggedUser: User? = null

class MainActivity : AppCompatActivity() {

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginButton = findViewById(R.id.button_login)
        registerButton = findViewById(R.id.button_register)

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
