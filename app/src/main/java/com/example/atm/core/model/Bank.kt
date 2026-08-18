package com.example.atm.core.model

import com.example.atm.core.exceptions.AccountInactiveError
import com.example.atm.core.exceptions.InvalidAccountError
import com.example.atm.core.exceptions.InvalidAmountError

/**
 * Class: Bank
 * Manages bank entities (Customers, Accounts, Cards) and coordinates transactions.
 * Demonstrates Association and Aggregation relationships.
 */
class Bank(
    val bankName: String = "Apex National Bank",
    val bankCode: String = "APEX-001"
) {
    private val customers = mutableMapOf<String, Customer>()
    private val accounts = mutableMapOf<String, Account>()
    private val cards = mutableMapOf<String, Card>()
    private val transactionHistory = mutableListOf<Transaction>()

    fun registerCustomer(customer: Customer) {
        customers[customer.customerId] = customer
    }

    fun registerAccount(account: Account) {
        accounts[account.accountNumber] = account
        customers[account.customerId]?.addAccount(account)
    }

    fun registerCard(card: Card) {
        cards[card.cardNumber] = card
        customers[card.customerId]?.addCard(card)
    }

    fun getCustomer(customerId: String): Customer? = customers[customerId]

    fun getAccount(accountNumber: String): Account? = accounts[accountNumber]

    fun getCard(cardNumber: String): Card? = cards[cardNumber]

    fun getAllAccounts(): List<Account> = accounts.values.toList()

    fun getAllCards(): List<Card> = cards.values.toList()

    fun getAllCustomers(): List<Customer> = customers.values.toList()

    fun recordTransaction(transaction: Transaction) {
        transactionHistory.add(transaction)
    }

    fun getTransactionsForAccount(accountNumber: String): List<Transaction> {
        return transactionHistory
            .filter { it.accountNumber == accountNumber }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Executes atomic inter-account funds transfer.
     */
    fun transferFunds(
        senderAccountNumber: String,
        receiverAccountNumber: String,
        amount: Double,
        txnIdGenerator: () -> String
    ): Pair<TransferTransaction, TransferTransaction> {
        if (senderAccountNumber == receiverAccountNumber) {
            throw InvalidAccountError(receiverAccountNumber, "Sender and receiver accounts cannot be identical.")
        }

        val senderAccount = accounts[senderAccountNumber]
            ?: throw InvalidAccountError(senderAccountNumber, "Sender account does not exist.")

        val receiverAccount = accounts[receiverAccountNumber]
            ?: throw InvalidAccountError(receiverAccountNumber, "Receiver account does not exist.")

        if (!senderAccount.isActive()) {
            throw AccountInactiveError(senderAccountNumber, "Sender account is not active.")
        }

        if (!receiverAccount.isActive()) {
            throw AccountInactiveError(receiverAccountNumber, "Receiver account is not active.")
        }

        if (amount <= 0) {
            throw InvalidAmountError("Transfer amount must be positive.")
        }

        // Debit sender
        val fee = senderAccount.calculateTransactionFee(TransactionType.TRANSFER_DEBIT, amount)
        senderAccount.debitTransfer(amount)

        // Credit receiver
        receiverAccount.creditTransfer(amount)

        val txId = txnIdGenerator()
        val senderTx = TransferTransaction(
            transactionId = "$txId-DR",
            accountNumber = senderAccountNumber,
            targetAccountNumber = receiverAccountNumber,
            amount = amount,
            fee = fee,
            isSenderDebit = true,
            status = TransactionStatus.SUCCESS
        )

        val receiverTx = TransferTransaction(
            transactionId = "$txId-CR",
            accountNumber = receiverAccountNumber,
            targetAccountNumber = senderAccountNumber,
            amount = amount,
            fee = 0.0,
            isSenderDebit = false,
            status = TransactionStatus.SUCCESS
        )

        recordTransaction(senderTx)
        recordTransaction(receiverTx)

        return Pair(senderTx, receiverTx)
    }
}
