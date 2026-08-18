package com.example.atm.core.model

/**
 * Concrete Subclass: CurrentAccount
 * Demonstrates Inheritance & Polymorphism.
 *
 * Rules:
 * - Allows overdraft limit of Rs. 50,000 (balance down to -50,000)
 * - Max withdrawal per transaction: Rs. 100,000
 * - Daily withdrawal limit: Rs. 300,000
 * - Minimum balance: 0.0
 * - Withdrawal fee: Rs. 0
 * - Transfer fee: Rs. 50
 */
class CurrentAccount(
    accountNumber: String,
    accountHolderName: String,
    customerId: String,
    balance: Double,
    pin: String,
    status: AccountStatus = AccountStatus.ACTIVE,
    dailyWithdrawnAmount: Double = 0.0,
    dailyTransferAmount: Double = 0.0,
    lastResetDate: String = getCurrentDateString()
) : Account(
    accountNumber = accountNumber,
    accountHolderName = accountHolderName,
    customerId = customerId,
    _balance = balance,
    _pin = pin,
    _status = status,
    _dailyWithdrawnAmount = dailyWithdrawnAmount,
    _dailyTransferAmount = dailyTransferAmount,
    _lastResetDate = lastResetDate
) {

    override fun getAccountType(): AccountType = AccountType.CURRENT

    override fun calculateWithdrawalLimit(): Double = 100_000.0

    override fun getDailyWithdrawalLimit(): Double = 300_000.0

    override fun getDailyTransferLimit(): Double = 500_000.0

    override fun getMinimumBalance(): Double = 0.0

    override fun getOverdraftLimit(): Double = 50_000.0

    override fun calculateTransactionFee(type: TransactionType, amount: Double): Double {
        return when (type) {
            TransactionType.WITHDRAWAL -> 0.0
            TransactionType.TRANSFER_DEBIT -> 50.0
            else -> 0.0
        }
    }

    override fun canWithdraw(amount: Double): Pair<Boolean, String?> {
        val remainingAfterDeduction = _balance - amount
        val maxNegative = -getOverdraftLimit()
        return if (remainingAfterDeduction < maxNegative) {
            Pair(
                false,
                "Exceeds overdraft limit of Rs. ${"%,.2f".format(getOverdraftLimit())}. " +
                        "Current Balance: Rs. ${"%,.2f".format(_balance)}, Amount needed: Rs. ${"%,.2f".format(amount)}"
            )
        } else {
            Pair(true, null)
        }
    }
}
