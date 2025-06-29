package it.uninsubria.benztrack

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarAdapter(private val cars: MutableList<Car>, private val onCarClick: (Car) -> Unit, private val onCarLongClick: (Car) -> Unit) : RecyclerView.Adapter<CarAdapter.CarViewHolder>() {

    class CarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val name: TextView = itemView.findViewById(R.id.text_car_name)
        val model: TextView = itemView.findViewById(R.id.text_car_model)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarViewHolder {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_car, parent, false)
        return CarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarViewHolder, position: Int) {

        val car = cars[position]
        holder.name.text = car.name + " - " + car.plate
        holder.model.text = "Loading..."

        car.model?.get()?.addOnSuccessListener { document ->

            if (document != null && document.exists()) {

                val modelName = document.getString(Database.NAME_FIELD) ?: "Unknown Model"
                holder.model.text = modelName
            }

            else {

                holder.model.text = "Unknown Model"
            }

        }?.addOnFailureListener {

            holder.model.text = "Error loading model"
        }

        holder.itemView.setOnClickListener {

            onCarClick(car)
        }

        holder.itemView.setOnLongClickListener {

            onCarLongClick(car)
            true
        }
    }

    override fun getItemCount() = cars.size

    fun removeCar(car: Car) {

        val index = cars.indexOf(car)

        if (index != -1) {

            cars.removeAt(index)
            notifyItemRemoved(index)
        }
    }
} 