package it.uninsubria.benztrack

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
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
        public const val SEARCH_TERMS_FIELD = "searchterms"

        public const val PLATE_FIELD = "plate"
        public const val MODEL_FIELD = "model"
        public const val MAINTENANCE_DATE_FILED = "maintenancedate"
        public const val INSURANCE_DATE_FILED = "insurance"
        public const val TAX_DATE_FILED = "taxdate"

        public const val DATE_FIELD = "date"
        public const val POSITION_FIELD = "position"
        public const val PRICE_PER_LITER_FIELD = "ppl"
        public const val MILEAGE_FIELD = "mileage"
        public const val AMOUNT_FIELD = "amount"
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
            usernameErrorMsg = "This field must not be empty"

        // Check password
        if (password.isEmpty())
            passwordErrorMsg = "This field must not be empty"

        if (username.isEmpty() || password.isEmpty())
            taskSource.setException(LoginException(
                "Login failed",
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
                            taskSource.setException(LoginException("Username or password are incorrect"))
                    }

                    else
                        taskSource.setException(LoginException("Username or password are incorrect"))
                }
                .addOnFailureListener { e ->

                    taskSource.setException(e as LoginException)
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
            errorMap[PASSWORD_FIELD] = "This field must not be empty"

        // Check email
        if (user.email.isEmpty())
            errorMap[EMAIL_FIELD] = "This field must not be empty"
        else if (!Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\$").matches(user.email))
            errorMap[EMAIL_FIELD] = "The email is not valid"

        // Check name
        if (user.name.isEmpty())
            errorMap[NAME_FIELD] = "This field must not be empty"

        // Check surname
        if (user.surname.isEmpty())
            errorMap[SURNAME_FIELD] = "This field must not be empty"

        // Check username
        if (user.username.isEmpty()) {

            errorMap[USERNAME_FIELD] = "This field must not be empty"
            taskSource.setException(RegistrationException(
                "Registration has failed",
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

                        errorMap[USERNAME_FIELD] = "Username already taken"
                        taskSource.setException(RegistrationException(
                            "Registration has failed",
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

                                    taskSource.setException(e as RegistrationException)
                                }
                        }

                        else {

                            taskSource.setException(RegistrationException(
                                "Registration has failed",
                                if (errorMap[USERNAME_FIELD] != null) errorMap[USERNAME_FIELD]!! else "",
                                if (errorMap[PASSWORD_FIELD] != null) errorMap[PASSWORD_FIELD]!! else "",
                                if (errorMap[EMAIL_FIELD] != null)    errorMap[EMAIL_FIELD]!! else "",
                                if (errorMap[NAME_FIELD] != null)     errorMap[NAME_FIELD]!! else "",
                                if (errorMap[SURNAME_FIELD] != null)  errorMap[SURNAME_FIELD]!! else ""))
                        }
                    }
                }
                .addOnFailureListener { e ->

                    taskSource.setException(e as RegistrationException)
                }
        }

        return taskSource.task
    }

    /**
     * Creates a new car model. It will check if the model parameters are valid. Warning: once created, it can't be removed
     *
     * @param model The car model
     */
    public fun createCarModel(model: CarModel): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()
        val errorMap = HashMap<String, String>()

        // Check name
        if (model.name.isEmpty())
            errorMap[NAME_FIELD] = "This field must not be empty"

        // Check year
        if (model.year < 1900 || model.year > LocalDate.now().year)
            errorMap[YEAR_FIELD] = "The year must be valid"

        // Check car
        if (model.capacity < 49) // Smallest possible car (From what I've researched)
            errorMap[CAPACITY_FIELD] = "The capacity must be valid"

        // Check CO2 factor
        if (model.co2factor < 0)
            errorMap[CO2_FACTOR_FIELD] = "The CO2 factor must be valid"

        // Check weight
        if (model.weight < 50) // Lightest possible car (From what I've researched)
            errorMap[WEIGHT_FIELD] = "The weight must be valid"

        // Check weight
        if (model.width < 1) // Smallest possible car (From what I've researched)
            errorMap[WIDTH_FIELD] = "The width must be valid"

        // Check length
        if (model.length < 2) // Shortest possible car (From what I've researched)
            errorMap[LENGTH_FIELD] = "The length must be valid"

        // Check height
        if (model.height < 1) // Shortest possible car (From what I've researched)
            errorMap[HEIGHT_FIELD] = "The height must be valid"

        if (errorMap.isEmpty()) {

            dbRef
                .collection(MODELS_COLLECTION)
                .document(sha256(model.toString()))
                .get()
                .addOnSuccessListener { document ->

                    // The car model already exists
                    if (document.exists()) {

                        errorMap["message"] = "This car model already exists"
                        taskSource.setException(CarModelException(message=errorMap["message"]!!))
                    }

                    else {

                        // Add search terms
                        val terms = model.name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
                        for (term in terms)
                            model.searchterms.add(term.uppercase())

                        dbRef
                            .collection(MODELS_COLLECTION)
                            .document(sha256(model.toString()))
                            .set(model)
                            .addOnSuccessListener {

                                taskSource.setResult(true)
                            }
                            .addOnFailureListener { e ->

                                taskSource.setException(e as CarModelException)
                            }
                    }
                }
                .addOnFailureListener { e ->

                    taskSource.setException(e as CarModelException)
                }
        }

        else {

            errorMap["message"] = "This car model cannot exist"
            taskSource.setException(CarModelException(
                if (errorMap["message"] != null)        errorMap["message"]!! else "" ,
                if (errorMap[NAME_FIELD] != null)       errorMap[NAME_FIELD]!! else "",
                if (errorMap[YEAR_FIELD] != null)       errorMap[YEAR_FIELD]!! else "",
                if (errorMap[CAPACITY_FIELD] != null)   errorMap[CAPACITY_FIELD]!! else "",
                if (errorMap[FUEL_FIELD] != null)       errorMap[FUEL_FIELD]!! else "",
                if (errorMap[CO2_FACTOR_FIELD] != null) errorMap[CO2_FACTOR_FIELD]!! else "",
                if (errorMap[WEIGHT_FIELD] != null)     errorMap[WEIGHT_FIELD]!! else "",
                if (errorMap[WIDTH_FIELD] != null)      errorMap[WIDTH_FIELD]!! else "",
                if (errorMap[LENGTH_FIELD] != null)     errorMap[LENGTH_FIELD]!! else "",
                if (errorMap[HEIGHT_FIELD] != null)     errorMap[HEIGHT_FIELD]!! else ""))
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
        val models = ArrayList<CarModel>()

        if (name.trim().isEmpty()) {

            taskSource.setException(Exception("Cannot find the specified model"))

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
                        .whereGreaterThanOrEqualTo(NAME_FIELD, name.trim().uppercase())
                        .whereLessThan(NAME_FIELD, name + '\uf8ff')
                        .get()
                        .addOnSuccessListener { secondaryQuery ->

                            if (!secondaryQuery.isEmpty) {

                                for (document in secondaryQuery) {

                                    val model = document.toObject(CarModel::class.java)
                                    models.add(model)
                                }

                                taskSource.setResult(models)
                            }

                            else {

                                taskSource.setException(Exception("Cannot find the specified model"))
                            }
                        }
                        .addOnFailureListener { e ->

                            taskSource.setException(e)
                        }
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(e)
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
                    taskSource.setException(Exception("Cannot find the specified model"))
            }
            .addOnFailureListener { e ->

                taskSource.setException(e)
            }

        return taskSource.task
    }

    /**
     * Adds a new car to the user's car collection
     *
     * @param username The user that desires to monitor a new car
     * @param car The actual car. It needs to be valid
     */
    public fun addNewCar(username: String, car: Car): Task<Boolean> {

        val taskSource = TaskCompletionSource<Boolean>()
        lateinit var errorMap: HashMap<String, String>

        isCarPresent(username, car.plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    taskSource.setException(CarException("A car with the same plate is already present"))
                }

                else {

                    errorMap = checkNewCarParameters(car)

                    if (errorMap.isEmpty()) {

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

                                taskSource.setException(e as CarException)
                            }
                    }

                    else {

                        taskSource.setException(CarException(
                            "Car creation has failed",
                            if (errorMap[PLATE_FIELD] != null)            errorMap[PLATE_FIELD]!! else "",
                            if (errorMap[NAME_FIELD] != null)             errorMap[NAME_FIELD]!! else "",
                            if (errorMap[MAINTENANCE_DATE_FILED] != null) errorMap[MAINTENANCE_DATE_FILED]!! else "",
                            if (errorMap[INSURANCE_DATE_FILED] != null)   errorMap[INSURANCE_DATE_FILED]!! else "",
                            if (errorMap[TAX_DATE_FILED] != null)         errorMap[TAX_DATE_FILED]!! else ""
                        ))
                    }
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(e as CarException)
            }

        return taskSource.task
    }

    /**
     * Deletes a car from the user's car collection
     *
     * @param username The car owner's id
     * @param plate The car's plate
     */
    public fun deleteCar(username: String, plate: String): Task<Boolean> {

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

                            taskSource.setException(e as CarException)
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

                            taskSource.setException(e as CarException)
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

                            taskSource.setException(e as CarException)
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

                            taskSource.setException(e as CarException)
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

                            taskSource.setException(e as CarException)
                        }
                }

                else {

                    taskSource.setException(CarException("The user does not have the specified car"))
                }
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

        isUserPresent(username)
            .addOnSuccessListener { userPresent ->

                if (userPresent) {

                    isCarPresent(username, plate)
                        .addOnSuccessListener { carPresent ->

                            if (carPresent) {

                                dbRef
                                    .collection(USERS_COLLECTION)
                                    .document(username)
                                    .collection(CARS_COLLECTION)
                                    .document(plate)
                                    .collection(REFILLS_COLLECTION)
                                    .orderBy(DATE_FIELD)
                                    .limit(1)
                                    .get()
                                    .addOnSuccessListener { query ->

                                        // Check that the mileage is greater than the last one (if there is one)
                                        for (document in query.documents) {

                                            val mileage = document.get(MILEAGE_FIELD) as Int
                                            if (mileage > refill.mileage)
                                                errorMap[MILEAGE_FIELD] = "The mileage value is not valid"
                                        }

                                        // Check position
                                        if (refill.position.isEmpty())
                                            errorMap[POSITION_FIELD] = "This value must not be empty"

                                        // Check position
                                        if (refill.ppl < 0)
                                            errorMap[POSITION_FIELD] = "The price per liter cannot be negative"

                                        // Check amount
                                        if (refill.amount < 0)
                                            errorMap[POSITION_FIELD] = "The amount cannot be negative"

                                        // Check consistency
                                        if (refill.amount != 0.0f && refill.ppl == 0.0f)
                                            errorMap[AMOUNT_FIELD] = "The value is not consistent with the price per liter"

                                        if (errorMap.isEmpty()) {

                                            val timestamp = Timestamp.now()
                                            refill.date = timestamp
                                            dbRef
                                                .collection(USERS_COLLECTION)
                                                .document(username)
                                                .collection(CARS_COLLECTION)
                                                .document(plate)
                                                .collection(REFILLS_COLLECTION)
                                                .document(timestamp.toString())
                                                .set(refill)
                                                .addOnSuccessListener {

                                                    taskSource.setResult(true)
                                                }
                                                .addOnFailureListener { e ->

                                                    taskSource.setException(e as RefillException)
                                                }
                                        }

                                        else {

                                            taskSource.setException(RefillException(
                                                "Failed to add new refill",
                                                if (errorMap[POSITION_FIELD] != null)        errorMap[POSITION_FIELD]!! else "",
                                                if (errorMap[PRICE_PER_LITER_FIELD] != null) errorMap[PRICE_PER_LITER_FIELD]!! else "",
                                                if (errorMap[MILEAGE_FIELD] != null)         errorMap[MILEAGE_FIELD]!! else "",
                                                if (errorMap[AMOUNT_FIELD] != null)          errorMap[AMOUNT_FIELD]!! else ""))
                                        }
                                    }
                                    .addOnFailureListener { e ->

                                        taskSource.setException(e as RefillException)
                                    }
                            }

                            else {

                                taskSource.setException(RefillException("The user does not have the specified car"))
                            }
                        }
                }

                else {

                    taskSource.setException(RefillException("The user does not exist"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(e as RefillException)
            }

        return taskSource.task
    }

    /**
     * Gets the refill data for a specific car of a specific user
     *
     * @param username The user's id
     * @param plate The user's car plate
     * @param from The start date (null to get the oldest date)
     * @param to The end date (by default the current date)
     * @throws RefillException
     */
    public fun getRefillData(username: String, plate: String, from: Date? = null, to: Date = Date.from(Instant.now())) : Task<ArrayList<Refill>> {

        val taskSource = TaskCompletionSource<ArrayList<Refill>>()

        isCarPresent(username, plate)
            .addOnSuccessListener { carPresent ->

                if (carPresent) {

                    if (from != null) {

                        dbRef
                            .collection(USERS_COLLECTION)
                            .document(username)
                            .collection(CARS_COLLECTION)
                            .document(plate)
                            .collection(REFILLS_COLLECTION)
                            .orderBy(DATE_FIELD)
                            .whereGreaterThanOrEqualTo(DATE_FIELD, Timestamp(from))
                            .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(to))
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

                                taskSource.setException(e as RefillException)
                            }
                    }

                    else {

                        dbRef
                            .collection(USERS_COLLECTION)
                            .document(username)
                            .collection(CARS_COLLECTION)
                            .document(plate)
                            .collection(REFILLS_COLLECTION)
                            .orderBy(DATE_FIELD)
                            .whereLessThanOrEqualTo(DATE_FIELD, Timestamp(to))
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

                                taskSource.setException(e as RefillException)
                            }
                    }
                }

                else {

                    taskSource.setException(RefillException("The user does not have the specified car"))
                }
            }
            .addOnFailureListener { e ->

                taskSource.setException(e as RefillException)
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
            errorMap[PLATE_FIELD] = "This field must not be empty"

        // Check name
        if (car.name.isEmpty())
            errorMap[NAME_FIELD] = "This field must not be empty"

        // Check maintenance date
        if (car.maintenancedate != null && car.maintenancedate!! < Timestamp.now())
            errorMap[MAINTENANCE_DATE_FILED] = "The maintenance date must be valid"

        // Check insurance date
        if (car.insurancedate != null && car.insurancedate!! < Timestamp.now())
            errorMap[INSURANCE_DATE_FILED] = "The insurance date must be valid"

        // Check tax date
        if (car.taxdate != null && car.taxdate!! < Timestamp.now())
            errorMap[TAX_DATE_FILED] = "The tax date must be valid"

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
}
