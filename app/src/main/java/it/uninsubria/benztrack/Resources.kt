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
    var width: Float,
    var length: Float,
    var height: Float,
    var searchterms: ArrayList<String>
) {

    constructor(): this("", Int.MAX_VALUE, Int.MAX_VALUE, FuelType.Petrol, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, ArrayList<String>())

    @Override
    public override fun toString(): String {

        return "$name\n$year\n$capacity\n$fuel\n$co2factor\n$weight$width\n$length\n$height"
    }
}

public data class Car(

    var plate: String,
    var name: String,
    var model: DocumentReference?,
    var maintenancedate: Timestamp?,
    var insurancedate: Timestamp?,
    var taxdate: Timestamp?
) {

    constructor(): this("", "", null, null, null, null)
}

public data class Refill(

    var date: Timestamp?,
    var position: String,
    var ppl: Float,
    var mileage: Float,
    var amount: Float
) {

    constructor(): this(null, "", Float.NaN, Float.NaN, Float.NaN)
}
