package it.uninsubria.benztrack

/**
 * When the login procedure fails
 *
 * @author adellafrattina
 */
public class LoginException(

    /**
     * Generic message
     */
    message: String
) : Exception(message)

/**
 * When the registration procedure fails
 *
 * @author adellafrattina
 */
public class RegistrationException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the username. If empty, then there are no errors
     */
    val username: String,

    /**
     * Error with the password. If empty, then there are no errors
     */
    val password: String,

    /**
     * Error with the email. If empty, then there are no errors
     */
    val email: String,

    /**
     * Error with the name. If empty, then there are no errors
     */
    val name: String,

    /**
     * Error with the surname. If empty, then there are no errors
     */
    val surname: String
) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "")
}

/**
 * When the car model creation procedure fails
 *
 * @author adellafrattina
 */
public class CarModelException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the name. If empty, then there are no errors
     */
    val name: String,

    /**
     * Error with the year. If empty, then there are no errors
     */
    val year: String,

    /**
     * Error with the capacity. If empty, then there are no errors
     */
    val capacity: String,

    /**
     * Error with the fuel type. If empty, then there are no errors
     */
    val fuel: String,

    /**
     * Error with the CO2 factor. If empty, then there are no errors
     */
    val co2factor: String,

    /**
     * Error with the weight. If empty, then there are no errors
     */
    val weight: String,

    /**
     * Error with the width. If empty, then there are no errors
     */
    val width: String,

    /**
     * Error with the length. If empty, then there are no errors
     */
    val length: String,

    /**
     * Error with the height. If empty, then there are no errors
     */
    val height: String
) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "", "", "", "", "")
}
