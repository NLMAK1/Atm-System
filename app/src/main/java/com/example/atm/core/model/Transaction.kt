package com.example.atm.core.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Abstract Base Class: Transaction
 * Demonstrates Polymorphism and Abstraction in the transaction subsystem.
 */
abstract class Transaction(
    val transactionId: String,
    val accountNumber: String,
    val amount: Double,
    val fee: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: TransactionStatus = TransactionStatus.SUCCESS,
    val failureReason: String? = null,
    val description: String = ""
) {

    abstract fun getTransactionType(): TransactionType

    abstract fun getDisplayType(): String

    abstract fun getSignPrefix(): String

    open fun getFormattedDateTime(): String {
        val sdf = SimpleDateFormat("dd-MMM HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    open fun formatReceipt(): String {
        return buildString {
            appendLine("----------------------------------------")
            appendLine("         ATM TRANSACTION RECEIPT        ")
            appendLine("----------------------------------------")
            appendLine("TXN ID   : $transactionId")
            appendLine("Date/Time: ${getFormattedDateTime()}")
            appendLine("Account  : $accountNumber")
            appendLine("Type     : ${getDisplayType()}")
            appendLine("Amount   : Rs. ${"%,.2f".format(amount)}")
            if (fee > 0) {
                appendLine("Fee      : Rs. ${"%,.2f".format(fee)}")
            }
            appendLine("Status   : $status")
            if (failureReason != null) {
                appendLine("Remarks  : $failureReason")
            }
            appendLine("----------------------------------------")
        }
    }
}

/**
 * Concrete Subclass: WithdrawalTransaction
 */
class WithdrawalTransaction(
    transactionId: String,
    accountNumber: String,
    amount: Double,
    fee: Double = 0.0,
    timestamp: Long = System.currentTimeMillis(),
    status: TransactionStatus = TransactionStatus.SUCCESS,
    failureReason: String? = null,
    val dispensedDenominations: Map<Int, Int> = emptyMap(),
    description: String = "ATM Cash Withdrawal"
) : Transaction(
    transactionId = transactionId,
    accountNumber = accountNumber,
    amount = amount,
    fee = fee,
    timestamp = timestamp,
    status = status,
    failureReason = failureReason,
    description = description
) {
    override fun getTransactionType(): TransactionType = TransactionType.WITHDRAWAL

    override fun getDisplayType(): String = "Withdrawal"

    override fun getSignPrefix(): String = "-"

    override fun formatReceipt(): String {
        val base = super.formatReceipt()
        if (dispensedDenominations.isNotEmpty()) {
            val denomSummary = dispensedDenominations.entries
                .filter { it.value > 0 }
                .sortedByDescending { it.key }
                .joinToString(", ") { "${it.key}x${it.value}" }
            return base + "Notes Dispensed: $denomSummary\n----------------------------------------\n"
        }
        return base
    }
}

/**
 * Concrete Subclass: DepositTransaction
 */
class DepositTransaction(
    transactionId: String,
    accountNumber: String,
    amount: Double,
    fee: Double = 0.0,
    timestamp: Long = System.currentTimeMillis(),
    status: TransactionStatus = TransactionStatus.SUCCESS,
    failureReason: String? = null,
    description: String = "ATM Cash Deposit"
) : Transaction(
    transactionId = transactionId,
    accountNumber = accountNumber,
    amount = amount,
    fee = fee,
    timestamp = timestamp,
    status = status,
    failureReason = failureReason,
    description = description
) {
    override fun getTransactionType(): TransactionType = TransactionType.DEPOSIT

    override fun getDisplayType(): String = "Deposit"

    override fun getSignPrefix(): String = "+"
}

/**
 * Concrete Subclass: TransferTransaction
 */
class TransferTransaction(
    transactionId: String,
    accountNumber: String,
    val targetAccountNumber: String,
    amount: Double,
    fee: Double = 0.0,
    val isSenderDebit: Boolean = true,
    timestamp: Long = System.currentTimeMillis(),
    status: TransactionStatus = TransactionStatus.SUCCESS,
    failureReason: String? = null,
    description: String = if (isSenderDebit) "Transfer to $targetAccountNumber" else "Transfer from $accountNumber"
) : Transaction(
    transactionId = transactionId,
    accountNumber = accountNumber,
    amount = amount,
    fee = fee,
    timestamp = timestamp,
    status = status,
    failureReason = failureReason,
    description = description
) {
    override fun getTransactionType(): TransactionType =
        if (isSenderDebit) TransactionType.TRANSFER_DEBIT else TransactionType.TRANSFER_CREDIT

    override fun getDisplayType(): String = if (isSenderDebit) "Transfer (Out)" else "Transfer (In)"

    override fun getSignPrefix(): String = if (isSenderDebit) "-" else "+"

    override fun formatReceipt(): String {
        return buildString {
            appendLine("----------------------------------------")
            appendLine("       FUNDS TRANSFER RECEIPT           ")
            appendLine("----------------------------------------")
            appendLine("TXN ID   : $transactionId")
            appendLine("Date/Time: ${getFormattedDateTime()}")
            appendLine("From A/C : $accountNumber")
            appendLine("To A/C   : $targetAccountNumber")
            appendLine("Amount   : Rs. ${"%,.2f".format(amount)}")
            if (fee > 0) {
                appendLine("Fee      : Rs. ${"%,.2f".format(fee)}")
            }
            appendLine("Status   : $status")
            appendLine("----------------------------------------")
        }
    }
}
