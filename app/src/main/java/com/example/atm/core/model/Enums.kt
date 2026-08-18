package com.example.atm.core.model

enum class AccountStatus {
    ACTIVE,
    BLOCKED,
    DORMANT,
    CLOSED
}

enum class CardStatus {
    ACTIVE,
    BLOCKED,
    EXPIRED
}

enum class TransactionType {
    DEPOSIT,
    WITHDRAWAL,
    TRANSFER_DEBIT,
    TRANSFER_CREDIT,
    PIN_CHANGE,
    BALANCE_INQUIRY
}

enum class TransactionStatus {
    SUCCESS,
    FAILED,
    REVERSED
}

enum class AccountType {
    SAVINGS,
    CURRENT
}
