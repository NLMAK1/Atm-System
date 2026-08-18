package com.example.atm.core.model

import com.example.atm.core.exceptions.ATMException
import com.example.atm.core.exceptions.AccountInactiveError
import com.example.atm.core.exceptions.CardBlockedError
import com.example.atm.core.exceptions.InvalidAccountError
import com.example.atm.core.exceptions.InvalidAmountError
import java.util.UUID

enum class ATMState {
    IDLE,
    CARD_INSERTED,
    ACCOUNT_SELECTION,
    AUTHENTICATED,
    CARD_BLOCKED
}

/**
 * Class: ATM
 * Coordinates user sessions, card reader, cash dispenser, and banking transactions.
 */
class ATM(
    val atmId: String = "ATM-042",
    val location: String = "Main Street Branch",
    val dispenser: CashDispenser = CashDispenser(),
    val bank: Bank = Bank()
) {
    var state: ATMState = ATMState.IDLE
        private set

    var currentCard: Card? = null
        private set

    var currentCustomer: Customer? = null
        private set

    var currentAccount: Account? = null
        private set

    fun isSessionActive(): Boolean = state == ATMState.AUTHENTICATED && currentAccount != null

    /**
     * 1. Insert Card step
     */
    fun insertCard(cardNumber: String): Card {
        if (state != ATMState.IDLE) {
            throw ATMException("A card session is already active. Please finish or eject first.")
        }

        val card = bank.getCard(cardNumber)
            ?: throw InvalidAccountError(cardNumber, "Card $cardNumber is not recognized by the system.")

        if (card.isBlocked()) {
            state = ATMState.CARD_BLOCKED
            currentCard = card
            throw CardBlockedError("Card $cardNumber is BLOCKED. Transaction not allowed.")
        }

        currentCard = card
        currentCustomer = bank.getCustomer(card.customerId)
        state = ATMState.CARD_INSERTED
        return card
    }

    /**
     * 2. Enter PIN and Authenticate
     */
    fun enterPin(pin: String): Customer {
        val card = currentCard
            ?: throw ATMException("No card inserted. Please insert your card first.")

        if (card.isBlocked()) {
            state = ATMState.CARD_BLOCKED
            throw CardBlockedError()
        }

        // Validate PIN on Card
        try {
            card.validatePin(pin)
        } catch (e: Exception) {
            if (card.isBlocked()) {
                state = ATMState.CARD_BLOCKED
            }
            throw e
        }

        val customer = currentCustomer
            ?: throw InvalidAccountError("", "Customer profile not found for card.")

        // If customer has multiple accounts, allow account selection, else default to primary account
        val linkedAccounts = customer.accounts
        if (linkedAccounts.size > 1) {
            state = ATMState.ACCOUNT_SELECTION
            // Try to set primary account by default if found
            currentAccount = customer.findAccount(card.primaryAccountNumber) ?: linkedAccounts.first()
        } else if (linkedAccounts.isNotEmpty()) {
            currentAccount = linkedAccounts.first()
            state = ATMState.AUTHENTICATED
        } else {
            val primaryAcc = bank.getAccount(card.primaryAccountNumber)
                ?: throw InvalidAccountError(card.primaryAccountNumber, "No bank account linked to this card.")
            currentAccount = primaryAcc
            state = ATMState.AUTHENTICATED
        }

        return customer
    }

    /**
     * Select account (for customers with multiple accounts)
     */
    fun selectAccount(accountNumber: String): Account {
        val customer = currentCustomer
            ?: throw ATMException("No customer authenticated.")

        val acc = customer.findAccount(accountNumber)
            ?: throw InvalidAccountError(accountNumber, "Account not associated with current customer.")

        if (!acc.isActive()) {
            throw AccountInactiveError(accountNumber)
        }

        currentAccount = acc
        state = ATMState.AUTHENTICATED
        return acc
    }

    /**
     * 3. Check Balance
     */
    fun checkBalance(): Double {
        val acc = requireActiveAccount()
        return acc.getBalance()
    }

    /**
     * 4. Deposit Money
     */
    fun deposit(amount: Double): DepositTransaction {
        val acc = requireActiveAccount()
        val txId = generateTxnId()

        acc.deposit(amount)

        val tx = DepositTransaction(
            transactionId = txId,
            accountNumber = acc.accountNumber,
            amount = amount,
            status = TransactionStatus.SUCCESS
        )

        bank.recordTransaction(tx)
        return tx
    }

    /**
     * 5. Withdraw Cash
     * Validates account rules, daily limits, ATM cash inventory, and dispenses notes.
     */
    fun withdraw(amount: Double): Pair<WithdrawalTransaction, Map<Int, Int>> {
        val acc = requireActiveAccount()

        // 1. Verify Cash Dispenser can dispense notes (Greedy denomination check)
        val dispensedNotes = dispenser.calculateDispense(amount)

        // 2. Withdraw from account (validates daily limits, min balance/overdraft, deducts fee)
        val fee = acc.calculateTransactionFee(TransactionType.WITHDRAWAL, amount)
        acc.withdraw(amount)

        // 3. Deduct cash notes from ATM dispenser
        dispenser.dispense(amount)

        val txId = generateTxnId()
        val tx = WithdrawalTransaction(
            transactionId = txId,
            accountNumber = acc.accountNumber,
            amount = amount,
            fee = fee,
            dispensedDenominations = dispensedNotes,
            status = TransactionStatus.SUCCESS
        )

        bank.recordTransaction(tx)
        return Pair(tx, dispensedNotes)
    }

    /**
     * 6. Transfer Money between accounts
     */
    fun transferMoney(targetAccountNumber: String, amount: Double): TransferTransaction {
        val acc = requireActiveAccount()
        val (senderTx, _) = bank.transferFunds(
            senderAccountNumber = acc.accountNumber,
            receiverAccountNumber = targetAccountNumber,
            amount = amount,
            txnIdGenerator = { generateTxnId() }
        )
        return senderTx
    }

    /**
     * 7. Change PIN
     */
    fun changePin(oldPin: String, newPin: String) {
        val card = currentCard ?: throw ATMException("No card in session.")
        val acc = currentAccount ?: throw ATMException("No account in session.")

        // Update PIN on both Card and Account
        card.changePin(oldPin, newPin)
        try {
            acc.changePin(oldPin, newPin)
        } catch (_: Exception) {
            // Account PIN sync
        }
    }

    /**
     * 8. Mini Statement (Last 5 transactions)
     */
    fun getMiniStatement(limit: Int = 5): List<Transaction> {
        val acc = requireActiveAccount()
        val txns = bank.getTransactionsForAccount(acc.accountNumber)
        return txns.take(limit)
    }

    /**
     * 9. Eject Card / End Session
     */
    fun ejectCard() {
        currentCard = null
        currentCustomer = null
        currentAccount = null
        state = ATMState.IDLE
    }

    private fun requireActiveAccount(): Account {
        if (state == ATMState.CARD_BLOCKED) {
            throw CardBlockedError()
        }
        val acc = currentAccount
            ?: throw ATMException("No active account selected. Please authenticate first.")
        if (!acc.isActive()) {
            throw AccountInactiveError(acc.accountNumber)
        }
        return acc
    }

    private fun generateTxnId(): String {
        val shortId = UUID.randomUUID().toString().take(6).uppercase()
        return "TXN-$shortId"
    }
}
