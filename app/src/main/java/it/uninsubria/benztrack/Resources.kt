package it.uninsubria.benztrack

import android.content.Context
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

/**
 * Toast wrapper class to avoid message queuing
 *
 * @author Le1nism
 */
object ToastManager {

    private var currentToast: Toast? = null

    /**
     * Show the toast
     *
     * @param context The activity context
     * @param message The message to display
     * @param duration The duration. Possible values: Toast.LENGTH_SHORT or Toast.LENGTH_LONG
     */
    fun show(context: Context, message: String?, duration: Int) {

        currentToast?.cancel()
        currentToast = Toast.makeText(context, message, duration).also { it.show() }
    }
}

/**
 * User data wrapper
 *
 * @author adellafrattina
 */
public data class User(

    /**
     * The user id
     */
    var username: String,

    /**
     * The user password
     */
    var password: String,

    /**
     * The user email
     */
    var email: String,

    /**
     * The user name
     */
    var name: String,

    /**
     * The user surname
     */
    var surname: String
) {

    constructor(): this("", "", "", "", "")
}

/**
 * All supported fuel types
 *
 * @author adellafrattina
 */
enum class FuelType {

    /**
     * For cars fueled by petrol
     */
    Petrol,

    /**
     * For cars fueled by diesel
     */
    Diesel,

    /**
     * For electric cars
     */
    Electric
}

/**
 * Car model data wrapper
 */
public data class CarModel(

    /**
     * The model's name (e.g. Alfa Romeo Tonale)
     */
    var name: String,

    /**
     * The model's year (needs to be valid)
     */
    var year: Int,

    /**
     * The model's capacity (expressed in cm^3)
     */
    var capacity: Int,

    /**
     * The model's fuel type
     */
    var fuel: FuelType,

    /**
     * The model's CO2 factor (in grams/kilometers)
     */
    var co2factor: Float,

    /**
     * The model's weight (in kilograms)
     */
    var weight: Float,

    /**
     * The model's width (in centimeters)
     */
    var width: Float,

    /**
     * The model's length (in centimeters)
     */
    var length: Float,

    /**
     * The model's height (in centimeters)
     */
    var height: Float,

    /**
     * The possible search terms that will be used in the search model algorithm
     */
    var searchterms: ArrayList<String>
) {

    constructor(): this("", Int.MAX_VALUE, Int.MAX_VALUE, FuelType.Petrol, Float.NaN, Float.NaN, Float.NaN, Float.NaN, Float.NaN, ArrayList<String>())

    @Override
    public override fun toString(): String {

        return "$name\n$year\n$capacity\n$fuel\n${co2factor.toInt().toFloat()}\n${weight.toInt().toFloat()}\n${length.toInt().toFloat()}\n${height.toInt().toFloat()}"
    }
}

/**
 * Car data wrapper
 *
 * @author adellafrattina
 */
public data class Car(

    /**
     * The car's plate string
     */
    var plate: String,

    /**
     * The car's arbitrary name
     */
    var name: String,

    /**
     * The car's model
     */
    var model: DocumentReference?,

    /**
     * The next maintenance date
     */
    var maintenancedate: Timestamp?,

    /**
     * The next insurance date
     */
    var insurancedate: Timestamp?,

    /**
     * The next tax date
     */
    var taxdate: Timestamp?
) {

    constructor(): this("", "", null, null, null, null)
}

/**
 * Refill data wrapper
 */
public data class Refill(

    /**
     * The refill's date
     */
    var date: Timestamp?,

    /**
     * The refill's fuel dispenser position (should be expressed in coordinates)
     */
    var position: String,

    /**
     * The refill's price per liter (euros/liters)
     */
    var ppl: Float,

    /**
     * The car's mileage at the time of the refill. The name is misleading, since it is measured in kilometers and not miles
     */
    var mileage: Float,

    /**
     * The refill's amount in euros
     */
    var amount: Float
) {

    constructor(): this(null, "", Float.NaN, Float.NaN, Float.NaN)
}
