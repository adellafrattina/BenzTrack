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
    message: String,

    /**
     * Error with the username. If empty, then there are no errors
     */
    val username: String,

    /**
     * Error with the password. If empty, then there are no errors
     */
    val password: String

) : Exception(message) {

    constructor(message: String): this(message, "", "")
}

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
    val height: String,

    /**
     * Error with the fuel capacity. If empty, then there are no errors
     */
    val fuelcapacity: String

) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "", "", "", "", "", "")
}

/**
 * When the car creation procedure fails
 *
 * @author adellafrattina
 */
public class CarException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the plate. If empty, then there are no errors
     */
    val plate: String,

    /**
     * Error with the name. If empty, then there are no errors
     */
    val name: String,

    /**
     * Error with the maintenance date. If empty, then there are no errors
     */
    val maintenancedate: String,

    /**
     * Error with the insurance date. If empty, then there are no errors
     */
    val insurancedate: String,

    /**
     * Error with the tax date. If empty, then there are no errors
     */
    val taxdate: String

) : Exception(message) {

    constructor(message: String): this(message, "", "" ,"", "", "")
}

/**
 * When the refill procedure fails
 *
 * @author adellafrattina
 */
public class RefillException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the position. If empty, then there are no errors
     */
    val position: String,

    /**
     * Error with the price per liter. If empty, then there are no errors
     */
    val ppl: String,

    /**
     * Error with the mileage. If empty, then there are no errors
     */
    val mileage: String,

    /**
     * Error with the amount. If empty, then there are no errors
      */
    val amount: String,

    /**
     * Error with the current fuel amount. If empty, then there are no errors
     */
    val currentfuelamount: String

) : Exception(message) {

    constructor(message: String): this(message, "", "", "", "", "")
}

/**
 * When the car maintenance procedure fails
 *
 * @author adellafrattina
 */
public class MaintenanceException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the amount. If empty, then there are no errors
     */
    val amount: String

) : Exception(message) {

    constructor(message: String): this(message, "")
}

/**
 * When the car insurance procedure fails
 *
 * @author adellafrattina
 */
public class InsuranceException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the amount. If empty, then there are no errors
     */
    val amount: String

) : Exception(message) {

    constructor(message: String): this(message, "")
}

/**
 * When the car tax procedure fails
 *
 * @author adellafrattina
 */
public class TaxException(

    /**
     * Generic message
     */
    message: String,

    /**
     * Error with the amount. If empty, then there are no errors
     */
    val amount: String

) : Exception(message) {

    constructor(message: String): this(message, "")
}
