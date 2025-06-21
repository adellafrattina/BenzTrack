package it.uninsubria.benztrack

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText

class AddCarActivity : AppCompatActivity() {

    private lateinit var carNameEdit: TextInputEditText
    private lateinit var plateEdit: TextInputEditText
    private lateinit var modelEdit: TextInputEditText
    private lateinit var modelsRecyclerView: RecyclerView
    private lateinit var confirmButton: Button
    private lateinit var addModelButton: Button
    private lateinit var database: Database
    private var selectedModel: CarModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_car)

        // Enable the up button in the action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Initialize views
        carNameEdit = findViewById(R.id.edit_car_name)
        plateEdit = findViewById(R.id.edit_plate)
        modelEdit = findViewById(R.id.edit_model)
        modelsRecyclerView = findViewById(R.id.recycler_models)
        confirmButton = findViewById(R.id.button_confirm)
        addModelButton = findViewById(R.id.button_add_model)
        database = Database()

        // Set up RecyclerView
        modelsRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = CarModelAdapter { model ->

            selectedModel = model
            modelEdit.setText(model.name)
            modelsRecyclerView.visibility = View.GONE
        }
        modelsRecyclerView.adapter = adapter

        // Set up model search
        modelEdit.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrBlank()) {
                    modelsRecyclerView.visibility = View.GONE
                    return
                }
                database.searchCarModelByName(s.toString())
                    .addOnSuccessListener { models ->
                        if (models.isNotEmpty()) {
                            adapter.submitList(models)
                            modelsRecyclerView.visibility = View.VISIBLE
                        } else {
                            modelsRecyclerView.visibility = View.GONE
                        }
                    }
                    .addOnFailureListener { e ->
                        ToastManager.show(this@AddCarActivity, "Error searching models: ${e.message}", Toast.LENGTH_SHORT)
                        modelsRecyclerView.visibility = View.GONE
                    }
            }
        })

        // Set up confirm button
        confirmButton.setOnClickListener {
            val carName = carNameEdit.text.toString()
            val plate = plateEdit.text.toString()
            
            if (carName.isBlank() || plate.isBlank() || selectedModel == null) {

                ToastManager.show(this, "Please fill all fields", Toast.LENGTH_SHORT)
                return@setOnClickListener
            }

            val car = Car()
            car.name = carName
            car.plate = plate
            //car.model =

            database.getCarModelDocumentReference(selectedModel!!)
                .addOnSuccessListener { model ->

                    car.model = model
                }
                .addOnFailureListener { e ->

                    ToastManager.show(this, e.message, Toast.LENGTH_SHORT)
                }

            database.addNewUserCar(loggedUser!!.username, car)
                .addOnSuccessListener {

                    ToastManager.show(this, "Car added successfully", Toast.LENGTH_SHORT)
                    finish()
                }

                .addOnFailureListener { e ->

                    ToastManager.show(this, "Error adding car: ${e.message}", Toast.LENGTH_SHORT)
                }
        }

        addModelButton.setOnClickListener {
            val intent = Intent(this, AddCarModelActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class CarModelAdapter(private val onModelSelected: (CarModel) -> Unit) :
    RecyclerView.Adapter<CarModelAdapter.ViewHolder>() {

    private var models: ArrayList<CarModel> = arrayListOf()

    fun submitList(newModels: ArrayList<CarModel>) {
        models = newModels
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_car_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.bind(model)
    }

    override fun getItemCount() = models.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val modelNameText: TextView = itemView.findViewById(R.id.text_model_name)

        fun bind(model: CarModel) {

            modelNameText.text = model.name
            itemView.setOnClickListener {

                onModelSelected(model)
            }
        }
    }
} 