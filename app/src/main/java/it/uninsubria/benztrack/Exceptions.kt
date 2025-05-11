package it.uninsubria.benztrack

public class LoginException(

    message: String
) : Exception(message)

public class RegistrationException(

    message: String,
    val username: String,
    val password: String,
    val email: String,
    val name: String,
    val surname: String
) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "")
}
