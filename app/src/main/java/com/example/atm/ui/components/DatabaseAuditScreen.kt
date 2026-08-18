package com.example.atm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atm.data.db.AccountEntity
import com.example.atm.data.db.AuditLogEntity
import com.example.atm.data.db.CardEntity
import com.example.atm.data.db.TransactionEntity
import com.example.atm.ui.AtmViewModel
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseAuditScreen(
    viewModel: AtmViewModel,
    modifier: Modifier = Modifier
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    var showRefillDialog by remember { mutableStateOf(false) }

    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val cards by viewModel.allCards.collectAsStateWithLifecycle()
    val transactions by viewModel.allTransactions.collectAsStateWithLifecycle()
    val auditLogs by viewModel.allAuditLogs.collectAsStateWithLifecycle()
    val atmCash by viewModel.atmCashEntity.collectAsStateWithLifecycle()

    val tabs = listOf("Accounts", "Cards", "Transactions", "Audit Logs", "ATM Cash")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AtmNavy900)
            .padding(12.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RELATIONAL DATABASE & AUDIT LOGS",
                    color = AtmTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Room SQLite Persistence • Real-time Data Integrity",
                    color = AtmCyan,
                    fontSize = 11.sp
                )
            }

            Surface(
                onClick = { showRefillDialog = true },
                shape = RoundedCornerShape(12.dp),
                color = AtmAmber.copy(alpha = 0.2f),
                border = androidx.compose.foundation.BorderStroke(1.dp, AtmAmber)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocalAtm, contentDescription = null, tint = AtmAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Refill ATM", color = AtmAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub Tab Selector
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = AtmNavy800,
            contentColor = AtmEmeraldGlow
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 12.sp,
                            fontWeight = if (selectedSubTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedSubTab == index) AtmEmeraldGlow else AtmTextSecondary
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> AccountsTableView(accounts)
                1 -> CardsTableView(cards, onUnblockCard = { viewModel.unblockCard(it) })
                2 -> TransactionsTableView(transactions)
                3 -> AuditLogsTableView(auditLogs)
                4 -> AtmCashTableView(atmCash, onRefillClick = { showRefillDialog = true })
            }
        }
    }

    if (showRefillDialog) {
        AtmRefillDialog(
            onDismiss = { showRefillDialog = false },
            onConfirm = { n5000, n1000, n500 ->
                viewModel.refillAtmCash(n5000, n1000, n500)
                showRefillDialog = false
            }
        )
    }
}

@Composable
private fun AccountsTableView(accounts: List<AccountEntity>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(accounts) { acc ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AtmNavy800),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = acc.accountHolderName, fontWeight = FontWeight.Bold, color = AtmTextPrimary, fontSize = 14.sp)
                            Text(text = "${acc.accountNumber} • ${acc.customerId}", color = AtmTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (acc.accountType == "SAVINGS") AtmEmerald.copy(alpha = 0.2f) else AtmBlue.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = acc.accountType,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (acc.accountType == "SAVINGS") AtmEmeraldGlow else AtmBlue,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = AtmNavy700)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Current Balance", color = AtmTextMuted, fontSize = 10.sp)
                            Text(text = "Rs. ${"%,.2f".format(acc.balance)}", color = AtmEmeraldGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "Daily Withdrawn / Transferred", color = AtmTextMuted, fontSize = 10.sp)
                            Text(text = "Rs. ${"%,.0f".format(acc.dailyWithdrawnAmount)} / Rs. ${"%,.0f".format(acc.dailyTransferAmount)}", color = AtmTextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardsTableView(cards: List<CardEntity>, onUnblockCard: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card ->
            val isBlocked = card.status == "BLOCKED"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AtmNavy800),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isBlocked) AtmRed.copy(alpha = 0.6f) else AtmNavy700
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = card.cardNumber, fontWeight = FontWeight.Bold, color = AtmCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "Primary A/C: ${card.primaryAccountNumber}", color = AtmTextSecondary, fontSize = 11.sp)
                        Text(text = "Customer: ${card.customerId} • PIN: ****", color = AtmTextMuted, fontSize = 10.sp)
                        if (card.failedPinAttempts > 0) {
                            Text(text = "Failed Attempts: ${card.failedPinAttempts}/3", color = if (isBlocked) AtmRed else AtmAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isBlocked) AtmRed.copy(alpha = 0.2f) else AtmEmerald.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = card.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isBlocked) AtmRed else AtmEmeraldGlow,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isBlocked) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { onUnblockCard(card.cardNumber) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmEmerald),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Unblock", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionsTableView(transactions: List<TransactionEntity>) {
    val sdf = SimpleDateFormat("dd-MMM HH:mm", Locale.US)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(transactions) { tx ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AtmNavy800),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = tx.transactionId, fontWeight = FontWeight.Bold, color = AtmTextPrimary, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "${sdf.format(Date(tx.timestamp))} • ${tx.accountNumber}", color = AtmTextMuted, fontSize = 10.sp)
                        Text(text = tx.description, color = AtmTextSecondary, fontSize = 11.sp)
                        if (!tx.dispensedNotesJson.isNullOrBlank()) {
                            Text(text = "Notes: ${tx.dispensedNotesJson}", color = TerminalGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        val isPositive = tx.transactionType.contains("DEPOSIT") || tx.transactionType.contains("CREDIT")
                        val sign = if (isPositive) "+" else "-"
                        Text(
                            text = "$sign Rs. ${"%,.2f".format(tx.amount)}",
                            fontWeight = FontWeight.Bold,
                            color = if (isPositive) TerminalGreen else AtmAmber,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (tx.fee > 0) {
                            Text(text = "Fee: Rs. ${tx.fee}", color = AtmTextMuted, fontSize = 10.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (tx.status == "SUCCESS") TerminalGreen.copy(alpha = 0.15f) else AtmRed.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = tx.status,
                                fontSize = 9.sp,
                                color = if (tx.status == "SUCCESS") TerminalGreen else AtmRed,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogsTableView(auditLogs: List<AuditLogEntity>) {
    val sdf = SimpleDateFormat("dd-MMM HH:mm:ss", Locale.US)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(auditLogs) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = TerminalBg),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (log.status == "SUCCESS") TerminalGreen else AtmRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "[${log.eventType}]",
                                color = if (log.status == "SUCCESS") AtmEmeraldGlow else AtmRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = log.entityRef,
                                color = AtmCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = log.details,
                            color = AtmTextSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        color = AtmTextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmCashTableView(
    atmCash: com.example.atm.data.db.AtmCashEntity?,
    onRefillClick: () -> Unit
) {
    val n5000 = atmCash?.notes5000 ?: 0
    val n1000 = atmCash?.notes1000 ?: 0
    val n500 = atmCash?.notes500 ?: 0
    val total = (n5000 * 5000.0) + (n1000 * 1000.0) + (n500 * 500.0)

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AtmNavy800),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ATM Vault Cash (ATM-042)", fontWeight = FontWeight.Bold, color = AtmTextPrimary, fontSize = 14.sp)
                        Text(text = "Denomination cassette balance", color = AtmTextMuted, fontSize = 11.sp)
                    }
                    Text(
                        text = "Rs. ${"%,.2f".format(total)}",
                        fontWeight = FontWeight.Bold,
                        color = AtmEmeraldGlow,
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = AtmNavy700)

                val denominations = listOf(
                    Triple("Rs. 5,000 Notes", n5000, n5000 * 5000.0),
                    Triple("Rs. 1,000 Notes", n1000, n1000 * 1000.0),
                    Triple("Rs.   500 Notes", n500, n500 * 500.0)
                )

                denominations.forEach { (label, count, subtotal) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = label, color = AtmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "$count notes", color = AtmCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text(text = "Rs. ${"%,.2f".format(subtotal)}", color = TerminalGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onRefillClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AtmAmber),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.LocalAtm, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Refill ATM Cash Cassettes", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AtmRefillDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int) -> Unit
) {
    var notes5000Input by remember { mutableStateOf("10") }
    var notes1000Input by remember { mutableStateOf("20") }
    var notes500Input by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Refill ATM Cash", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column {
                Text("Specify number of notes to add into each cassette:", color = AtmTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = notes5000Input,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) notes5000Input = it },
                    label = { Text("Rs. 5,000 Notes count") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmAmber,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes1000Input,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) notes1000Input = it },
                    label = { Text("Rs. 1,000 Notes count") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmAmber,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes500Input,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) notes500Input = it },
                    label = { Text("Rs. 500 Notes count") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmAmber,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val n5000 = notes5000Input.toIntOrNull() ?: 0
                    val n1000 = notes1000Input.toIntOrNull() ?: 0
                    val n500 = notes500Input.toIntOrNull() ?: 0
                    onConfirm(n5000, n1000, n500)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtmAmber)
            ) {
                Text("Confirm Refill", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}
