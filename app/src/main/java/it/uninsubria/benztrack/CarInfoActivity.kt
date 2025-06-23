package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class CarInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_car_info)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val addRefillButton = findViewById<Button>(R.id.button_add_refill)
        val carPlate = intent.getStringExtra("car_plate")
        addRefillButton.setOnClickListener {
            val intent = Intent(this, AddRefillActivity::class.java)
            intent.putExtra("car_plate", carPlate)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
