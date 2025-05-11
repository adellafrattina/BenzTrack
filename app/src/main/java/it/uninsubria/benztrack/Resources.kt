package it.uninsubria.benztrack

import android.content.Context
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

object ToastManager {

    private var currentToast: Toast? = null

    fun show(context: Context, message: String?, duration: Int) {

        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, duration).also { it.show() }
    }
}

public data class User(

    var username: String,
    var password: String,
    var email: String,
    var name: String,
    var surname: String
) {

    constructor(): this("", "", "", "", "")
}

enum class FuelType {

    Petrol,
    Diesel,
    Electric
}

public data class CarModel(

    var name: String,
    var year: Int,
    var capacity: Int,
    var fuel: FuelType,
    var co2factor: Float,
    var weight: Float,
    var length: Float,
    var height: Float
) {

    constructor(): this("", 1970, 0, FuelType.Petrol, 0.0f, 0.0f, 0.0f, 0.0f)
}

public data class Car(

    var plate: String,
    var name: String,
    var model: DocumentReference,
    var maintenancedate: Timestamp,
    var insurancedate: Timestamp,
    var taxdate: Timestamp
)

public data class Refill(

    var date: Timestamp,
    var position: String,
    var ppl: Float,
    var km: Float,
    var amount: Float
)
