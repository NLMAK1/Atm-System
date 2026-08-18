package com.example.atm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schema
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AtmAmber
import com.example.ui.theme.AtmBlue
import com.example.ui.theme.AtmCyan
import com.example.ui.theme.AtmEmerald
import com.example.ui.theme.AtmEmeraldGlow
import com.example.ui.theme.AtmNavy700
import com.example.ui.theme.AtmNavy800
import com.example.ui.theme.AtmNavy900
import com.example.ui.theme.AtmRed
import com.example.ui.theme.AtmTextMuted
import com.example.ui.theme.AtmTextPrimary
import com.example.ui.theme.AtmTextSecondary
import com.example.ui.theme.TerminalBg
import com.example.ui.theme.TerminalGreen

@Composable
fun ArchitectureDocsScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AtmNavy900)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        // Header
        Text(
            text = "SYSTEM DESIGN & OOP ARCHITECTURE",
            color = AtmTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Text(
            text = "Clean Architecture • Object-Oriented Principles • UML & Domain Rules",
            color = AtmCyan,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Four Pillars of OOP
        OopPrincipleCard(
            title = "1. Encapsulation",
            icon = Icons.Default.Lock,
            color = AtmEmerald,
            summary = "Sensitive fields are strictly protected/private and inaccessible to direct external tampering.",
            details = listOf(
                "account.balance cannot be set directly (e.g. balance = -50000 is forbidden).",
                "Balance changes occur only through guarded business methods: deposit(), withdraw(), debitTransfer(), creditTransfer().",
                "PIN validation and mutation happen exclusively through validatePin() and changePin().",
                "Card failed attempts and status (ACTIVE / BLOCKED) are managed within the Card state machine."
            ),
            codeSnippet = """
                abstract class Account(
                    protected var balance: Double,
                    protected var pin: String,
                    protected var status: AccountStatus
                ) {
                    fun getBalance(): Double = balance
                    fun withdraw(amount: Double): Double {
                        /* validation & business rules */
                        balance -= totalDeduction
                        return balance
                    }
                }
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OopPrincipleCard(
            title = "2. Inheritance",
            icon = Icons.Default.AccountTree,
            color = AtmCyan,
            summary = "Hierarchical code reuse and specialized account/transaction variants.",
            details = listOf(
                "Account (Base) ──► SavingsAccount (Min balance Rs. 5k, Rs. 50 fee)",
                "Account (Base) ──► CurrentAccount (Overdraft limit Rs. 50k, Rs. 0 fee)",
                "Transaction (Base) ──► WithdrawalTransaction (Denomination notes tracking)",
                "Transaction (Base) ──► DepositTransaction",
                "Transaction (Base) ──► TransferTransaction (Sender debit & Receiver credit)"
            ),
            codeSnippet = """
                class SavingsAccount(...) : Account(...) {
                    override fun getAccountType() = AccountType.SAVINGS
                    override fun calculateWithdrawalLimit() = 50_000.0
                    override fun getMinimumBalance() = 5_000.0
                }
                
                class CurrentAccount(...) : Account(...) {
                    override fun getAccountType() = AccountType.CURRENT
                    override fun getOverdraftLimit() = 50_000.0
                }
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OopPrincipleCard(
            title = "3. Polymorphism",
            icon = Icons.Default.DataObject,
            color = AtmAmber,
            summary = "Uniform interfaces with dynamic method dispatch based on runtime object type.",
            details = listOf(
                "account.calculateWithdrawalLimit() returns Rs. 50,000 for Savings and Rs. 100,000 for Current.",
                "account.canWithdraw(amount) enforces Rs. 5,000 minimum balance for Savings, but allows negative balance up to -Rs. 50,000 for Current.",
                "account.calculateTransactionFee(type, amount) returns Rs. 50 for Savings withdrawal vs Rs. 0 for Current withdrawal.",
                "transaction.formatReceipt() dynamically generates customized slips with notes breakdown for withdrawals or sender/receiver info for transfers."
            ),
            codeSnippet = """
                // Polymorphic Call Site in ATM:
                val fee = account.calculateTransactionFee(TransactionType.WITHDRAWAL, amount)
                val (canWithdraw, reason) = account.canWithdraw(amount + fee)
                if (!canWithdraw) throw InsufficientBalanceError(...)
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OopPrincipleCard(
            title = "4. Abstraction",
            icon = Icons.Default.Schema,
            color = AtmBlue,
            summary = "Contract-driven design hiding internal complexities from client code.",
            details = listOf(
                "Abstract Account class defines core banking contracts without exposing underlying balance storage.",
                "Abstract Transaction class hides serialization, receipt formatting, and accounting debit/credit calculations.",
                "ATM class acts as a high-level Facade/Coordinator between CardReader, CashDispenser, and Bank services."
            ),
            codeSnippet = """
                abstract class Transaction(...) {
                    abstract fun getTransactionType(): TransactionType
                    abstract fun getDisplayType(): String
                    abstract fun getSignPrefix(): String
                    open fun formatReceipt(): String { ... }
                }
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Class Relationships Diagram Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AtmNavy800),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CLASS RELATIONSHIPS & DOMAIN ARCHITECTURE",
                    color = AtmEmeraldGlow,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val relationships = listOf(
                    "Composition" to "ATM *has a* CashDispenser (inventory of notes 5000, 1000, 500).",
                    "Aggregation" to "Bank *has many* Customers, Accounts, Cards, and ATMs.",
                    "Association" to "Customer *has* 1..* Bank Accounts and 1..* ATM Cards.",
                    "Association" to "Account *records many* Transactions.",
                    "Dependency" to "ATM *uses* Bank and CashDispenser to execute transactional flows."
                )

                relationships.forEach { (type, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "[$type]",
                            color = AtmCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.width(110.dp)
                        )
                        Text(
                            text = desc,
                            color = AtmTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Business Logic & Security Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AtmNavy800),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CORE BUSINESS RULES & CONSTRAINTS",
                    color = AtmAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val rules = listOf(
                    "3 PIN Attempts Limit" to "Max 3 incorrect PIN entries automatically transitions Card status to BLOCKED.",
                    "Greedy Cash Dispenser" to "Calculates optimal note counts across 5000, 1000, 500 cassettes (e.g. Rs. 7,500 = 1x5000 + 2x1000 + 1x500).",
                    "Daily Withdrawal Limits" to "Savings: Rs. 100,000/day. Current: Rs. 300,000/day. Automatically resets on new date.",
                    "Inter-Account Transfer" to "Atomic debit from sender + credit to receiver with validation preventing self-transfers or inactive accounts.",
                    "Room Relational Storage" to "Complete audit trail with full transaction histories, card states, and vault cash persisted in SQLite."
                )

                rules.forEach { (rule, desc) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = rule, color = AtmTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(text = desc, color = AtmTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OopPrincipleCard(
    title: String,
    icon: ImageVector,
    color: Color,
    summary: String,
    details: List<String>,
    codeSnippet: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AtmNavy800),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = summary,
                color = AtmTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            details.forEach { detail ->
                Row(
                    modifier = Modifier.padding(vertical = 1.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text(text = "• ", color = color, fontSize = 12.sp)
                    Text(text = detail, color = AtmTextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                color = TerminalBg,
                border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
            ) {
                Text(
                    text = codeSnippet,
                    color = TerminalGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 13.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}
