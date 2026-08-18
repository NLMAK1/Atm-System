package com.example.atm.core.exceptions

/**
 * Base custom exception for all ATM and Banking domain errors.
 */
open class ATMException(message: String) : Exception(message)

/**
 * Thrown when the entered PIN does not match the card/account PIN.
 */
class InvalidPINError(
    val remainingAttempts: Int,
    message: String = if (remainingAttempts > 0) "Invalid PIN. Remaining attempts: $remainingAttempts" else "Invalid PIN. Card has been blocked."
) : ATMException(message)

/**
 * Thrown when an operation is attempted on a blocked card.
 */
class CardBlockedError(
    message: String = "Card is BLOCKED due to security reasons. Please contact your bank."
) : ATMException(message)

/**
 * Thrown when an account does not have sufficient funds or overdraft margin.
 */
class InsufficientBalanceError(
    val currentBalance: Double,
    val requestedAmount: Double,
    val requiredTotal: Double = requestedAmount,
    message: String = "Insufficient balance. Available: Rs. ${"%,.2f".format(currentBalance)}, Requested (incl. fees): Rs. ${"%,.2f".format(requiredTotal)}"
) : ATMException(message)

/**
 * Thrown when the ATM machine cash inventory does not have enough notes or total cash.
 */
class InsufficientATMFundsError(
    val availableCash: Double,
    val requestedAmount: Double,
    message: String = "ATM has insufficient cash. Available: Rs. ${"%,.2f".format(availableCash)}, Requested: Rs. ${"%,.2f".format(requestedAmount)}"
) : ATMException(message)

/**
 * Thrown when an invalid transaction amount is specified (e.g. <= 0, non-denominational).
 */
class InvalidAmountError(
    message: String = "Invalid amount. Amount must be positive and in supported multiples."
) : ATMException(message)

/**
 * Thrown when attempting to operate on an inactive, dormant, or closed account.
 */
class AccountInactiveError(
    val accountNumber: String,
    message: String = "Account $accountNumber is inactive or blocked. Transactions are prohibited."
) : ATMException(message)

/**
 * Thrown when the daily withdrawal or transfer limit is exceeded.
 */
class DailyLimitExceededError(
    val dailyLimit: Double,
    val attemptedTotal: Double,
    message: String = "Daily limit exceeded! Daily limit: Rs. ${"%,.2f".format(dailyLimit)}, Attempted total today: Rs. ${"%,.2f".format(attemptedTotal)}"
) : ATMException(message)

/**
 * Thrown when an account number does not exist or target account is invalid.
 */
class InvalidAccountError(
    val accountNumber: String,
    message: String = "Account $accountNumber was not found or is invalid."
) : ATMException(message)

/**
 * Thrown when the ATM cannot dispense the exact requested amount using available note denominations.
 */
class DenominationUnavailableError(
    val requestedAmount: Double,
    message: String = "Cannot dispense Rs. ${"%,.2f".format(requestedAmount)} with available denominations (500, 1000, 5000)."
) : ATMException(message)
