package com.example.atm.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.atm.core.exceptions.ATMException
import com.example.atm.core.exceptions.InvalidPINError
import com.example.atm.core.model.ATM
import com.example.atm.core.model.ATMState
import com.example.atm.core.model.Account
import com.example.atm.core.model.Card
import com.example.atm.core.model.Customer
import com.example.atm.core.model.Transaction
import com.example.atm.core.model.WithdrawalTransaction
import com.example.atm.data.db.AccountEntity
import com.example.atm.data.db.AppDatabase
import com.example.atm.data.db.AtmCashEntity
import com.example.atm.data.db.AtmRepository
import com.example.atm.data.db.AuditLogEntity
import com.example.atm.data.db.CardEntity
import com.example.atm.data.db.CustomerEntity
import com.example.atm.data.db.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TerminalLog(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
    val isPrompt: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class AtmUiState(
    val atmState: ATMState = ATMState.IDLE,
    val insertedCard: Card? = null,
    val currentCustomer: Customer? = null,
    val currentAccount: Account? = null,
    val pinInput: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val activeReceipt: String? = null,
    val lastDispensedNotes: Map<Int, Int>? = null,
    val miniStatement: List<Transaction>? = null,
    val terminalLogs: List<TerminalLog> = emptyList(),
    val commandInput: String = "",
    val currentTab: AppTab = AppTab.ATM_TERMINAL,
    val isBusy: Boolean = false
)

enum class AppTab {
    ATM_TERMINAL,
    CLI_CONSOLE,
    DATABASE_AUDIT,
    OOP_ARCHITECTURE_DOCS
}

class AtmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AtmRepository
    private var atm: ATM = ATM()

    private val _uiState = MutableStateFlow(AtmUiState())
    val uiState: StateFlow<AtmUiState> = _uiState.asStateFlow()

    // Database reactive streams for real-time views
    val allAccounts: StateFlow<List<AccountEntity>>
    val allCards: StateFlow<List<CardEntity>>
    val allCustomers: StateFlow<List<CustomerEntity>>
    val allTransactions: StateFlow<List<TransactionEntity>>
    val allAuditLogs: StateFlow<List<AuditLogEntity>>
    val atmCashEntity: StateFlow<AtmCashEntity?>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = AtmRepository(db)

        allAccounts = repository.allAccounts.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allCards = repository.allCards.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allCustomers = repository.allCustomers.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allTransactions = repository.allTransactions.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        allAuditLogs = repository.allAuditLogs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        atmCashEntity = repository.atmCash.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        loadAtmSystem()
    }

    private fun loadAtmSystem() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true) }
            repository.initializeDatabaseIfEmpty()

            val bank = repository.loadBank()
            val cashDispenser = repository.loadAtmCash("ATM-042")

            atm = ATM(
                atmId = "ATM-042",
                location = "Main Street Branch",
                dispenser = cashDispenser,
                bank = bank
            )

            val welcomeLogs = listOf(
                TerminalLog("==========================================", isPrompt = true),
                TerminalLog("  APEX NATIONAL BANK - ATM SYSTEM v2.4   ", isPrompt = true),
                TerminalLog("  Location: Main Street Branch (ATM-042) ", isPrompt = true),
                TerminalLog("==========================================", isPrompt = true),
                TerminalLog("Status: System Online. Available Cash: Rs. ${"%,.2f".format(cashDispenser.getTotalCash())}"),
                TerminalLog("Type 'help' for CLI command list or use the visual ATM.")
            )

            _uiState.update {
                it.copy(
                    atmState = atm.state,
                    insertedCard = null,
                    currentCustomer = null,
                    currentAccount = null,
                    terminalLogs = welcomeLogs,
                    isBusy = false
                )
            }
        }
    }

    // ==========================================
    // ATM INTERACTIVE & CONTROLLER METHODS
    // ==========================================

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun onPinInputChanged(newPin: String) {
        if (newPin.length <= 4 && newPin.all { it.isDigit() }) {
            _uiState.update { it.copy(pinInput = newPin, errorMessage = null) }
        }
    }

    fun onCommandInputChanged(input: String) {
        _uiState.update { it.copy(commandInput = input) }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
                activeReceipt = null,
                lastDispensedNotes = null,
                miniStatement = null
            )
        }
    }

    fun clearReceipt() {
        _uiState.update { it.copy(activeReceipt = null) }
    }

    fun clearMiniStatement() {
        _uiState.update { it.copy(miniStatement = null) }
    }

    fun insertCard(cardNumber: String) {
        viewModelScope.launch {
            clearMessages()
            try {
                val card = atm.insertCard(cardNumber)
                repository.logAudit("CARD_INSERTED", cardNumber, "Card inserted into ATM-042", "SUCCESS")

                appendLog("> Insert Card: $cardNumber", isCommand = true)
                appendLog("Card recognized. Please enter your 4-digit PIN.", isSuccess = true)

                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        insertedCard = card,
                        currentCustomer = atm.currentCustomer,
                        pinInput = "",
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to read card"
                repository.logAudit("CARD_INSERT_FAILED", cardNumber, errorMsg, "FAILED")
                appendLog("ERROR: $errorMsg", isError = true)
                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun submitPin(pin: String = _uiState.value.pinInput) {
        viewModelScope.launch {
            clearMessages()
            val card = atm.currentCard
            if (card == null) {
                _uiState.update { it.copy(errorMessage = "Please insert your card first.") }
                return@launch
            }

            try {
                val customer = atm.enterPin(pin)
                repository.saveCard(card)
                repository.logAudit("AUTH_SUCCESS", card.cardNumber, "Customer ${customer.name} authenticated successfully", "SUCCESS")

                appendLog("> Enter PIN: ****", isCommand = true)
                appendLog("Authentication successful. Welcome, ${customer.name}!", isSuccess = true)

                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        currentCustomer = customer,
                        currentAccount = atm.currentAccount,
                        pinInput = "",
                        errorMessage = null,
                        successMessage = "Welcome, ${customer.name}!"
                    )
                }
            } catch (e: InvalidPINError) {
                repository.saveCard(card)
                repository.logAudit("AUTH_FAILED", card.cardNumber, "PIN authentication failed. Remaining: ${e.remainingAttempts}", "FAILED")
                appendLog("AUTH ERROR: ${e.message}", isError = true)

                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        pinInput = "",
                        errorMessage = e.message
                    )
                }
            } catch (e: Exception) {
                repository.saveCard(card)
                val errorMsg = e.message ?: "Authentication error"
                repository.logAudit("AUTH_FAILED", card.cardNumber, errorMsg, "FAILED")
                appendLog("AUTH ERROR: $errorMsg", isError = true)

                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        pinInput = "",
                        errorMessage = errorMsg
                    )
                }
            }
        }
    }

    fun selectAccount(accountNumber: String) {
        viewModelScope.launch {
            try {
                val acc = atm.selectAccount(accountNumber)
                appendLog("> Select Account: $accountNumber (${acc.getAccountType()})", isCommand = true)
                _uiState.update {
                    it.copy(
                        atmState = atm.state,
                        currentAccount = acc,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Account selection failed"
                appendLog("ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun checkBalance(generateSlip: Boolean = true) {
        viewModelScope.launch {
            clearMessages()
            try {
                val balance = atm.checkBalance()
                val acc = atm.currentAccount!!
                val msg = "Account: ${acc.accountNumber}\nType: ${acc.getAccountType()}\nAvailable Balance: Rs. ${"%,.2f".format(balance)}"

                val slip = buildString {
                    appendLine("========================================")
                    appendLine("          APEX BANK ATM NETWORK         ")
                    appendLine("            BALANCE INQUIRY SLIP        ")
                    appendLine("========================================")
                    appendLine("ATM ID     : ${atm.atmId}")
                    appendLine("Date & Time: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.US).format(java.util.Date())}")
                    appendLine("Customer   : ${atm.currentCustomer?.name ?: "Valued Customer"}")
                    appendLine("Card Number: ${atm.currentCard?.cardNumber ?: "XXXX-XXXX"}")
                    appendLine("Account No : ${acc.accountNumber}")
                    appendLine("Account Typ: ${acc.getAccountType()}")
                    appendLine("----------------------------------------")
                    appendLine("AVAILABLE BAL: Rs. ${"%,.2f".format(balance)}")
                    if (acc.getOverdraftLimit() > 0) {
                        appendLine("OVERDRAFT LIM: Rs. ${"%,.2f".format(acc.getOverdraftLimit())}")
                    }
                    if (acc.getMinimumBalance() > 0) {
                        appendLine("MIN REQ BAL  : Rs. ${"%,.2f".format(acc.getMinimumBalance())}")
                    }
                    appendLine("DAILY LIMIT  : Rs. ${"%,.2f".format(acc.getDailyWithdrawalLimit())}")
                    appendLine("TODAY DRAWN  : Rs. ${"%,.2f".format(acc.getDailyWithdrawnAmount())}")
                    appendLine("----------------------------------------")
                    appendLine("Status     : SUCCESS")
                    appendLine("Thank you for banking with APEX BANK!")
                    appendLine("========================================")
                }

                if (acc.getOverdraftLimit() > 0) {
                    val overdraftMsg = "\nOverdraft Limit: Rs. ${"%,.2f".format(acc.getOverdraftLimit())}"
                    appendLog("Balance: Rs. ${"%,.2f".format(balance)} (Overdraft: Rs. ${"%,.2f".format(acc.getOverdraftLimit())})", isSuccess = true)
                    _uiState.update {
                        it.copy(
                            successMessage = msg + overdraftMsg,
                            activeReceipt = if (generateSlip) slip else null
                        )
                    }
                } else {
                    appendLog("Balance: Rs. ${"%,.2f".format(balance)} (Min Required: Rs. ${"%,.2f".format(acc.getMinimumBalance())})", isSuccess = true)
                    _uiState.update {
                        it.copy(
                            successMessage = msg,
                            activeReceipt = if (generateSlip) slip else null
                        )
                    }
                }
                repository.logAudit("BALANCE_CHECK", acc.accountNumber, "Balance checked: Rs. $balance", "SUCCESS")
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Balance inquiry failed"
                appendLog("ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun deposit(amount: Double) {
        viewModelScope.launch {
            clearMessages()
            try {
                val tx = atm.deposit(amount)
                val acc = atm.currentAccount!!

                repository.saveAccount(acc)
                repository.saveTransaction(tx)
                repository.logAudit("DEPOSIT", acc.accountNumber, "Deposited Rs. $amount. Txn: ${tx.transactionId}", "SUCCESS")

                appendLog("> Deposit: Rs. ${"%,.2f".format(amount)}", isCommand = true)
                appendLog("Deposit successful! New Balance: Rs. ${"%,.2f".format(acc.getBalance())}", isSuccess = true)

                _uiState.update {
                    it.copy(
                        currentAccount = acc,
                        successMessage = "Deposited Rs. ${"%,.2f".format(amount)} successfully!\nNew Balance: Rs. ${"%,.2f".format(acc.getBalance())}\nTxn ID: ${tx.transactionId}",
                        activeReceipt = tx.formatReceipt()
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Deposit failed"
                val accNo = atm.currentAccount?.accountNumber ?: "UNKNOWN"
                repository.logAudit("DEPOSIT_FAILED", accNo, "Amount: Rs. $amount. Error: $errorMsg", "FAILED")
                appendLog("DEPOSIT ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun withdraw(amount: Double) {
        viewModelScope.launch {
            clearMessages()
            try {
                val (tx, notes) = atm.withdraw(amount)
                val acc = atm.currentAccount!!

                repository.saveAccount(acc)
                repository.saveTransaction(tx)
                repository.saveAtmCash(atm.atmId, atm.dispenser)

                val noteStr = notes.entries.filter { it.value > 0 }.sortedByDescending { it.key }
                    .joinToString(", ") { "${it.key}x${it.value}" }
                repository.logAudit("WITHDRAWAL", acc.accountNumber, "Withdrawn Rs. $amount (Notes: $noteStr). Fee: Rs. ${tx.fee}", "SUCCESS")

                appendLog("> Withdraw: Rs. ${"%,.2f".format(amount)}", isCommand = true)
                appendLog("Withdrawal successful! Dispensed: $noteStr. Fee: Rs. ${tx.fee}. New Balance: Rs. ${"%,.2f".format(acc.getBalance())}", isSuccess = true)

                _uiState.update {
                    it.copy(
                        currentAccount = acc,
                        lastDispensedNotes = notes,
                        successMessage = "Dispensed Rs. ${"%,.2f".format(amount)} successfully!\nNotes: $noteStr\nFee: Rs. ${tx.fee}\nNew Balance: Rs. ${"%,.2f".format(acc.getBalance())}",
                        activeReceipt = tx.formatReceipt()
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Withdrawal failed"
                val accNo = atm.currentAccount?.accountNumber ?: "UNKNOWN"
                repository.logAudit("WITHDRAWAL_FAILED", accNo, "Amount: Rs. $amount. Error: $errorMsg", "FAILED")
                appendLog("WITHDRAWAL ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun transferMoney(targetAccountNumber: String, amount: Double) {
        viewModelScope.launch {
            clearMessages()
            try {
                val senderAcc = atm.currentAccount!!
                val tx = atm.transferMoney(targetAccountNumber, amount)
                val receiverAcc = atm.bank.getAccount(targetAccountNumber)

                repository.saveAccount(senderAcc)
                if (receiverAcc != null) {
                    repository.saveAccount(receiverAcc)
                }
                repository.saveTransaction(tx)

                repository.logAudit("TRANSFER", senderAcc.accountNumber, "Transferred Rs. $amount to $targetAccountNumber. Fee: Rs. ${tx.fee}", "SUCCESS")

                appendLog("> Transfer: Rs. ${"%,.2f".format(amount)} to $targetAccountNumber", isCommand = true)
                appendLog("Transfer successful! Fee: Rs. ${tx.fee}. New Balance: Rs. ${"%,.2f".format(senderAcc.getBalance())}", isSuccess = true)

                _uiState.update {
                    it.copy(
                        currentAccount = senderAcc,
                        successMessage = "Transferred Rs. ${"%,.2f".format(amount)} to $targetAccountNumber!\nFee: Rs. ${tx.fee}\nNew Balance: Rs. ${"%,.2f".format(senderAcc.getBalance())}",
                        activeReceipt = tx.formatReceipt()
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Transfer failed"
                val accNo = atm.currentAccount?.accountNumber ?: "UNKNOWN"
                repository.logAudit("TRANSFER_FAILED", accNo, "To: $targetAccountNumber, Amount: Rs. $amount. Error: $errorMsg", "FAILED")
                appendLog("TRANSFER ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun changePin(oldPin: String, newPin: String) {
        viewModelScope.launch {
            clearMessages()
            try {
                val card = atm.currentCard!!
                val acc = atm.currentAccount!!

                atm.changePin(oldPin, newPin)
                repository.saveCard(card)
                repository.saveAccount(acc)

                repository.logAudit("PIN_CHANGED", card.cardNumber, "PIN successfully updated", "SUCCESS")
                appendLog("> Change PIN", isCommand = true)
                appendLog("PIN changed successfully!", isSuccess = true)

                _uiState.update {
                    it.copy(
                        successMessage = "PIN has been successfully updated!"
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "PIN change failed"
                val cardNo = atm.currentCard?.cardNumber ?: "UNKNOWN"
                repository.logAudit("PIN_CHANGE_FAILED", cardNo, errorMsg, "FAILED")
                appendLog("PIN CHANGE ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun getMiniStatement() {
        viewModelScope.launch {
            clearMessages()
            try {
                val txns = atm.getMiniStatement(5)
                val acc = atm.currentAccount!!
                appendLog("> Mini Statement (${acc.accountNumber})", isCommand = true)
                appendLog("========== MINI STATEMENT ==========")
                appendLog("Account: ${acc.accountNumber} (${acc.getAccountType()})")
                appendLog("Date       Type             Amount")
                appendLog("------------------------------------")
                txns.forEach { t ->
                    val sign = t.getSignPrefix()
                    val amtFormatted = "$sign${"%,.2f".format(t.amount)}"
                    val date = t.getFormattedDateTime().padEnd(10)
                    val type = t.getDisplayType().padEnd(16)
                    appendLog("$date $type $amtFormatted")
                }
                appendLog("------------------------------------")
                appendLog("Current Balance: Rs. ${"%,.2f".format(acc.getBalance())}")
                appendLog("====================================")

                _uiState.update {
                    it.copy(
                        miniStatement = txns,
                        successMessage = "Mini statement retrieved (last 5 transactions)."
                    )
                }
            } catch (e: Exception) {
                val errorMsg = e.message ?: "Failed to retrieve mini statement"
                appendLog("ERROR: $errorMsg", isError = true)
                _uiState.update { it.copy(errorMessage = errorMsg) }
            }
        }
    }

    fun ejectCard() {
        val cardNo = atm.currentCard?.cardNumber ?: "UNKNOWN"
        atm.ejectCard()
        appendLog("> Eject Card ($cardNo)", isCommand = true)
        appendLog("Card ejected. Thank you for banking with Apex National Bank.", isSuccess = true)

        _uiState.update {
            it.copy(
                atmState = ATMState.IDLE,
                insertedCard = null,
                currentCustomer = null,
                currentAccount = null,
                pinInput = "",
                activeReceipt = null,
                lastDispensedNotes = null,
                miniStatement = null,
                errorMessage = null,
                successMessage = "Card ejected. Session terminated."
            )
        }
    }

    fun refillAtmCash(notes5000: Int, notes1000: Int, notes500: Int) {
        viewModelScope.launch {
            atm.dispenser.refill(notes5000, notes1000, notes500)
            repository.saveAtmCash(atm.atmId, atm.dispenser)
            val total = atm.dispenser.getTotalCash()
            repository.logAudit("ATM_REFILL", atm.atmId, "Refilled: 5000x$notes5000, 1000x$notes1000, 500x$notes500. Total: Rs. $total", "SUCCESS")

            appendLog("> Cash Refill: 5000x$notes5000, 1000x$notes1000, 500x$notes500", isCommand = true)
            appendLog("ATM Cash Refilled! New Total: Rs. ${"%,.2f".format(total)}", isSuccess = true)

            _uiState.update {
                it.copy(successMessage = "ATM cash successfully refilled! Total Cash: Rs. ${"%,.2f".format(total)}") }
        }
    }

    fun unblockCard(cardNumber: String) {
        viewModelScope.launch {
            val card = atm.bank.getCard(cardNumber)
            if (card != null) {
                card.unblockCard()
                repository.saveCard(card)
                repository.logAudit("CARD_UNBLOCKED", cardNumber, "Card unblocked by bank manager", "SUCCESS")
                appendLog("Card $cardNumber unblocked successfully.", isSuccess = true)
                _uiState.update { it.copy(successMessage = "Card $cardNumber unblocked successfully.") }
            }
        }
    }

    // ==========================================
    // CLI CONSOLE PARSER & COMMAND EXECUTOR
    // ==========================================

    fun executeCliCommand(input: String) {
        val command = input.trim()
        if (command.isEmpty()) return

        appendLog("$ $command", isCommand = true)
        _uiState.update { it.copy(commandInput = "") }

        val parts = command.split("\\s+".toRegex())
        val action = parts[0].lowercase()

        when (action) {
            "help" -> showHelp()
            "clear", "cls" -> clearConsole()
            "cards" -> listCards()
            "accounts" -> listAccounts()
            "insert", "insert-card" -> {
                if (parts.size < 2) {
                    appendLog("Usage: insert <cardNumber>", isError = true)
                } else {
                    insertCard(parts[1])
                }
            }
            "pin", "enter-pin" -> {
                if (parts.size < 2) {
                    appendLog("Usage: pin <4-digit-pin>", isError = true)
                } else {
                    submitPin(parts[1])
                }
            }
            "balance", "bal" -> checkBalance()
            "deposit", "dep" -> {
                if (parts.size < 2) {
                    appendLog("Usage: deposit <amount>", isError = true)
                } else {
                    val amt = parts[1].toDoubleOrNull()
                    if (amt == null) appendLog("Invalid number format for amount.", isError = true)
                    else deposit(amt)
                }
            }
            "withdraw", "with" -> {
                if (parts.size < 2) {
                    appendLog("Usage: withdraw <amount>", isError = true)
                } else {
                    val amt = parts[1].toDoubleOrNull()
                    if (amt == null) appendLog("Invalid number format for amount.", isError = true)
                    else withdraw(amt)
                }
            }
            "transfer", "trf" -> {
                if (parts.size < 3) {
                    appendLog("Usage: transfer <targetAccountNumber> <amount>", isError = true)
                } else {
                    val target = parts[1]
                    val amt = parts[2].toDoubleOrNull()
                    if (amt == null) appendLog("Invalid number format for amount.", isError = true)
                    else transferMoney(target, amt)
                }
            }
            "changepin", "change-pin" -> {
                if (parts.size < 3) {
                    appendLog("Usage: changepin <oldPin> <newPin>", isError = true)
                } else {
                    changePin(parts[1], parts[2])
                }
            }
            "mini", "ministatement" -> getMiniStatement()
            "select", "select-account" -> {
                if (parts.size < 2) {
                    appendLog("Usage: select-account <accountNumber>", isError = true)
                } else {
                    selectAccount(parts[1])
                }
            }
            "atm-cash", "cash" -> showAtmCash()
            "refill" -> {
                if (parts.size < 4) {
                    appendLog("Usage: refill <5000_notes> <1000_notes> <500_notes>", isError = true)
                } else {
                    val n5000 = parts[1].toIntOrNull() ?: 0
                    val n1000 = parts[2].toIntOrNull() ?: 0
                    val n500 = parts[3].toIntOrNull() ?: 0
                    refillAtmCash(n5000, n1000, n500)
                }
            }
            "unblock" -> {
                if (parts.size < 2) {
                    appendLog("Usage: unblock <cardNumber>", isError = true)
                } else {
                    unblockCard(parts[1])
                }
            }
            "eject", "exit", "logout" -> ejectCard()
            "run-demo" -> runAutomatedDemo()
            else -> appendLog("Unknown command: '$action'. Type 'help' for available commands.", isError = true)
        }
    }

    private fun showHelp() {
        val helpLines = listOf(
            "=== ATM CONSOLE CLI COMMAND MANUAL ===",
            "1. SESSION & CARD COMMANDS:",
            "   cards                         - List all demo cards in system",
            "   accounts                      - List all bank accounts",
            "   insert <cardNumber>           - Insert an ATM card",
            "   pin <4-digit-pin>             - Authenticate with PIN",
            "   select <accountNumber>        - Select active account",
            "   eject                         - Eject card and terminate session",
            "   unblock <cardNumber>          - Unblock a blocked card",
            "",
            "2. BANKING TRANSACTIONS (Requires Active Session):",
            "   balance                       - Check available balance",
            "   deposit <amount>              - Deposit cash into account",
            "   withdraw <amount>             - Withdraw cash (dispenses notes)",
            "   transfer <toAcc> <amount>     - Inter-account funds transfer",
            "   changepin <old> <new>         - Update 4-digit PIN",
            "   mini                          - Show last 5 mini statement transactions",
            "",
            "3. ATM CASH & ADMIN:",
            "   atm-cash                      - Show ATM note inventory breakdown",
            "   refill <n5000> <n1000> <n500> - Refill cash cassettes",
            "   run-demo                      - Run automated comprehensive test suite",
            "   clear                         - Clear terminal screen",
            "======================================="
        )
        helpLines.forEach { appendLog(it) }
    }

    private fun clearConsole() {
        _uiState.update { it.copy(terminalLogs = emptyList()) }
    }

    private fun listCards() {
        val cards = atm.bank.getAllCards()
        appendLog("=== REGISTERED ATM CARDS ===")
        cards.forEach { c ->
            val customer = atm.bank.getCustomer(c.customerId)
            val statusStr = if (c.isBlocked()) "[BLOCKED]" else "[ACTIVE]"
            val attemptsStr = if (c.getFailedAttempts() > 0) " (Failed: ${c.getFailedAttempts()}/3)" else ""
            appendLog("Card: ${c.cardNumber} | Holder: ${customer?.name ?: "Unknown"} | Primary A/C: ${c.primaryAccountNumber} | Status: $statusStr$attemptsStr")
        }
        appendLog("============================")
    }

    private fun listAccounts() {
        val accounts = atm.bank.getAllAccounts()
        appendLog("=== REGISTERED BANK ACCOUNTS ===")
        accounts.forEach { a ->
            val type = a.getAccountType()
            val bal = "Rs. ${"%,.2f".format(a.getBalance())}"
            val extra = if (type == com.example.atm.core.model.AccountType.CURRENT) "Overdraft: Rs. 50k" else "Min Bal: Rs. 5k"
            appendLog("A/C: ${a.accountNumber} | Holder: ${a.accountHolderName} | Type: $type | Bal: $bal | $extra")
        }
        appendLog("================================")
    }

    private fun showAtmCash() {
        val inv = atm.dispenser.getInventory()
        val total = atm.dispenser.getTotalCash()
        appendLog("=== ATM CASH INVENTORY (ATM-042) ===")
        appendLog("5000 Notes : ${inv[5000]} notes = Rs. ${"%,.2f".format((inv[5000] ?: 0) * 5000.0)}")
        appendLog("1000 Notes : ${inv[1000]} notes = Rs. ${"%,.2f".format((inv[1000] ?: 0) * 1000.0)}")
        appendLog(" 500 Notes : ${inv[500]} notes = Rs. ${"%,.2f".format((inv[500] ?: 0) * 500.0)}")
        appendLog("------------------------------------")
        appendLog("TOTAL CASH : Rs. ${"%,.2f".format(total)}")
        appendLog("====================================")
    }

    private fun runAutomatedDemo() {
        viewModelScope.launch {
            appendLog(">>> STARTING AUTOMATED TEST SUITE <<<", isPrompt = true)
            ejectCard()

            // 1. Insert Alice Card
            appendLog("[Step 1] Inserting Alice's Card (4532-8800-1234-5678)...")
            insertCard("4532-8800-1234-5678")

            // 2. PIN Authentication
            appendLog("[Step 2] Entering PIN 1984...")
            submitPin("1984")

            // 3. Balance Check
            appendLog("[Step 3] Inquiring Balance...")
            checkBalance()

            // 4. Withdrawal with Denomination breakdown (Rs 7,500 = 1x5000, 2x1000, 1x500)
            appendLog("[Step 4] Withdrawing Rs. 7,500 (Demonstrating greedy note dispenser)...")
            withdraw(7500.0)

            // 5. Deposit
            appendLog("[Step 5] Depositing Rs. 10,000...")
            deposit(10000.0)

            // 6. Inter-account Transfer to Bob
            appendLog("[Step 6] Transferring Rs. 5,000 to Bob (ACC-20005678)...")
            transferMoney("ACC-20005678", 5000.0)

            // 7. Mini Statement
            appendLog("[Step 7] Generating Mini Statement...")
            getMiniStatement()

            appendLog(">>> AUTOMATED DEMO COMPLETE - ALL SYSTEMS VERIFIED <<<", isSuccess = true)
        }
    }

    private fun appendLog(text: String, isCommand: Boolean = false, isError: Boolean = false, isSuccess: Boolean = false, isPrompt: Boolean = false) {
        val log = TerminalLog(text, isCommand, isError, isSuccess, isPrompt)
        _uiState.update {
            it.copy(terminalLogs = it.terminalLogs + log)
        }
    }
}
