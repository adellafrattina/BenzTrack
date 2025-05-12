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

public class CarModelException(

    message: String,
    val name: String,
    val year: String,
    val capacity: String,
    val fuel: String,
    val co2factor: String,
    val weight: String,
    val width: String,
    val length: String,
    val height: String
) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "", "", "", "", "")
}
