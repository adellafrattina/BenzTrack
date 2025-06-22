package it.uninsubria.benztrack

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.tasks.Task

object Handler {

    public fun init(context: Context): Task<User>? {

        pref = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        val username = pref.getString("username", null)
        val password = pref.getString("password", null)

        if (username != null && password != null)
            return database.login(username, password)

        return null
    }

    public fun save() {

        with(pref.edit()) {

            putString("username", if (loggedUser == null) null else loggedUser!!.username)
            putString("password", if (loggedUser == null) null else loggedUser!!.password)
            apply()
        }
    }

    /**
     * Global database instance for the application
     */
    public val database = Database()

    /**
     * Global variable to store the currently logged-in user.
     * Null when no user is logged in.
     */
    public var loggedUser: User? = null

    /**
     * Shared preferences
     */
    private lateinit var pref: SharedPreferences
}
