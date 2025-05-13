package it.uninsubria.benztrack

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Activity responsible for displaying user profile information.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var usernameTextView: TextView
    private lateinit var nameTextView: TextView
    private lateinit var surnameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var nameLabelTextView: TextView
    private lateinit var surnameLabelTextView: TextView
    private lateinit var emailLabelTextView: TextView
    private lateinit var backButton: Button

    /**
     * Initializes the activity and sets up the UI components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        usernameTextView = findViewById(R.id.text_username)
        nameTextView = findViewById(R.id.text_name)
        surnameTextView = findViewById(R.id.text_surname)
        emailTextView = findViewById(R.id.text_email)
        nameLabelTextView = findViewById(R.id.label_name)
        surnameLabelTextView = findViewById(R.id.label_surname)
        emailLabelTextView = findViewById(R.id.label_email)
        backButton = findViewById(R.id.button_back)

        // Get data from logged user
        val username = loggedUser?.username
        val name = loggedUser?.name
        val surname = loggedUser?.surname
        val email = loggedUser?.email

        // Set data to views
        usernameTextView.text = username

        // Check if additional fields are available (from registration)
        nameTextView.text = name
        surnameTextView.text = surname
        emailTextView.text = email

        // Set up back button click listener
        backButton.setOnClickListener {
            finish() // Close this activity and return to previous one
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
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

    /**
     * Handles action bar item selection.
     * 
     * @param item The selected menu item
     * @return true if the item was handled, false otherwise
     */
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}