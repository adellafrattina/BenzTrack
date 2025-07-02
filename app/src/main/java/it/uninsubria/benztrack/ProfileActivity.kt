package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog

/**
 * Activity responsible for displaying user profile information and car management.
 */
class ProfileActivity : AppCompatActivity() {

    private lateinit var noCarsTextView: TextView
    private lateinit var addCarButton: Button
    private lateinit var carsRecyclerView: RecyclerView
    private var carAdapter: CarAdapter? = null

    /**
     * Initializes the activity and sets up the UI components.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)
        Handler.database.setContext(this)

        // Initialize views
        noCarsTextView = findViewById(R.id.text_no_cars)
        addCarButton = findViewById(R.id.button_add_car)
        carsRecyclerView = findViewById(R.id.recycler_cars)
        carsRecyclerView.layoutManager = LinearLayoutManager(this)
        carsRecyclerView.visibility = View.GONE

        // Set up button click listeners
        addCarButton.setOnClickListener {

            val intent = Intent(this, AddCarActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {

        super.onResume()
        Handler.database.setContext(this)

        // Load cars for the logged user
        if (Handler.loggedUser != null) {

            Database().getUserCars(Handler.loggedUser!!.username)
                .addOnSuccessListener { cars ->

                    if (cars.isNotEmpty()) {

                        carAdapter = CarAdapter(cars.toMutableList(), { car ->
                            val intent = Intent(this, CarGraphActivity::class.java)
                            intent.putExtra("car_plate", car.plate)
                            startActivity(intent)
                        }, { car ->

                            AlertDialog.Builder(this)
                                .setTitle(getString(R.string.delete_car))
                                .setMessage(getString(R.string.delete_car_confirm, car.name, car.plate))
                                .setPositiveButton(getString(R.string.yes)) { _, _ ->

                                    Handler.database.deleteUserCar(Handler.loggedUser!!.username, car.plate)
                                        .addOnSuccessListener {

                                            ToastManager.show(this, getString(R.string.deleted_car, car.name, car.plate), Toast.LENGTH_SHORT)
                                            carAdapter?.removeCar(car)
                                        }
                                        .addOnFailureListener { e ->

                                            ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                                        }
                                }
                                .setNegativeButton(getString(R.string.no), null)
                                .show()
                        })

                        carsRecyclerView.adapter = carAdapter
                        carsRecyclerView.visibility = View.VISIBLE
                        noCarsTextView.visibility = View.GONE
                    }

                    else {

                        carsRecyclerView.visibility = View.GONE
                        noCarsTextView.visibility = View.VISIBLE
                    }
                }

                .addOnFailureListener {

                    carsRecyclerView.visibility = View.GONE
                    noCarsTextView.visibility = View.VISIBLE
                }
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

            R.id.action_view_info -> {

                val intent = Intent(this, UserInfoActivity::class.java)
                startActivity(intent)
                true
            }

            R.id.action_logout -> {

                Handler.loggedUser = null
                Handler.save()
                ToastManager.show(this, getString(R.string.logged_out_success), Toast.LENGTH_SHORT)
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