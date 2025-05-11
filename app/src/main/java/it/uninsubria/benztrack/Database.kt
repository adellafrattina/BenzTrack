package it.uninsubria.benztrack

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

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
        public const val LENGTH_FIELD = "length"
        public const val HEIGHT_FIELD = "height"

        public const val PLATE_FIELD = "plate"
        public const val MODEL_FIELD = "model"

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

        dbRef
            .collection(USERS_COLLECTION)
            .document(username)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val storedPassword = document.getString(PASSWORD_FIELD)
                    if (storedPassword == password.hashCode().toString()) {

                        val user = document.toObject(User::class.java)
                        if (user != null)
                            taskSource.setResult(user)
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
                            user.password = user.password.hashCode().toString()

                            dbRef
                                .collection(USERS_COLLECTION)
                                .document(user.username)
                                .set(user)
                                .addOnSuccessListener {

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
     * The firestore database reference
     */
    private val dbRef: FirebaseFirestore = Firebase.firestore
}
