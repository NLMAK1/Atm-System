package com.example.atm.data.db

import com.example.atm.core.model.Account
import com.example.atm.core.model.AccountStatus
import com.example.atm.core.model.AccountType
import com.example.atm.core.model.Bank
import com.example.atm.core.model.Card
import com.example.atm.core.model.CardStatus
import com.example.atm.core.model.CashDispenser
import com.example.atm.core.model.CurrentAccount
import com.example.atm.core.model.Customer
import com.example.atm.core.model.DepositTransaction
import com.example.atm.core.model.SavingsAccount
import com.example.atm.core.model.Transaction
import com.example.atm.core.model.TransactionStatus
import com.example.atm.core.model.TransactionType
import com.example.atm.core.model.TransferTransaction
import com.example.atm.core.model.WithdrawalTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONObject

class AtmRepository(private val database: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()
    val allAuditLogs: Flow<List<AuditLogEntity>> = database.auditLogDao().getAllAuditLogs()
    val allAccounts: Flow<List<AccountEntity>> = database.accountDao().getAllAccounts()
    val allCards: Flow<List<CardEntity>> = database.cardDao().getAllCards()
    val allCustomers: Flow<List<CustomerEntity>> = database.customerDao().getAllCustomers()
    val atmCash: Flow<AtmCashEntity?> = database.atmCashDao().observeAtmCash("ATM-042")

    /**
     * Seeds default database records if empty.
     */
    suspend fun initializeDatabaseIfEmpty() = withContext(Dispatchers.IO) {
        val existingAccounts = database.accountDao().getAllAccounts().firstOrNull()
        if (existingAccounts.isNullOrEmpty()) {
            seedInitialData()
        }
    }

    private suspend fun seedInitialData() {
        val customers = listOf(
            CustomerEntity(
                customerId = "CUST-101",
                name = "Alice Morgan",
                phone = "+1-555-0101",
                email = "alice.morgan@apexbank.com"
            ),
            CustomerEntity(
                customerId = "CUST-102",
                name = "Bob Henderson",
                phone = "+1-555-0102",
                email = "bob.henderson@apexbank.com"
            ),
            CustomerEntity(
                customerId = "CUST-103",
                name = "Charlie Vance",
                phone = "+1-555-0103",
                email = "charlie.vance@apexbank.com"
            )
        )
        database.customerDao().insertAll(customers)

        val accounts = listOf(
            AccountEntity(
                accountNumber = "ACC-10002345",
                accountHolderName = "Alice Morgan",
                customerId = "CUST-101",
                accountType = AccountType.SAVINGS.name,
                balance = 75000.0,
                pin = "1984",
                status = AccountStatus.ACTIVE.name,
                dailyWithdrawnAmount = 15000.0,
                dailyTransferAmount = 0.0,
                lastResetDate = Account.getCurrentDateString()
            ),
            AccountEntity(
                accountNumber = "ACC-10002346",
                accountHolderName = "Alice Morgan (Biz)",
                customerId = "CUST-101",
                accountType = AccountType.CURRENT.name,
                balance = 120000.0,
                pin = "1984",
                status = AccountStatus.ACTIVE.name,
                dailyWithdrawnAmount = 0.0,
                dailyTransferAmount = 0.0,
                lastResetDate = Account.getCurrentDateString()
            ),
            AccountEntity(
                accountNumber = "ACC-20005678",
                accountHolderName = "Bob Henderson",
                customerId = "CUST-102",
                accountType = AccountType.SAVINGS.name,
                balance = 35000.0,
                pin = "2468",
                status = AccountStatus.ACTIVE.name,
                dailyWithdrawnAmount = 0.0,
                dailyTransferAmount = 0.0,
                lastResetDate = Account.getCurrentDateString()
            ),
            AccountEntity(
                accountNumber = "ACC-30009876",
                accountHolderName = "Charlie Vance",
                customerId = "CUST-103",
                accountType = AccountType.CURRENT.name,
                balance = 15000.0,
                pin = "7788",
                status = AccountStatus.ACTIVE.name,
                dailyWithdrawnAmount = 0.0,
                dailyTransferAmount = 0.0,
                lastResetDate = Account.getCurrentDateString()
            )
        )
        database.accountDao().insertAll(accounts)

        val cards = listOf(
            CardEntity(
                cardNumber = "4532-8800-1234-5678",
                customerId = "CUST-101",
                primaryAccountNumber = "ACC-10002345",
                pin = "1984",
                status = CardStatus.ACTIVE.name,
                failedPinAttempts = 0
            ),
            CardEntity(
                cardNumber = "4532-8800-1234-9999",
                customerId = "CUST-101",
                primaryAccountNumber = "ACC-10002346",
                pin = "1984",
                status = CardStatus.ACTIVE.name,
                failedPinAttempts = 0
            ),
            CardEntity(
                cardNumber = "5421-9900-8765-4321",
                customerId = "CUST-102",
                primaryAccountNumber = "ACC-20005678",
                pin = "2468",
                status = CardStatus.ACTIVE.name,
                failedPinAttempts = 0
            ),
            CardEntity(
                cardNumber = "4000-1100-3333-7788",
                customerId = "CUST-103",
                primaryAccountNumber = "ACC-30009876",
                pin = "7788",
                status = CardStatus.ACTIVE.name,
                failedPinAttempts = 2 // Pre-set 2 failed attempts so 1 more blocks it!
            )
        )
        database.cardDao().insertAll(cards)

        // Seed ATM Cash: 5000x10, 1000x30, 500x40 = Rs. 100,000
        database.atmCashDao().insertOrUpdateAtmCash(
            AtmCashEntity(
                atmId = "ATM-042",
                notes5000 = 10,
                notes1000 = 30,
                notes500 = 40,
                lastUpdated = System.currentTimeMillis()
            )
        )

        val sampleTransactions = listOf(
            TransactionEntity(
                transactionId = "TXN-1001",
                accountNumber = "ACC-10002345",
                transactionType = TransactionType.DEPOSIT.name,
                amount = 20000.0,
                fee = 0.0,
                timestamp = System.currentTimeMillis() - 86400000 * 3,
                status = TransactionStatus.SUCCESS.name,
                failureReason = null,
                description = "ATM Cash Deposit",
                dispensedNotesJson = null
            ),
            TransactionEntity(
                transactionId = "TXN-1002",
                accountNumber = "ACC-10002345",
                transactionType = TransactionType.WITHDRAWAL.name,
                amount = 10000.0,
                fee = 50.0,
                timestamp = System.currentTimeMillis() - 86400000 * 2,
                status = TransactionStatus.SUCCESS.name,
                failureReason = null,
                description = "ATM Cash Withdrawal",
                dispensedNotesJson = "{\"5000\":2}"
            ),
            TransactionEntity(
                transactionId = "TXN-1003",
                accountNumber = "ACC-10002345",
                transactionType = TransactionType.TRANSFER_DEBIT.name,
                amount = 15000.0,
                fee = 100.0,
                timestamp = System.currentTimeMillis() - 86400000,
                status = TransactionStatus.SUCCESS.name,
                failureReason = null,
                description = "Transfer to ACC-20005678",
                dispensedNotesJson = null
            ),
            TransactionEntity(
                transactionId = "TXN-1004",
                accountNumber = "ACC-10002345",
                transactionType = TransactionType.DEPOSIT.name,
                amount = 30000.0,
                fee = 0.0,
                timestamp = System.currentTimeMillis() - 43200000,
                status = TransactionStatus.SUCCESS.name,
                failureReason = null,
                description = "Salary Deposit",
                dispensedNotesJson = null
            ),
            TransactionEntity(
                transactionId = "TXN-1005",
                accountNumber = "ACC-10002345",
                transactionType = TransactionType.WITHDRAWAL.name,
                amount = 5000.0,
                fee = 50.0,
                timestamp = System.currentTimeMillis() - 21600000,
                status = TransactionStatus.SUCCESS.name,
                failureReason = null,
                description = "ATM Cash Withdrawal",
                dispensedNotesJson = "{\"5000\":1}"
            )
        )
        database.transactionDao().insertAll(sampleTransactions)

        logAudit("SYSTEM_INIT", "ATM-042", "System initialized with sample customers, accounts and cash inventory.", "SUCCESS")
    }

    /**
     * Reconstructs domain Bank instance from database.
     */
    suspend fun loadBank(): Bank = withContext(Dispatchers.IO) {
        val bank = Bank()
        val customers = database.customerDao().getAllCustomers().firstOrNull() ?: emptyList()
        val accounts = database.accountDao().getAllAccounts().firstOrNull() ?: emptyList()
        val cards = database.cardDao().getAllCards().firstOrNull() ?: emptyList()
        val txns = database.transactionDao().getAllTransactions().firstOrNull() ?: emptyList()

        for (c in customers) {
            bank.registerCustomer(
                Customer(
                    customerId = c.customerId,
                    name = c.name,
                    phone = c.phone,
                    email = c.email
                )
            )
        }

        for (a in accounts) {
            val type = runCatching { AccountType.valueOf(a.accountType) }.getOrDefault(AccountType.SAVINGS)
            val status = runCatching { AccountStatus.valueOf(a.status) }.getOrDefault(AccountStatus.ACTIVE)
            val account: Account = if (type == AccountType.CURRENT) {
                CurrentAccount(
                    accountNumber = a.accountNumber,
                    accountHolderName = a.accountHolderName,
                    customerId = a.customerId,
                    balance = a.balance,
                    pin = a.pin,
                    status = status,
                    dailyWithdrawnAmount = a.dailyWithdrawnAmount,
                    dailyTransferAmount = a.dailyTransferAmount,
                    lastResetDate = a.lastResetDate
                )
            } else {
                SavingsAccount(
                    accountNumber = a.accountNumber,
                    accountHolderName = a.accountHolderName,
                    customerId = a.customerId,
                    balance = a.balance,
                    pin = a.pin,
                    status = status,
                    dailyWithdrawnAmount = a.dailyWithdrawnAmount,
                    dailyTransferAmount = a.dailyTransferAmount,
                    lastResetDate = a.lastResetDate
                )
            }
            bank.registerAccount(account)
        }

        for (cd in cards) {
            val status = runCatching { CardStatus.valueOf(cd.status) }.getOrDefault(CardStatus.ACTIVE)
            val card = Card(
                cardNumber = cd.cardNumber,
                customerId = cd.customerId,
                primaryAccountNumber = cd.primaryAccountNumber,
                pin = cd.pin,
                status = status,
                failedPinAttempts = cd.failedPinAttempts
            )
            bank.registerCard(card)
        }

        for (t in txns) {
            val type = runCatching { TransactionType.valueOf(t.transactionType) }.getOrDefault(TransactionType.DEPOSIT)
            val status = runCatching { TransactionStatus.valueOf(t.status) }.getOrDefault(TransactionStatus.SUCCESS)
            val domainTx: Transaction = when (type) {
                TransactionType.WITHDRAWAL -> {
                    val noteMap = parseNotesJson(t.dispensedNotesJson)
                    WithdrawalTransaction(
                        transactionId = t.transactionId,
                        accountNumber = t.accountNumber,
                        amount = t.amount,
                        fee = t.fee,
                        timestamp = t.timestamp,
                        status = status,
                        failureReason = t.failureReason,
                        dispensedDenominations = noteMap,
                        description = t.description
                    )
                }
                TransactionType.DEPOSIT -> {
                    DepositTransaction(
                        transactionId = t.transactionId,
                        accountNumber = t.accountNumber,
                        amount = t.amount,
                        fee = t.fee,
                        timestamp = t.timestamp,
                        status = status,
                        failureReason = t.failureReason,
                        description = t.description
                    )
                }
                TransactionType.TRANSFER_DEBIT -> {
                    TransferTransaction(
                        transactionId = t.transactionId,
                        accountNumber = t.accountNumber,
                        targetAccountNumber = t.description.substringAfter("Transfer to ", ""),
                        amount = t.amount,
                        fee = t.fee,
                        isSenderDebit = true,
                        timestamp = t.timestamp,
                        status = status,
                        failureReason = t.failureReason,
                        description = t.description
                    )
                }
                TransactionType.TRANSFER_CREDIT -> {
                    TransferTransaction(
                        transactionId = t.transactionId,
                        accountNumber = t.accountNumber,
                        targetAccountNumber = t.description.substringAfter("Transfer from ", ""),
                        amount = t.amount,
                        fee = t.fee,
                        isSenderDebit = false,
                        timestamp = t.timestamp,
                        status = status,
                        failureReason = t.failureReason,
                        description = t.description
                    )
                }
                else -> {
                    DepositTransaction(
                        transactionId = t.transactionId,
                        accountNumber = t.accountNumber,
                        amount = t.amount,
                        fee = t.fee,
                        timestamp = t.timestamp,
                        status = status,
                        failureReason = t.failureReason,
                        description = t.description
                    )
                }
            }
            bank.recordTransaction(domainTx)
        }

        bank
    }

    /**
     * Loads ATM cash dispenser from database.
     */
    suspend fun loadAtmCash(atmId: String = "ATM-042"): CashDispenser = withContext(Dispatchers.IO) {
        val entity = database.atmCashDao().getAtmCash(atmId)
        val dispenser = CashDispenser()
        if (entity != null) {
            dispenser.setInventory(entity.notes5000, entity.notes1000, entity.notes500)
        }
        dispenser
    }

    /**
     * Persists updated Account state to database.
     */
    suspend fun saveAccount(account: Account) = withContext(Dispatchers.IO) {
        val existing = database.accountDao().getAccountByNumber(account.accountNumber)
        val entity = AccountEntity(
            accountNumber = account.accountNumber,
            accountHolderName = account.accountHolderName,
            customerId = account.customerId,
            accountType = account.getAccountType().name,
            balance = account.getBalance(),
            pin = existing?.pin ?: "0000",
            status = account.getStatus().name,
            dailyWithdrawnAmount = account.getDailyWithdrawnAmount(),
            dailyTransferAmount = account.getDailyTransferAmount(),
            lastResetDate = account.getLastResetDate()
        )
        database.accountDao().insertAccount(entity)
    }

    /**
     * Persists updated Card state to database.
     */
    suspend fun saveCard(card: Card) = withContext(Dispatchers.IO) {
        val entity = CardEntity(
            cardNumber = card.cardNumber,
            customerId = card.customerId,
            primaryAccountNumber = card.primaryAccountNumber,
            pin = card.getRawPinForStorage(),
            status = card.getStatus().name,
            failedPinAttempts = card.getFailedAttempts()
        )
        database.cardDao().insertCard(entity)
    }

    /**
     * Persists a Transaction to database.
     */
    suspend fun saveTransaction(tx: Transaction) = withContext(Dispatchers.IO) {
        val notesJson = if (tx is WithdrawalTransaction && tx.dispensedDenominations.isNotEmpty()) {
            val json = JSONObject()
            tx.dispensedDenominations.forEach { (k, v) -> json.put(k.toString(), v) }
            json.toString()
        } else null

        val entity = TransactionEntity(
            transactionId = tx.transactionId,
            accountNumber = tx.accountNumber,
            transactionType = tx.getTransactionType().name,
            amount = tx.amount,
            fee = tx.fee,
            timestamp = tx.timestamp,
            status = tx.status.name,
            failureReason = tx.failureReason,
            description = tx.description,
            dispensedNotesJson = notesJson
        )
        database.transactionDao().insertTransaction(entity)
    }

    /**
     * Persists updated ATM Cash inventory to database.
     */
    suspend fun saveAtmCash(atmId: String, dispenser: CashDispenser) = withContext(Dispatchers.IO) {
        val inv = dispenser.getInventory()
        val entity = AtmCashEntity(
            atmId = atmId,
            notes5000 = inv[5000] ?: 0,
            notes1000 = inv[1000] ?: 0,
            notes500 = inv[500] ?: 0,
            lastUpdated = System.currentTimeMillis()
        )
        database.atmCashDao().insertOrUpdateAtmCash(entity)
    }

    /**
     * Logs an audit record to database.
     */
    suspend fun logAudit(eventType: String, entityRef: String, details: String, status: String) = withContext(Dispatchers.IO) {
        database.auditLogDao().insertAuditLog(
            AuditLogEntity(
                timestamp = System.currentTimeMillis(),
                eventType = eventType,
                entityRef = entityRef,
                details = details,
                status = status
            )
        )
    }

    private fun parseNotesJson(jsonString: String?): Map<Int, Int> {
        if (jsonString.isNullOrBlank()) return emptyMap()
        return try {
            val json = JSONObject(jsonString)
            val map = mutableMapOf<Int, Int>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key.toInt()] = json.getInt(key)
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
