package it.uninsubria.benztrack

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button: Button = findViewById(R.id.button_greeting)
        button.setOnClickListener {

            Toast.makeText(this, "Hello World!", Toast.LENGTH_LONG).show()
        }
    }
}
