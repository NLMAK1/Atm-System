package com.example.atm.core.model

/**
 * Concrete Subclass: SavingsAccount
 * Demonstrates Inheritance & Polymorphism.
 *
 * Rules:
 * - Minimum balance: Rs. 5,000
 * - Max withdrawal per transaction: Rs. 50,000
 * - Daily withdrawal limit: Rs. 100,000
 * - Withdrawal fee: Rs. 50
 * - Transfer fee: Rs. 100
 */
class SavingsAccount(
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

    override fun getAccountType(): AccountType = AccountType.SAVINGS

    override fun calculateWithdrawalLimit(): Double = 50_000.0

    override fun getDailyWithdrawalLimit(): Double = 100_000.0

    override fun getDailyTransferLimit(): Double = 150_000.0

    override fun getMinimumBalance(): Double = 5_000.0

    override fun getOverdraftLimit(): Double = 0.0

    override fun calculateTransactionFee(type: TransactionType, amount: Double): Double {
        return when (type) {
            TransactionType.WITHDRAWAL -> 50.0
            TransactionType.TRANSFER_DEBIT -> 100.0
            else -> 0.0
        }
    }

    override fun canWithdraw(amount: Double): Pair<Boolean, String?> {
        val remainingAfterDeduction = _balance - amount
        return if (remainingAfterDeduction < getMinimumBalance()) {
            Pair(
                false,
                "Savings Account requires a minimum balance of Rs. ${"%,.2f".format(getMinimumBalance())}. " +
                        "Current Balance: Rs. ${"%,.2f".format(_balance)}, Amount needed: Rs. ${"%,.2f".format(amount)}"
            )
        } else {
            Pair(true, null)
        }
    }
}
