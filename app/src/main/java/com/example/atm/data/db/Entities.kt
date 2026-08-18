package com.example.atm.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey val customerId: String,
    val name: String,
    val phone: String,
    val email: String
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountNumber: String,
    val accountHolderName: String,
    val customerId: String,
    val accountType: String, // "SAVINGS" or "CURRENT"
    val balance: Double,
    val pin: String,
    val status: String,
    val dailyWithdrawnAmount: Double,
    val dailyTransferAmount: Double,
    val lastResetDate: String
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val cardNumber: String,
    val customerId: String,
    val primaryAccountNumber: String,
    val pin: String,
    val status: String,
    val failedPinAttempts: Int
)

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val transactionId: String,
    val accountNumber: String,
    val transactionType: String,
    val amount: Double,
    val fee: Double,
    val timestamp: Long,
    val status: String,
    val failureReason: String?,
    val description: String,
    val dispensedNotesJson: String?
)

@Entity(tableName = "atm_cash")
data class AtmCashEntity(
    @PrimaryKey val atmId: String,
    val notes5000: Int,
    val notes1000: Int,
    val notes500: Int,
    val lastUpdated: Long
)

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val logId: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String,
    val entityRef: String,
    val details: String,
    val status: String
)
