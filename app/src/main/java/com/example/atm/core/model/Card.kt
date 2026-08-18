package com.example.atm.core.model

import com.example.atm.core.exceptions.CardBlockedError
import com.example.atm.core.exceptions.InvalidPINError

/**
 * Class: Card
 * Encapsulates card credentials, PIN verification, and max 3 failed PIN attempts logic.
 */
class Card(
    val cardNumber: String,
    val customerId: String,
    val primaryAccountNumber: String,
    private var pin: String,
    private var status: CardStatus = CardStatus.ACTIVE,
    private var failedPinAttempts: Int = 0
) {

    fun getStatus(): CardStatus = status

    fun getFailedAttempts(): Int = failedPinAttempts

    fun isBlocked(): Boolean = status == CardStatus.BLOCKED

    /**
     * Validates entered PIN against card PIN.
     * Tracks failed attempts and blocks card after 3 consecutive failures.
     */
    fun validatePin(enteredPin: String): Boolean {
        if (status == CardStatus.BLOCKED) {
            throw CardBlockedError("Card $cardNumber is BLOCKED. Cannot authenticate.")
        }

        if (this.pin == enteredPin) {
            failedPinAttempts = 0
            return true
        } else {
            failedPinAttempts++
            val remaining = MAX_PIN_ATTEMPTS - failedPinAttempts
            if (failedPinAttempts >= MAX_PIN_ATTEMPTS) {
                status = CardStatus.BLOCKED
                throw InvalidPINError(0, "PIN incorrect. Card has been BLOCKED due to $MAX_PIN_ATTEMPTS failed attempts.")
            } else {
                throw InvalidPINError(remaining, "Invalid PIN. Remaining attempts: $remaining")
            }
        }
    }

    /**
     * Updates card PIN after checking existing PIN.
     */
    fun changePin(oldPin: String, newPin: String) {
        if (status == CardStatus.BLOCKED) {
            throw CardBlockedError("Cannot change PIN on a BLOCKED card.")
        }
        if (this.pin != oldPin) {
            throw InvalidPINError(0, "Current PIN does not match.")
        }
        Account.validateNewPinFormat(newPin)
        this.pin = newPin
        this.failedPinAttempts = 0
    }

    fun blockCard() {
        this.status = CardStatus.BLOCKED
    }

    fun unblockCard(newPin: String? = null) {
        this.status = CardStatus.ACTIVE
        this.failedPinAttempts = 0
        if (newPin != null) {
            this.pin = newPin
        }
    }

    // Helper for direct hydration
    fun setEncapsulatedState(newStatus: CardStatus, attempts: Int) {
        this.status = newStatus
        this.failedPinAttempts = attempts
    }

    fun getRawPinForStorage(): String = pin

    companion object {
        const val MAX_PIN_ATTEMPTS = 3
    }
}
