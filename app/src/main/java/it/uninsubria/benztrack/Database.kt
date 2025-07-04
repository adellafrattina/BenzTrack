package it.uninsubria.benztrack

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.Date

/**
 * The database class
 * @author adellafrattina
 */
public class Database {

    companion object {

        public const val USERS_COLLECTION = "users"
        public const val MODELS_COLLECTION = "models"
        public const val CARS_COLLECTION = "cars"
        public const val REFILLS_COLLECTION = "refills"
        public const val MAINTENANCE_COLLECTION = "maintenance"
        public const val INSURANCE_COLLECTION = "insurance"
        public const val TAX_COLLECTION = "tax"

        public const val USERNAME_FIELD = "username"
        public const val PASSWORD_FIELD = "password"
        public const val EMAIL_FIELD = "email"
        public const val NAME_FIELD = "name"
        public const val SURNAME_FIELD = "surname"

        public const val YEAR_FIELD = "year"
        public const val CAPACITY_FIELD = "capacity"
        public const val FUEL_FIELD = "fuel"
        public const val CO2_FACTOR_FIELD = "co2factor"
        public const val WEIGHT_FIELD = "weight"
        public const val WIDTH_FIELD = "width"
        public const val LENGTH_FIELD = "length"
        public const val HEIGHT_FIELD = "height"
        public const val FUEL_CAPACITY_FIELD = "fuelcapacity"
        public const val SEARCH_TERMS_FIELD = "searchterms"

        public const val PLATE_FIELD = "plate"
        public const val MODEL_FIELD = "model"
        public const val MAINTENANCE_DATE_FIELD = "maintenancedate"
        public const val INSURANCE_DATE_FIELD = "insurancedate"
        public const val TAX_DATE_FIELD = "taxdate"

        public const val DATE_FIELD = "date"
        public const val POSITION_FIELD = "position"
        public const val PRICE_PER_LITER_FIELD = "ppl"
        public const val MILEAGE_FIELD = "mileage"
        public const val AMOUNT_FIELD = "amount"
        public const val CURRENT_FUEL_AMOUNT_FIELD = "currentfuelamount"
    }

    public fun setContext(context: Context) {

        contextRef = context
    }

    /**
     * Logs into an account with the specified parameters
     *
     * @param username The user's id
     * @param password The user's password
     * @return The user if the login was successful
     */
    public fun login(username: String, password: String): Task<User> {

        val taskSource = TaskCompletionSource<User>()
        var usernameErrorMsg = ""
        var passwordErrorMsg = ""

        // Check username
        if (username.isEmpty())
            usernameErrorMsg = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check password
        if (password.isEmpty())
            passwordErrorMsg = contextRef.getString(R.string.this_field_must_not_be_empty)

        if (username.isEmpty() || password.isEmpty())
            taskSource.setException(LoginException(
                contextRef.getString(R.string.login_unsuccessful),
                usernameErrorMsg,
                passwordErrorMsg))

        else {

            dbRef
                .collection(USERS_COLLECTION)
                .document(username)
                .get()
                .addOnSuccessListener { document ->

                    if (document.exists()) {

                        val storedPassword = document.getString(PASSWORD_FIELD)
                        if (storedPassword == sha256(password)) {

                            val user = document.toObject(User::class.java)
                            if (user != null) {

                                user.password = password
                                taskSource.setResult(user)
                            }

                            else
                                taskSource.setException(LoginException("User object is null"))
                        }

                        else
                            taskSource.setException(LoginException(contextRef.getString(R.string.username_or_password_are_incorrect)))
                    }

                    else
                        taskSource.setException(LoginException(contextRef.getString(R.string.username_or_password_are_incorrect)))
                }
                .addOnFailureListener { e ->

                    taskSource.setException(LoginException(e.message?:""))
                }
        }

        return taskSource.task
    }

    /**
     * Registers a new user. It will check if the user parameters are valid
     *
     * @param user The new user
     * @return True if the registration was successful, an exception if some parameters where invalid
     */
    public fun registration(user: User): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()
        val errorMap = HashMap<String, String>()

        // Check password
        if (user.password.isEmpty())
            errorMap[PASSWORD_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check email
        if (user.email.isEmpty())
            errorMap[EMAIL_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)
        else if (!Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$").matches(user.email))
            errorMap[EMAIL_FIELD] = contextRef.getString(R.string.the_email_must_be_valid)

        // Check name
        if (user.name.isEmpty())
            errorMap[NAME_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check surname
        if (user.surname.isEmpty())
            errorMap[SURNAME_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check username
        if (user.username.isEmpty()) {

            errorMap[USERNAME_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)
            taskSource.setException(RegistrationException(
                contextRef.getString(R.string.registration_unsuccessful),
                if (errorMap[USERNAME_FIELD] != null) errorMap[USERNAME_FIELD]!! else "",
                if (errorMap[PASSWORD_FIELD] != null) errorMap[PASSWORD_FIELD]!! else "",
                if (errorMap[EMAIL_FIELD] != null)    errorMap[EMAIL_FIELD]!! else "",
                if (errorMap[NAME_FIELD] != null)     errorMap[NAME_FIELD]!! else "",
                if (errorMap[SURNAME_FIELD] != null)  errorMap[SURNAME_FIELD]!! else ""))
        }

        else {

            dbRef
                .collection(USERS_COLLECTION)
                .document(user.username)
                .get()
                .addOnSuccessListener { document ->

                    // The user name already exists
                    if (document.exists()) {

                        errorMap[USERNAME_FIELD] = contextRef.getString(R.string.username_already_taken)
                        taskSource.setException(RegistrationException(
                            contextRef.getString(R.string.registration_unsuccessful),
                            if (errorMap[USERNAME_FIELD] != null) errorMap[USERNAME_FIELD]!! else "",
                            if (errorMap[PASSWORD_FIELD] != null) errorMap[PASSWORD_FIELD]!! else "",
                            if (errorMap[EMAIL_FIELD] != null)    errorMap[EMAIL_FIELD]!! else "",
                            if (errorMap[NAME_FIELD] != null)     errorMap[NAME_FIELD]!! else "",
                            if (errorMap[SURNAME_FIELD] != null)  errorMap[SURNAME_FIELD]!! else ""))
                    }

                    else {

                        // Register the user in the database only if there are no errors
                        if (errorMap.isEmpty()) {

                            // Hash the password before storing it into the database
                            val pwd = user.password
                            user.password = sha256(user.password)

                            dbRef
                                .collection(USERS_COLLECTION)
                                .document(user.username)
                                .set(user)
                                .addOnSuccessListener {

                                    // Reset the un-hashed password into the user structure
                                    user.password = pwd
                                    taskSource.setResult(true)
                                }
                                .addOnFailureListener { e ->

                                    taskSource.setException(RegistrationException(e.message?:""))
                                }
                        }

                        else {

                            taskSource.setException(RegistrationException(
                                contextRef.getString(R.string.registration_unsuccessful),
                                if (errorMap[USERNAME_FIELD] != null) errorMap[USERNAME_FIELD]!! else "",
                                if (errorMap[PASSWORD_FIELD] != null) errorMap[PASSWORD_FIELD]!! else "",
                                if (errorMap[EMAIL_FIELD] != null)    errorMap[EMAIL_FIELD]!! else "",
                                if (errorMap[NAME_FIELD] != null)     errorMap[NAME_FIELD]!! else "",
                                if (errorMap[SURNAME_FIELD] != null)  errorMap[SURNAME_FIELD]!! else ""))
                        }
                    }
                }
                .addOnFailureListener { e ->

                    taskSource.setException(RegistrationException(e.message?:""))
                }
        }

        return taskSource.task
    }

    /**
     * Creates a new car model. It will check if the model parameters are valid. Warning: once created, it can't be removed
     *
     * @param model The car model
     */
    public fun createCarModel(model: CarModel): Task<DocumentReference> {

        val taskSource = TaskCompletionSource<DocumentReference>()
        val errorMap = HashMap<String, String>()

        // Check name
        if (model.name.isEmpty())
            errorMap[NAME_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check year
        if (model.year < 1900 || model.year > LocalDate.now().year)
            errorMap[YEAR_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check car
        if (model.capacity < 49) // Smallest possible car (From what I've researched)
            errorMap[CAPACITY_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check CO2 factor
        if (model.co2factor.isNaN() || model.co2factor < 0)
            errorMap[CO2_FACTOR_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check weight
        if (model.weight < 50) // Lightest possible car (From what I've researched)
            errorMap[WEIGHT_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check weight
        if (model.width < 1) // Smallest possible car (From what I've researched)
            errorMap[WIDTH_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check length
        if (model.length < 2) // Shortest possible car (From what I've researched)
            errorMap[LENGTH_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check height
        if (model.height < 1) // Shortest possible car (From what I've researched)
            errorMap[HEIGHT_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check fuel capacity
        if (model.fuelcapacity.isNaN() || model.fuelcapacity < 0)
            errorMap[FUEL_CAPACITY_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        if (errorMap.isEmpty()) {

            dbRef
                .collection(MODELS_COLLECTION)
                .document(sha256(model.toString()))
                .get()
                .addOnSuccessListener { document ->

                    // The car model already exists
                    if (document.exists()) {

                        errorMap["message"] = contextRef.getString(R.string.car_model_already_exists)
                        taskSource.setException(CarModelException(message=errorMap["message"]!!))
                    }

                    else {

                        // Add search terms
                        val terms = model.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        for (term in terms)
                            model.searchterms.add(term.uppercase())

                        val hash = sha256(model.toString())
                        dbRef
                            .collection(MODELS_COLLECTION)
                            .document(hash)
                            .set(model)
                            .addOnSuccessListener {

                                // Return the document reference
                                dbRef
                                    .collection(MODELS_COLLECTION)
                                    .document(hash)
                                    .get()
                                    .addOnSuccessListener { query ->

                                        taskSource.setResult(query.reference)
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(CarModelException(e.message?:""))
                                    }
                            }
                            .addOnFailureListener { e ->

                                taskSource.setException(CarModelException(e.message?:""))
                            }
                    }
                }
                .addOnFailureListener { e ->

                    taskSource.setException(CarModelException(e.message?:""))
                }
        }

        else {

            errorMap["message"] = contextRef.getString(R.string.this_car_model_cannot_exist)
            taskSource.setException(CarModelException(
                if (errorMap["message"] != null)                errorMap["message"]!! else "" ,
                if (errorMap[NAME_FIELD] != null)               errorMap[NAME_FIELD]!! else "",
                if (errorMap[YEAR_FIELD] != null)               errorMap[YEAR_FIELD]!! else "",
                if (errorMap[CAPACITY_FIELD] != null)           errorMap[CAPACITY_FIELD]!! else "",
                if (errorMap[FUEL_FIELD] != null)               errorMap[FUEL_FIELD]!! else "",
                if (errorMap[CO2_FACTOR_FIELD] != null)         errorMap[CO2_FACTOR_FIELD]!! else "",
                if (errorMap[WEIGHT_FIELD] != null)             errorMap[WEIGHT_FIELD]!! else "",
                if (errorMap[WIDTH_FIELD] != null)              errorMap[WIDTH_FIELD]!! else "",
                if (errorMap[LENGTH_FIELD] != null)             errorMap[LENGTH_FIELD]!! else "",
                if (errorMap[HEIGHT_FIELD] != null)             errorMap[HEIGHT_FIELD]!! else "",
                if (errorMap[FUEL_CAPACITY_FIELD] != null)      errorMap[FUEL_CAPACITY_FIELD]!! else ""))
        }

        return taskSource.task
    }

    /**
     * Searches the car model by name
     *
     * @param name The string the user typed. It will be used to search all the matching car model's name
     */
    public fun searchCarModelByName(name: String): Task<ArrayList<CarModel>> {

        val taskSource = TaskCompletionSource<ArrayList<CarModel>>()

        if (name.trim().isEmpty()) {

            taskSource.setException(CarModelException(contextRef.getString(R.string.cannot_find_the_specified_model)))

            return taskSource.task
        }

        val terms = name.trim().uppercase().split("\\s+".toRegex()).filter { it.isNotBlank() }

        // Search by searchterms array
        dbRef
            .collection(MODELS_COLLECTION)
            .whereArrayContainsAny(SEARCH_TERMS_FIELD, terms)
            .get()
            .addOnSuccessListener { primaryQuery ->

                if (!primaryQuery.isEmpty) {

                    val models = ArrayList<CarModel>()
                    for (document in primaryQuery) {

                        val model = document.toObject(CarModel::class.java)
                        models.add(model)
                    }

                    taskSource.setResult(models)
                }

                else {

                    // Alternative type of search if the searchterms array was null
                    dbRef
                        .collection(MODELS_COLLECTION)
                        .whereEqualTo(NAME_FIELD, name.trim())
                        .get()
                        .addOnSuccessListener { secondaryQuery ->

                            if (!secondaryQuery.isEmpty) {

                                val models = ArrayList<CarModel>()
                                for (document in secondaryQuery) {

                                    val model = document.toObject(CarModel::class.java)
                                    models.add(model)
                                }

                                taskSource.setResult(models)
                            }

                            else {

                                taskSource.setException(CarModelException(contextRef.getString(R.string.cannot_find_the_specified_model)))
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(CarModelException(e.message?:""))
                        }
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(CarModelException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the document reference of a specified car model
     *
     * @param model The car's model
     */
    public fun getCarModelDocumentReference(model: CarModel): Task<DocumentReference?> {

        val taskSource = TaskCompletionSource<DocumentReference?>()

        dbRef
            .collection(MODELS_COLLECTION)
            .document(sha256(model.toString()))
            .get()
            .addOnSuccessListener { document ->

                if (document.exists())
                    taskSource.setResult(document.reference)
                else
                    taskSource.setException(CarModelException(contextRef.getString(R.string.cannot_find_the_specified_model)))
            }
            .addOnFailureListener { e ->

                taskSource.setException(CarModelException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Adds a new car to the user's car collection
     *
     * @param username The user that desires to monitor a new car
     * @param car The actual car. It needs to be valid
     */
    public fun addNewUserCar(username: String, car: Car): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        val errorMap = checkNewCarParameters(car)
        if (errorMap.isNotEmpty()) {

            taskSource.setException(CarException(
                contextRef.getString(R.string.car_creation_has_failed),
                if (errorMap[PLATE_FIELD] != null)            errorMap[PLATE_FIELD]!! else "",
                if (errorMap[NAME_FIELD] != null)             errorMap[NAME_FIELD]!! else "",
                if (errorMap[MAINTENANCE_DATE_FIELD] != null) errorMap[MAINTENANCE_DATE_FIELD]!! else "",
                if (errorMap[INSURANCE_DATE_FIELD] != null)   errorMap[INSURANCE_DATE_FIELD]!! else "",
                if (errorMap[TAX_DATE_FIELD] != null)         errorMap[TAX_DATE_FIELD]!! else ""
            ))
        }

        else {

            isCarPresent(username, car.plate)
                .addOnSuccessListener { carPresent ->

                    if (carPresent) {

                        taskSource.setException(CarException(contextRef.getString(R.string.plate_already_present)))
                    }

                    else {

                        dbRef
                            .collection(USERS_COLLECTION)
                            .document(username)
                            .collection(CARS_COLLECTION)
                            .document(car.plate)
                            .set(car)
                            .addOnSuccessListener {

                                taskSource.setResult(true)
                            }
                            .addOnFailureListener { e ->

                                taskSource.setException(CarException(e.message?:""))
                            }
                    }
                }
                .addOnFailureListener { e ->

                    taskSource.setException(CarException(e.message?:""))
                }
        }

        return taskSource.task
    }

    /**
     * Deletes a car from the user's car collection
     *
     * @param username The car owner's id
     * @param plate The car's plate
     */
    public fun deleteUserCar(username: String, plate: String): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    // Delete refills collection
                    val refillsPath = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(REFILLS_COLLECTION)

                    deleteSubcollection(refillsPath)
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }

                    // Delete maintenance collection
                    val maintenancePath = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(MAINTENANCE_COLLECTION)

                    deleteSubcollection(maintenancePath)
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }

                    // Delete insurance collection
                    val insurancePath = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(INSURANCE_COLLECTION)

                    deleteSubcollection(insurancePath)
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }

                    // Delete tax collection
                    val taxPath = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(TAX_COLLECTION)

                    deleteSubcollection(taxPath)
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .delete()
                        .addOnSuccessListener {

                            taskSource.setResult(true)
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(CarException("The user does not have the specified car"))
                }
            }

        return taskSource.task
    }

    /**
     * Gets all the cars owned by a specific user
     *
     * @param username The user's id
     * @throws CarException
     */
    public fun getUserCars(username: String): Task<ArrayList<Car>> {

        val taskSource = TaskCompletionSource<ArrayList<Car>>()

        isUserPresent(username)
            .addOnSuccessListener { userPresent ->

                if (userPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .get()
                        .addOnSuccessListener { query ->

                            val list = ArrayList<Car>()
                            for (document in query.documents) {

                                val car = document.toObject(Car::class.java)
                                if (car != null)
                                    list.add(car)
                            }

                            taskSource.setResult(list)
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(CarException("The user does not exist"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(CarException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the specified car owned by a specific user
     *
     * @param username The user's id
     * @param plate The car's plate
     * @throws CarException
     */
    public fun getUserCar(username: String, plate: String): Task<Car> {

        val taskSource = TaskCompletionSource<Car>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .get()
                        .addOnSuccessListener { document ->

                            val car = document.toObject(Car::class.java)
                            if (car != null)
                                taskSource.setResult(car)
                            else
                                taskSource.setException(CarException("Cannot convert document object to Car type"))
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(CarException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(CarException("The user does not exist"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(CarException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the specified car model owned by a specific user
     *
     * @param username The user's id
     * @param plate The car's plate
     * @throws CarModelException
     */
    public fun getUserCarModel(username: String, plate: String): Task<CarModel> {

        val taskSource = TaskCompletionSource<CarModel>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .get()
                        .addOnSuccessListener { document ->

                            val car = document.toObject(Car::class.java)

                            if (car != null) {

                                car.model!!.get()
                                    .addOnSuccessListener { doc ->

                                        val model = doc.toObject(CarModel::class.java)
                                        if (model != null)
                                            taskSource.setResult(model)
                                        else
                                            taskSource.setException(CarModelException("Cannot convert document object to CarModel type"))
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(CarModelException(e.message?:""))
                                    }
                            }

                            else
                                taskSource.setException(CarModelException("Cannot convert document object to Car type"))
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(CarModelException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(CarModelException("The user does not exist"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(CarModelException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Sets a new refill for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param refill The actual refill data
     */
    public fun setNewRefill(username: String, plate: String, refill: Refill): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()
        val errorMap = HashMap<String, String>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    val lessTask = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(REFILLS_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                        .whereLessThanOrEqualTo(DATE_FIELD, refill.date)
                        .limit(1)
                        .get()

                    val greaterTask = dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(REFILLS_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.ASCENDING)
                        .whereGreaterThan(DATE_FIELD, refill.date)
                        .limit(1)
                        .get()

                    val modelTask = getUserCarModel(username, plate)

                    Tasks.whenAll(lessTask, greaterTask, modelTask)
                        .addOnCompleteListener {

                            if (it.isSuccessful) {

                                val prevRefill: Refill? =
                                    if (lessTask.result.documents.isNotEmpty())
                                        lessTask.result.documents[0].toObject(Refill::class.java)
                                    else
                                        null

                                val nextRefill: Refill? =
                                    if (greaterTask.result.documents.isNotEmpty())
                                        greaterTask.result.documents[0].toObject(Refill::class.java)
                                    else
                                        null

                                val model = modelTask.result

                                val fuelCapacity = model.fuelcapacity

                                if (fuelCapacity < refill.amount / refill.ppl + refill.currentfuelamount)
                                    errorMap["message"] = contextRef.getString(R.string.fuel_capacity_overflow, fuelCapacity.toString(), (refill.amount / refill.ppl + refill.currentfuelamount - fuelCapacity).toString())// "Your car can only contain ${fuelCapacity}L of fuel (there are ${refill.amount / refill.ppl + refill.currentfuelamount - fuelCapacity} extra liters)"

                                // Check mileage
                                if (refill.mileage.isNaN())
                                    errorMap[MILEAGE_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

                                // Check price per liter
                                if (refill.ppl.isNaN())
                                    errorMap[PRICE_PER_LITER_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

                                else if (refill.ppl < 0)
                                    errorMap[PRICE_PER_LITER_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

                                // Check amount
                                if (refill.amount.isNaN())
                                    errorMap[AMOUNT_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

                                else if (refill.amount < 0)
                                    errorMap[AMOUNT_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

                                // Check current fuel amount
                                if (refill.currentfuelamount.isNaN())
                                    errorMap[CURRENT_FUEL_AMOUNT_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)
                                else if (refill.currentfuelamount < 0)
                                    errorMap[CURRENT_FUEL_AMOUNT_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

                                // Check consistency
                                if (refill.amount != 0.0f && refill.ppl == 0.0f)
                                    errorMap[AMOUNT_FIELD] = contextRef.getString(R.string.not_consistent_with_ppl)

                                // Check previous refill data
                                if (prevRefill != null) {

                                    if (prevRefill.mileage > refill.mileage && !refill.mileage.isNaN())
                                        errorMap[MILEAGE_FIELD] = contextRef.getString(R.string.prev_mileage_not_valid, prevRefill.mileage.toString())// "The mileage value is not valid (should be higher than the previous one - ${prevRefill.mileage}km)"

                                    if (refill.currentfuelamount > prevRefill.currentfuelamount + prevRefill.amount / prevRefill.ppl)
                                        errorMap[CURRENT_FUEL_AMOUNT_FIELD] = contextRef.getString(R.string.prev_current_fuel_amount_not_valid, (prevRefill.currentfuelamount + prevRefill.amount / prevRefill.ppl).toString())// "The current fuel amount is not valid (should be less than ${prevRefill.currentfuelamount + prevRefill.amount / prevRefill.ppl}L)"
                                }

                                // Check next refill data
                                if (nextRefill != null) {

                                    if (nextRefill.mileage < refill.mileage && !refill.mileage.isNaN())
                                        errorMap[MILEAGE_FIELD] = contextRef.getString(R.string.next_mileage_not_valid, nextRefill.mileage.toString())//"The mileage value is not valid (should be lower than the next one - ${nextRefill.mileage}km)"

                                    if (refill.currentfuelamount + refill.amount / refill.ppl < nextRefill.currentfuelamount)
                                        errorMap[CURRENT_FUEL_AMOUNT_FIELD] = contextRef.getString(R.string.next_current_fuel_amount_not_valid, nextRefill.currentfuelamount.toString())// "The current fuel amount is not valid (should be greater than ${nextRefill.currentfuelamount}L)"
                                }

                                // If there are errors, then do not save the refill. Instead, throw a RefillException
                                if (errorMap.isEmpty()) {

                                    dbRef
                                        .collection(USERS_COLLECTION)
                                        .document(username)
                                        .collection(CARS_COLLECTION)
                                        .document(plate)
                                        .collection(REFILLS_COLLECTION)
                                        .document(refill.date.toString())
                                        .set(refill)
                                        .addOnSuccessListener {

                                            taskSource.setResult(true)
                                        }
                                        .addOnFailureListener { e ->

                                            taskSource.setException(RefillException(e.message?:""))
                                        }
                                }

                                else {

                                    taskSource.setException(RefillException(
                                        if (errorMap["message"] != null)                 errorMap["message"]!! else contextRef.getString(R.string.refill_failed),
                                        if (errorMap[POSITION_FIELD] != null)            errorMap[POSITION_FIELD]!! else "",
                                        if (errorMap[PRICE_PER_LITER_FIELD] != null)     errorMap[PRICE_PER_LITER_FIELD]!! else "",
                                        if (errorMap[MILEAGE_FIELD] != null)             errorMap[MILEAGE_FIELD]!! else "",
                                        if (errorMap[AMOUNT_FIELD] != null)              errorMap[AMOUNT_FIELD]!! else "",
                                        if (errorMap[CURRENT_FUEL_AMOUNT_FIELD] != null) errorMap[CURRENT_FUEL_AMOUNT_FIELD]!! else ""))
                                }
                            }

                            else {

                                taskSource.setException(RefillException(it.exception!!.message?:""))
                            }
                        }
                }

                else {

                    taskSource.setException(RefillException("The user does not have the specified car"))
                }
            }

        return taskSource.task
    }

    /**
     * Gets the refill data for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param from The start date (Date.from(Instant.EPOCH) by default to get the oldest date)
     * @param to The end date (Date.from(Instant.EPOCH) by default to get the latest date)
     * @throws RefillException
     */
    public fun getRefillData(username: String, plate: String, from: Date = Date.from(Instant.EPOCH), to: Date = Date.from(Instant.EPOCH)) : Task<ArrayList<Refill>> {

        val taskSource = TaskCompletionSource<ArrayList<Refill>>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(REFILLS_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { queryRefill ->

                            var last: Date? = null
                            for (document in queryRefill.documents) {

                                val refill = document.toObject(Refill::class.java)
                                if (refill != null)
                                    last = refill.date.toDate()
                                else
                                    taskSource.setException(RefillException("Last refill date is null (database error)"))
                            }

                            if (last == null)
                                taskSource.setResult(ArrayList())

                            else {

                                if (to != Date.from(Instant.EPOCH))
                                    last = to

                                dbRef
                                    .collection(USERS_COLLECTION)
                                    .document(username)
                                    .collection(CARS_COLLECTION)
                                    .document(plate)
                                    .collection(REFILLS_COLLECTION)
                                    .orderBy(DATE_FIELD, Query.Direction.ASCENDING)
                                    .whereGreaterThanOrEqualTo(DATE_FIELD, Timestamp(from))
                                    .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(last))
                                    .get()
                                    .addOnSuccessListener { query ->

                                        val list = ArrayList<Refill>()
                                        for (document in query.documents) {

                                            val refill = document.toObject(Refill::class.java)
                                            if (refill != null)
                                                list.add(refill)
                                        }

                                        taskSource.setResult(list)
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(RefillException(e.message?:""))
                                    }
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(RefillException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(RefillException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(RefillException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Sets a new date for the car maintenance for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param date The maintenance date (null to remove it)
     * @throws MaintenanceException
     */
    public fun setNewMaintenanceDate(username: String, plate: String, date: Timestamp? = Timestamp.now()): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .update(MAINTENANCE_DATE_FIELD, date)
                        .addOnSuccessListener {

                            taskSource.setResult(true)
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(MaintenanceException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(MaintenanceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(MaintenanceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * To pay for the car maintenance for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param maintenance The actual maintenance data
     * @throws MaintenanceException
     */
    public fun payMaintenance(username: String, plate: String, maintenance: Maintenance): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(MAINTENANCE_COLLECTION)
                        .document(maintenance.date.toString())
                        .set(maintenance)
                        .addOnSuccessListener {

                            dbRef
                                .collection(USERS_COLLECTION)
                                .document(username)
                                .collection(CARS_COLLECTION)
                                .document(plate)
                                .update(MAINTENANCE_DATE_FIELD, null)
                                .addOnSuccessListener {

                                    taskSource.setResult(true)
                                }
                                .addOnFailureListener { e ->

                                    taskSource.setException(MaintenanceException(e.message?:""))
                                }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(MaintenanceException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(MaintenanceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(MaintenanceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the maintenance data for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param from The start date (Date.from(Instant.EPOCH) by default to get the oldest date)
     * @param to The end date (Date.from(Instant.EPOCH) by default to get the latest date)
     * @throws MaintenanceException
     */
    public fun getMaintenanceData(username: String, plate: String, from: Date = Date.from(Instant.EPOCH), to: Date = Date.from(Instant.EPOCH)) : Task<ArrayList<Maintenance>> {

        val taskSource = TaskCompletionSource<ArrayList<Maintenance>>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(MAINTENANCE_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { queryMaintenance ->

                            var last: Date? = null
                            for (document in queryMaintenance.documents) {

                                val maintenance = document.toObject(Maintenance::class.java)
                                if (maintenance != null)
                                    last = maintenance.date.toDate()
                                else
                                    taskSource.setException(MaintenanceException("Last maintenance date is null (database error)"))
                            }

                            if (last == null)
                                taskSource.setResult(ArrayList())

                            else {

                                if (to != Date.from(Instant.EPOCH))
                                    last = to

                                dbRef
                                    .collection(USERS_COLLECTION)
                                    .document(username)
                                    .collection(CARS_COLLECTION)
                                    .document(plate)
                                    .collection(MAINTENANCE_COLLECTION)
                                    .orderBy(DATE_FIELD)
                                    .whereGreaterThanOrEqualTo(DATE_FIELD, Timestamp(from))
                                    .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(last))
                                    .get()
                                    .addOnSuccessListener { query ->

                                        val list = ArrayList<Maintenance>()
                                        for (document in query.documents) {

                                            val maintenance = document.toObject(Maintenance::class.java)
                                            if (maintenance != null)
                                                list.add(maintenance)
                                        }

                                        taskSource.setResult(list)
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(MaintenanceException(e.message?:""))
                                    }
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(RefillException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(MaintenanceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(MaintenanceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Sets a new date for the car insurance for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param date The insurance date (null to remove it)
     * @throws InsuranceException
     */
    public fun setNewInsuranceDate(username: String, plate: String, date: Timestamp? = Timestamp.now()): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .update(INSURANCE_DATE_FIELD, date)
                        .addOnSuccessListener {

                            taskSource.setResult(true)
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(InsuranceException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(InsuranceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(InsuranceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * To pay for the car insurance for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param insurance The actual insurance data
     * @throws InsuranceException
     */
    public fun payInsurance(username: String, plate: String, insurance: Insurance): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(INSURANCE_COLLECTION)
                        .document(insurance.date.toString())
                        .set(insurance)
                        .addOnSuccessListener {

                            dbRef
                                .collection(USERS_COLLECTION)
                                .document(username)
                                .collection(CARS_COLLECTION)
                                .document(plate)
                                .update(INSURANCE_DATE_FIELD, null)
                                .addOnSuccessListener {

                                    taskSource.setResult(true)
                                }
                                .addOnFailureListener { e ->

                                    taskSource.setException(InsuranceException(e.message?:""))
                                }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(InsuranceException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(InsuranceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(InsuranceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the insurance data for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param from The start date (Date.from(Instant.EPOCH) by default to get the oldest date)
     * @param to The start date (Date.from(Instant.EPOCH) by default to get the latest date)
     * @throws InsuranceException
     */
    public fun getInsuranceData(username: String, plate: String, from: Date = Date.from(Instant.EPOCH), to: Date = Date.from(Instant.EPOCH)) : Task<ArrayList<Insurance>> {

        val taskSource = TaskCompletionSource<ArrayList<Insurance>>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(INSURANCE_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { queryInsurance ->

                            var last: Date? = null
                            for (document in queryInsurance.documents) {

                                val insurance = document.toObject(Insurance::class.java)
                                if (insurance != null)
                                    last = insurance.date.toDate()
                                else
                                    taskSource.setException(InsuranceException("Last insurance date is null (database error)"))
                            }

                            if (last == null)
                                taskSource.setResult(ArrayList())

                            else {

                                if (to != Date.from(Instant.EPOCH))
                                    last = to

                                dbRef
                                    .collection(USERS_COLLECTION)
                                    .document(username)
                                    .collection(CARS_COLLECTION)
                                    .document(plate)
                                    .collection(INSURANCE_COLLECTION)
                                    .orderBy(DATE_FIELD)
                                    .whereGreaterThanOrEqualTo(DATE_FIELD, Timestamp(from))
                                    .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(last))
                                    .get()
                                    .addOnSuccessListener { query ->

                                        val list = ArrayList<Insurance>()
                                        for (document in query.documents) {

                                            val insurance = document.toObject(Insurance::class.java)
                                            if (insurance != null)
                                                list.add(insurance)
                                        }

                                        taskSource.setResult(list)
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(InsuranceException(e.message?:""))
                                    }
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(InsuranceException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(InsuranceException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(InsuranceException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Sets a new date for the car tax for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param date The tax date (null to remove it)
     * @throws TaxException
     */
    public fun setNewTaxDate(username: String, plate: String, date: Timestamp? = Timestamp.now()): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .update(TAX_DATE_FIELD, date)
                        .addOnSuccessListener {

                            taskSource.setResult(true)
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(TaxException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(TaxException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(TaxException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * To pay for the car tax for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param tax The actual insurance data
     * @throws TaxException
     */
    public fun payTax(username: String, plate: String, tax: Tax): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(TAX_COLLECTION)
                        .document(tax.date.toString())
                        .set(tax)
                        .addOnSuccessListener {

                            dbRef
                                .collection(USERS_COLLECTION)
                                .document(username)
                                .collection(CARS_COLLECTION)
                                .document(plate)
                                .update(TAX_DATE_FIELD, null)
                                .addOnSuccessListener {

                                    taskSource.setResult(true)
                                }
                                .addOnFailureListener { e ->

                                    taskSource.setException(TaxException(e.message?:""))
                                }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(TaxException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(TaxException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(TaxException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Gets the tax data for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param from The start date (Date.from(Instant.EPOCH) by default to get the oldest date)
     * @param to The start date (Date.from(Instant.EPOCH) by default to get the latest date)
     * @throws TaxException
     */
    public fun getTaxData(username: String, plate: String, from: Date = Date.from(Instant.EPOCH), to: Date = Date.from(Instant.EPOCH)) : Task<ArrayList<Tax>> {

        val taskSource = TaskCompletionSource<ArrayList<Tax>>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .collection(TAX_COLLECTION)
                        .orderBy(DATE_FIELD, Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .addOnSuccessListener { queryInsurance ->

                            var last: Date? = null
                            for (document in queryInsurance.documents) {

                                val tax = document.toObject(Tax::class.java)
                                if (tax != null)
                                    last = tax.date.toDate()
                                else
                                    taskSource.setException(TaxException("Last tax date is null (database error)"))
                            }

                            if (last == null)
                                taskSource.setResult(ArrayList())

                            else {

                                if (to != Date.from(Instant.EPOCH))
                                    last = to

                                dbRef
                                    .collection(USERS_COLLECTION)
                                    .document(username)
                                    .collection(CARS_COLLECTION)
                                    .document(plate)
                                    .collection(TAX_COLLECTION)
                                    .orderBy(DATE_FIELD)
                                    .whereGreaterThanOrEqualTo(DATE_FIELD, Timestamp(from))
                                    .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(last))
                                    .get()
                                    .addOnSuccessListener { query ->

                                        val list = ArrayList<Tax>()
                                        for (document in query.documents) {

                                            val tax = document.toObject(Tax::class.java)
                                            if (tax != null)
                                                list.add(tax)
                                        }

                                        taskSource.setResult(list)
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(TaxException(e.message?:""))
                                    }
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(TaxException(e.message?:""))
                        }
                }

                else {

                    taskSource.setException(TaxException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(TaxException(e.message?:""))
            }

        return taskSource.task
    }

    /**
     * Hashes a string with Secure Hashing Algorithm with 256 characters
     */
    private fun sha256(input: String): String {

        val bytes = input.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Checks if the user is present
     *
     * @param username The user's id
     */
    private fun isUserPresent(username: String): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        dbRef
            .collection(USERS_COLLECTION)
            .document(username)
            .get()
            .addOnSuccessListener { document ->

                taskSource.setResult(document.exists())
            }
            .addOnFailureListener { e ->

                taskSource.setException(e)
            }

        return taskSource.task
    }

    /**
     * Checks if the car is present
     *
     * @param username The car owner's id
     * @param plate The car's plate
     */
    private fun isCarPresent(username: String, plate: String): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        isUserPresent(username)
            .addOnSuccessListener { userPresent ->

                if (userPresent) {

                    dbRef
                        .collection(USERS_COLLECTION)
                        .document(username)
                        .collection(CARS_COLLECTION)
                        .document(plate)
                        .get()
                        .addOnSuccessListener { document ->

                            taskSource.setResult(document.exists())
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(e)
                        }
                }

                else {

                    taskSource.setException(Exception("The user does not exist"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(e)
            }

        return taskSource.task
    }

    /**
     * Checks if the car parameters are valid
     *
     * @param car The desired car
     */
    private fun checkNewCarParameters(car: Car): HashMap<String, String> {

        val errorMap = HashMap<String, String>()

        // Check plate
        if (car.plate.isEmpty())
            errorMap[PLATE_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check name
        if (car.name.isEmpty())
            errorMap[NAME_FIELD] = contextRef.getString(R.string.this_field_must_not_be_empty)

        // Check maintenance date
        if (car.maintenancedate != null && car.maintenancedate!! < Timestamp.now())
            errorMap[MAINTENANCE_DATE_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check insurance date
        if (car.insurancedate != null && car.insurancedate!! < Timestamp.now())
            errorMap[INSURANCE_DATE_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        // Check tax date
        if (car.taxdate != null && car.taxdate!! < Timestamp.now())
            errorMap[TAX_DATE_FIELD] = contextRef.getString(R.string.the_field_must_be_valid)

        return errorMap
    }

    /**
     * Deletes a subcollection
     *
     * @param subcollection The collection reference
     */
    private fun deleteSubcollection(subcollection: CollectionReference): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()

        subcollection
            .get()
            .addOnSuccessListener { query ->

                if (!query.isEmpty) {

                    for (doc in query.documents) {

                        doc.reference.delete()
                    }
                }

                taskSource.setResult(true)
            }
            .addOnFailureListener { e ->

                taskSource.setException(e)
            }

        return taskSource.task
    }

    /**
     * The firestore database reference
     */
    private val dbRef: FirebaseFirestore = Firebase.firestore

    private lateinit var contextRef: Context
}
