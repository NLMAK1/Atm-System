package com.example.atm.core.model

import com.example.atm.core.exceptions.AccountInactiveError
import com.example.atm.core.exceptions.DailyLimitExceededError
import com.example.atm.core.exceptions.InsufficientBalanceError
import com.example.atm.core.exceptions.InvalidAmountError
import com.example.atm.core.exceptions.InvalidPINError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Abstract Base Class: Account
 * Demonstrates Abstraction, Encapsulation, and Polymorphism base structure.
 */
abstract class Account(
    val accountNumber: String,
    val accountHolderName: String,
    val customerId: String,
    protected var _balance: Double,
    protected var _pin: String,
    protected var _status: AccountStatus = AccountStatus.ACTIVE,
    protected var _dailyWithdrawnAmount: Double = 0.0,
    protected var _dailyTransferAmount: Double = 0.0,
    protected var _lastResetDate: String = getCurrentDateString()
) {

    // === ENCAPSULATION GETTERS ===

    fun getBalance(): Double = _balance

    fun getStatus(): AccountStatus = _status

    fun getDailyWithdrawnAmount(): Double {
        checkAndResetDailyLimits()
        return _dailyWithdrawnAmount
    }

    fun getDailyTransferAmount(): Double {
        checkAndResetDailyLimits()
        return _dailyTransferAmount
    }

    fun getLastResetDate(): String = _lastResetDate

    fun isBlocked(): Boolean = _status == AccountStatus.BLOCKED || _status == AccountStatus.CLOSED

    fun isActive(): Boolean = _status == AccountStatus.ACTIVE

    // === ABSTRACT POLYMORPHIC METHODS ===

    abstract fun getAccountType(): AccountType

    abstract fun calculateWithdrawalLimit(): Double

    abstract fun calculateTransactionFee(type: TransactionType, amount: Double): Double

    abstract fun canWithdraw(amount: Double): Pair<Boolean, String?>

    abstract fun getMinimumBalance(): Double

    abstract fun getOverdraftLimit(): Double

    abstract fun getDailyWithdrawalLimit(): Double

    abstract fun getDailyTransferLimit(): Double

    // === ENCAPSULATED BUSINESS METHODS ===

    /**
     * Verifies PIN against the account PIN.
     */
    fun validatePin(enteredPin: String): Boolean {
        return _pin == enteredPin
    }

    /**
     * Controlled PIN change with validation rules.
     */
    fun changePin(oldPin: String, newPin: String) {
        if (_pin != oldPin) {
            throw InvalidPINError(0, "Current PIN does not match.")
        }
        validateNewPinFormat(newPin)
        this._pin = newPin
    }

    /**
     * Sets status with encapsulation.
     */
    fun setStatus(newStatus: AccountStatus) {
        this._status = newStatus
    }

    /**
     * Resets daily limits if date has changed.
     */
    fun checkAndResetDailyLimits() {
        val today = getCurrentDateString()
        if (today != _lastResetDate) {
            _dailyWithdrawnAmount = 0.0
            _dailyTransferAmount = 0.0
            _lastResetDate = today
        }
    }

    /**
     * Controlled deposit method.
     */
    fun deposit(amount: Double): Double {
        if (!isActive()) {
            throw AccountInactiveError(accountNumber)
        }
        if (amount <= 0) {
            throw InvalidAmountError("Deposit amount must be positive. Provided: Rs. $amount")
        }

        _balance += amount
        return _balance
    }

    /**
     * Controlled withdrawal method applying account type rules and fees.
     */
    fun withdraw(amount: Double): Double {
        if (!isActive()) {
            throw AccountInactiveError(accountNumber)
        }
        if (amount <= 0) {
            throw InvalidAmountError("Withdrawal amount must be positive. Provided: Rs. $amount")
        }

        checkAndResetDailyLimits()

        val fee = calculateTransactionFee(TransactionType.WITHDRAWAL, amount)
        val totalDeduction = amount + fee

        // Check single transaction limit
        val singleLimit = calculateWithdrawalLimit()
        if (amount > singleLimit) {
            throw InvalidAmountError(
                "Requested amount Rs. ${"%,.2f".format(amount)} exceeds maximum per transaction limit of Rs. ${"%,.2f".format(singleLimit)}"
            )
        }

        // Check daily limit
        val dailyLimit = getDailyWithdrawalLimit()
        if (_dailyWithdrawnAmount + amount > dailyLimit) {
            throw DailyLimitExceededError(dailyLimit, _dailyWithdrawnAmount + amount)
        }

        // Check account specific withdrawal allowance (minimum balance or overdraft)
        val (canWithdrawAllowed, reason) = canWithdraw(totalDeduction)
        if (!canWithdrawAllowed) {
            throw InsufficientBalanceError(
                currentBalance = _balance,
                requestedAmount = amount,
                requiredTotal = totalDeduction,
                message = reason ?: "Insufficient balance for withdrawal"
            )
        }

        _balance -= totalDeduction
        _dailyWithdrawnAmount += amount
        return _balance
    }

    /**
     * Controlled debit for inter-account transfer.
     */
    fun debitTransfer(amount: Double): Double {
        if (!isActive()) {
            throw AccountInactiveError(accountNumber)
        }
        if (amount <= 0) {
            throw InvalidAmountError("Transfer amount must be positive.")
        }

        checkAndResetDailyLimits()

        val fee = calculateTransactionFee(TransactionType.TRANSFER_DEBIT, amount)
        val totalDeduction = amount + fee

        val dailyLimit = getDailyTransferLimit()
        if (_dailyTransferAmount + amount > dailyLimit) {
            throw DailyLimitExceededError(dailyLimit, _dailyTransferAmount + amount)
        }

        val (canWithdrawAllowed, reason) = canWithdraw(totalDeduction)
        if (!canWithdrawAllowed) {
            throw InsufficientBalanceError(
                currentBalance = _balance,
                requestedAmount = amount,
                requiredTotal = totalDeduction,
                message = reason ?: "Insufficient balance for transfer"
            )
        }

        _balance -= totalDeduction
        _dailyTransferAmount += amount
        return _balance
    }

    /**
     * Controlled credit for inter-account transfer.
     */
    fun creditTransfer(amount: Double): Double {
        if (!isActive()) {
            throw AccountInactiveError(accountNumber)
        }
        if (amount <= 0) {
            throw InvalidAmountError("Transfer credit amount must be positive.")
        }

        _balance += amount
        return _balance
    }

    // Helper for direct internal state hydration from DB
    fun setEncapsulatedState(
        newBalance: Double,
        dailyWithdrawn: Double,
        dailyTransfer: Double,
        resetDate: String,
        newStatus: AccountStatus
    ) {
        this._balance = newBalance
        this._dailyWithdrawnAmount = dailyWithdrawn
        this._dailyTransferAmount = dailyTransfer
        this._lastResetDate = resetDate
        this._status = newStatus
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            return sdf.format(Date())
        }

        fun validateNewPinFormat(newPin: String) {
            if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                throw InvalidAmountError("PIN must be exactly 4 digits.")
            }
            if (newPin in listOf("0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999", "1234", "4321")) {
                throw InvalidAmountError("PIN is too simple/insecure. Choose a stronger 4-digit PIN.")
            }
        }
    }
}
