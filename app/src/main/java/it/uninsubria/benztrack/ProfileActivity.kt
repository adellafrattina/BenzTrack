package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem

/**
 * Activity responsible for displaying user profile information and car management.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var noCarsTextView: TextView
    private lateinit var addCarButton: Button
    private lateinit var graphsButton: Button

    /**
     * Initializes the activity and sets up the UI components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        noCarsTextView = findViewById(R.id.text_no_cars)
        addCarButton = findViewById(R.id.button_add_car)
        graphsButton = findViewById(R.id.button_graphs)

        // Set up button click listeners
        addCarButton.setOnClickListener {
            val intent = Intent(this, AddCarActivity::class.java)
            startActivity(intent)
        }

        graphsButton.setOnClickListener {
            // TODO: Implement graphs functionality
            Toast.makeText(this, "Graphs functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Creates the options menu.
     * 
     * @param menu The menu to inflate
     * @return true if the menu was created successfully
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.profile_menu, menu)
        return true
    }

    /**
     * Handles action bar item selection.
     * 
     * @param item The selected menu item
     * @return true if the item was handled, false otherwise
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
                true
            }
            R.id.action_logout -> {
                loggedUser = null
                ToastManager.show(this, "Logged out successfully", Toast.LENGTH_SHORT)
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Handles the back button press.
     * Provides a smooth transition animation when navigating back.
     */
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}