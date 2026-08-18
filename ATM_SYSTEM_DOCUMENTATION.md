# ATM Banking System - Object-Oriented Architecture & System Design

## 1. System Overview & Architecture

The **Apex National Bank ATM System** is a robust, modular banking application built using **Pure Object-Oriented Programming (OOP)**, **Clean Architecture**, and **Android Jetpack Compose** backed by a **Room Relational Database (SQLite)**.

The system features both a **Hardware-Simulated ATM Terminal (GUI)** with digital CRT display, physical keypad, and note-dispenser tray, as well as an authentic **Interactive CLI Bash Console** for command-line banking.

```
┌────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER                   │
│   [ATM Terminal GUI]  [Interactive CLI]  [DB Viewer]   │
│                   AtmViewModel                         │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                      DOMAIN LAYER                      │
│   ATM (State Machine) ──► Bank ──► Customer ──► Account│
│   CashDispenser (Notes Greedy Algorithm)               │
│   Transaction (Polymorphic Execution & Receipts)       │
└───────────────────────────┬────────────────────────────┘
                            │
┌───────────────────────────▼────────────────────────────┐
│                    DATA PERSISTENCE                    │
│   AtmRepository ◄──► Room SQLite Database (DAOs)       │
│   Entities: Accounts, Cards, Txns, Cash, AuditLogs     │
└────────────────────────────────────────────────────────┘
```

---

## 2. Object-Oriented Programming (OOP) Pillars

### 2.1 Encapsulation
Encapsulation bundles data and the methods operating on that data within a single class while restricting direct access to internal state to prevent corruption.

* **Protected/Private State**: Fields such as `balance`, `pin`, `dailyWithdrawnAmount`, `dailyTransferAmount`, `failedPinAttempts`, and `cardStatus` cannot be mutated directly by external code (e.g., `account.balance = -50000` is impossible).
* **Guarded Mutators**: Balance modifications are strictly mediated via domain methods like `deposit()`, `withdraw()`, `debitTransfer()`, and `creditTransfer()`, each containing business invariant validations.
* **Security PIN Verification**: The PIN is validated through `card.validatePin(inputPin)` and mutated only via `card.changePin(oldPin, newPin)` which checks the old PIN and updates the state machine.

```kotlin
abstract class Account(
    val accountNumber: String,
    val customerId: String,
    val accountHolderName: String,
    protected var balance: Double,
    protected var pin: String,
    protected var status: AccountStatus = AccountStatus.ACTIVE
) {
    fun getBalance(): Double = balance

    fun withdraw(amount: Double): Double {
        val fee = calculateTransactionFee(TransactionType.WITHDRAWAL, amount)
        val totalDeduction = amount + fee
        val (canWithdraw, reason) = canWithdraw(totalDeduction)
        if (!canWithdraw) throw InsufficientBalanceException(reason ?: "Cannot withdraw")
        
        balance -= totalDeduction
        dailyWithdrawnAmount += amount
        return balance
    }
}
```

---

### 2.2 Inheritance
Inheritance promotes code reuse and hierarchical domain specialization:

* **`Account` (Abstract Base Class)**: Provides shared properties (`accountNumber`, `balance`, `dailyWithdrawnAmount`, `transactions`) and concrete logic for deposit, balance inquiry, and transfer debit/credit.
  * **`SavingsAccount` (Concrete Subclass)**: Specializes withdrawal rules (enforces minimum balance of Rs. 5,000, Rs. 50 transaction fee, Rs. 50,000 single txn limit).
  * **`CurrentAccount` (Concrete Subclass)**: Specializes corporate rules (allows overdraft up to -Rs. 50,000, Rs. 0 transaction fee, Rs. 100,000 single txn limit).
* **`Transaction` (Abstract Base Class)**: Provides transaction ID, timestamp, fee, and status tracking.
  * **`WithdrawalTransaction`**: Adds note denomination breakdown (`notesDispensed: Map<Int, Int>`).
  * **`DepositTransaction`**: Handles credit recording.
  * **`TransferTransaction`**: Records sender and beneficiary account references.

```
                  ┌────────────────────────┐
                  │    Account (Abstract)  │
                  └───────────┬────────────┘
                              │
               ┌──────────────┴──────────────┐
               ▼                             ▼
    ┌──────────────────────┐      ┌──────────────────────┐
    │    SavingsAccount    │      │    CurrentAccount    │
    │  - minBalance: 5,000 │      │  - overdraft: 50,000 │
    │  - fee: Rs. 50       │      │  - fee: Rs. 0        │
    └──────────────────────┘      └──────────────────────┘
```

---

### 2.3 Polymorphism
Polymorphism allows the ATM to treat all accounts and transactions through a unified interface while executing subclass-specific behaviors at runtime (Dynamic Method Dispatch):

* **Polymorphic Balance Validation (`canWithdraw`)**: When a withdrawal is requested, `account.canWithdraw(amount)` checks minimum balance for `SavingsAccount` vs overdraft allowance for `CurrentAccount`.
* **Polymorphic Fee Calculation (`calculateTransactionFee`)**: Returns Rs. 50 for savings withdrawals and Rs. 0 for business current accounts.
* **Polymorphic Receipt Generation (`formatReceipt`)**: Generates custom printed slips formatted specifically for withdrawals (displaying note counts), deposits, or inter-account transfers.

```kotlin
// Polymorphic call site inside ATM.withdraw():
val fee = currentAccount.calculateTransactionFee(TransactionType.WITHDRAWAL, amount)
val (canWithdraw, reason) = currentAccount.canWithdraw(amount + fee)
if (!canWithdraw) {
    throw InsufficientBalanceException(reason ?: "Withdrawal rejected")
}
```

---

### 2.4 Abstraction
Abstraction hides internal algorithmic and storage complexities, exposing only high-level conceptual operations:

* **`CashDispenser`**: Hides greedy note allocation and cassette inventory management behind `dispenseCash(amount)`.
* **`Bank`**: Encapsulates inter-account settlement and multi-customer directories behind `authenticate()`, `findAccount()`, `executeTransfer()`.
* **`ATM`**: Serves as a Facade coordinating card readers, pin verifiers, note dispensers, and banking backends.

---

## 3. Class Relationships & UML

```
┌──────────────┐ 1      * ┌────────────────┐ 1      * ┌──────────────┐
│   Customer   ├─────────►│    Account     ├─────────►│ Transaction  │
└──────┬───────┘          └────────────────┘          └──────────────┘
       │ 1
       │ *
┌──────▼───────┐
│     Card     │
└──────────────┘

┌──────────────┐ Composition 1 ┌────────────────┐
│     ATM      ├──────────────►│ CashDispenser  │
└──────┬───────┘               └────────────────┘
       │ Dependency
       ▼
┌──────────────┐ Aggregation * ┌────────────────┐
│     Bank     ├──────────────►│    Customer    │
└──────────────┘               └────────────────┘
```

| Relationship Type | Classes | Explanation |
| :--- | :--- | :--- |
| **Composition** | `ATM` ──► `CashDispenser` | The ATM creates and owns its internal note cassettes; the dispenser cannot exist without the ATM. |
| **Aggregation** | `Bank` ──► `Customer` / `Account` | The Bank aggregates customers and accounts; customer entities can exist independently. |
| **Association** | `Customer` ──► `Card` / `Account` | A customer is associated with one or more bank accounts and debit cards. |
| **Association** | `Account` ──► `Transaction` | An account maintains a record of its executed transactions. |
| **Dependency** | `ATM` ──► `Bank` | The ATM relies on the Bank service to authenticate PINs and process transactions. |

---

## 4. Business Logic & Security Rules

### 4.1 Card Authentication & PIN Attempt Lockout
1. A customer inserts their card (`ATMState.CARD_INSERTED`).
2. If the user enters an incorrect PIN:
   * The failed attempt counter on the Card is incremented (`failedPinAttempts++`).
   * An audit log `[PIN_FAILED]` is written to the database.
   * If `failedPinAttempts >= 3`, the card is automatically set to `CardStatus.BLOCKED`.
   * Further PIN entries or transactions are rejected until an administrator unblocks the card.

### 4.2 Greedy Cash Dispenser Algorithm
Cash is dispensed using available physical note cassettes (`Rs. 5,000`, `Rs. 1,000`, `Rs. 500`):
```kotlin
// Greedy allocation:
var remaining = amount
for (denom in listOf(5000, 1000, 500)) {
    val available = inventory[denom] ?: 0
    val needed = (remaining / denom).toInt()
    val take = minOf(needed, available)
    if (take > 0) {
        dispensed[denom] = take
        remaining -= take * denom
    }
}
if (remaining > 0) throw ATMOutOfCashException("Cannot dispense exact note combination")
```

### 4.3 Account Rules & Constraints

| Rule | Savings Account | Current (Checking) Account |
| :--- | :--- | :--- |
| **Minimum Balance** | Rs. 5,000 (Mandatory) | Rs. 0 |
| **Overdraft Limit** | None (Rs. 0) | Rs. 50,000 (Can balance down to -Rs. 50,000) |
| **Withdrawal Fee** | Rs. 50 per ATM withdrawal | Rs. 0 (Free business withdrawals) |
| **Single Txn Limit** | Rs. 50,000 | Rs. 100,000 |
| **Daily Limit** | Rs. 100,000 | Rs. 300,000 |

---

## 5. Relational Database Schema (Room SQLite)

```sql
-- Customers Table
CREATE TABLE customers (
    customerId TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    phoneNumber TEXT NOT NULL,
    email TEXT NOT NULL
);

-- Accounts Table
CREATE TABLE accounts (
    accountNumber TEXT PRIMARY KEY,
    customerId TEXT NOT NULL,
    accountHolderName TEXT NOT NULL,
    accountType TEXT NOT NULL, -- 'SAVINGS' or 'CURRENT'
    balance REAL NOT NULL,
    pin TEXT NOT NULL,
    status TEXT NOT NULL,
    dailyWithdrawnAmount REAL NOT NULL,
    dailyTransferAmount REAL NOT NULL,
    lastDailyResetTimestamp INTEGER NOT NULL
);

-- Cards Table
CREATE TABLE cards (
    cardNumber TEXT PRIMARY KEY,
    customerId TEXT NOT NULL,
    primaryAccountNumber TEXT NOT NULL,
    status TEXT NOT NULL, -- 'ACTIVE', 'BLOCKED', 'EXPIRED'
    failedPinAttempts INTEGER NOT NULL,
    expiryDate TEXT NOT NULL
);

-- Transactions Table
CREATE TABLE transactions (
    transactionId TEXT PRIMARY KEY,
    accountNumber TEXT NOT NULL,
    transactionType TEXT NOT NULL,
    amount REAL NOT NULL,
    fee REAL NOT NULL,
    timestamp INTEGER NOT NULL,
    status TEXT NOT NULL,
    description TEXT NOT NULL,
    sourceAccountNumber TEXT,
    targetAccountNumber TEXT,
    dispensedNotesJson TEXT
);

-- Security Audit Logs Table
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    timestamp INTEGER NOT NULL,
    eventType TEXT NOT NULL,
    entityRef TEXT NOT NULL,
    details TEXT NOT NULL,
    status TEXT NOT NULL
);
```

---

## 6. Interactive CLI Commands Guide

The application includes a built-in bash-like terminal under the **CLI Console** tab:

| Command | Arguments | Description | Example |
| :--- | :--- | :--- | :--- |
| `run-demo` | None | Runs automated end-to-end demo test script | `run-demo` |
| `cards` | None | Lists all bank cards and their PIN hints | `cards` |
| `accounts` | None | Displays all customer accounts & balances | `accounts` |
| `insert` | `<card-number>` | Inserts card into ATM card reader | `insert 4532-8800-1234-5678` |
| `pin` | `<4-digit-pin>` | Authenticates card PIN | `pin 1984` |
| `balance` | None | Checks current account balance | `balance` |
| `deposit` | `<amount>` | Deposits cash into active account | `deposit 10000` |
| `withdraw` | `<amount>` | Withdraws cash and dispenses notes | `withdraw 7500` |
| `transfer` | `<target-acc> <amount>` | Transfers funds to another account | `transfer ACC-20005678 3000` |
| `mini` | None | Prints mini statement (last 5 transactions) | `mini` |
| `changepin` | `<old> <new>` | Updates security PIN | `changepin 1984 4321` |
| `atm-cash` | None | Shows ATM vault cash cassette levels | `atm-cash` |
| `refill` | `[5000s] [1000s] [500s]`| Refills ATM cash cassettes | `refill 20 50 50` |
| `eject` | None | Ejects card and resets ATM to IDLE | `eject` |
| `clear` | None | Clears terminal screen | `clear` |
| `help` | None | Displays command syntax reference | `help` |

---

## 7. Demo Accounts & Pre-Seeded Test Data

| Customer | Account No | Type | Balance | Card Number | PIN | Notes |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Alice Morgan** | `ACC-10002345` | Savings | Rs. 85,000 | `4532-8800-1234-5678` | `1984` | Min balance Rs. 5k, Rs. 50 fee |
| **Alice Morgan** | `ACC-10002346` | Current | Rs. 150,000 | `4532-8800-1234-9999` | `1984` | Overdraft Rs. 50k, Rs. 0 fee |
| **Bob Henderson** | `ACC-20005678` | Savings | Rs. 42,000 | `5421-9900-8765-4321` | `2468` | Active savings account |
| **Charlie Vance** | `ACC-30009876` | Current | Rs. 12,500 | `4000-1100-3333-7788` | `7788` | Seeded with 2 failed attempts |
