package it.uninsubria.benztrack

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val u = User()
        u.username = "admin"
        u.password = "admin"
        u.name = "admin"
        u.surname = "admin"
        u.email = "benztrackdatabase@gmail.com"

        val button: Button = findViewById(R.id.button_greeting)
        button.setOnClickListener {

            db
                .registration(u)
                .addOnSuccessListener {

                    ToastManager.show(this, "Registration was successful", Toast.LENGTH_SHORT)
                }
                .addOnFailureListener { e ->

                    e as RegistrationException
                    if (e.username.isNotBlank())
                        Log.e("test", e.username)
                    if (e.password.isNotBlank())
                        Log.e("test", e.password)
                    if (e.email.isNotBlank())
                        Log.e("test", e.email)
                    if (e.name.isNotBlank())
                        Log.e("test", e.name)
                    if (e.surname.isNotBlank())
                        Log.e("test", e.surname)

                    ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                }
        }
    }

    private val db = Database()
}
