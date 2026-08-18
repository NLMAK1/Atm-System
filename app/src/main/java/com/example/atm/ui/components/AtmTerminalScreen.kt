package com.example.atm.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atm.core.model.ATMState
import com.example.atm.core.model.AccountType
import com.example.atm.data.db.CardEntity
import com.example.atm.data.db.AccountEntity
import com.example.atm.ui.AtmUiState
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

@Composable
fun AtmTerminalScreen(
    uiState: AtmUiState,
    viewModel: AtmViewModel,
    modifier: Modifier = Modifier
) {
    var showDepositDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showMiniStatementDialog by remember { mutableStateOf(false) }
    var showCardPickerSheet by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AtmNavy900)
            .padding(12.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ATM Machine Top Bezel & Status
        AtmHeaderBezel(
            atmState = uiState.atmState,
            insertedCardNumber = uiState.insertedCard?.cardNumber,
            onCardSlotClick = { showCardPickerSheet = true },
            onEjectClick = { viewModel.ejectCard() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Main Digital CRT Display
        AtmMainScreenDisplay(
            uiState = uiState,
            viewModel = viewModel,
            onActionSelected = { actionId ->
                when (actionId) {
                    1 -> viewModel.checkBalance()
                    2 -> showDepositDialog = true
                    3 -> showWithdrawDialog = true
                    4 -> showTransferDialog = true
                    5 -> showChangePinDialog = true
                    6 -> {
                        viewModel.getMiniStatement()
                        showMiniStatementDialog = true
                    }
                    7 -> viewModel.ejectCard()
                }
            },
            onCardPickerClick = { showCardPickerSheet = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Cash Dispenser Slot & Dispensed Notes Notification
        CashDispenserSlot(
            lastDispensedNotes = uiState.lastDispensedNotes,
            onClearNotes = { viewModel.clearMessages() }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Physical ATM Keypad
        AtmHardwareKeypad(
            pinInput = uiState.pinInput,
            onDigitClick = { digit ->
                if (uiState.atmState == ATMState.CARD_INSERTED) {
                    if (uiState.pinInput.length < 4) {
                        viewModel.onPinInputChanged(uiState.pinInput + digit)
                    }
                }
            },
            onClearClick = { viewModel.onPinInputChanged("") },
            onCancelClick = { viewModel.ejectCard() },
            onEnterClick = {
                if (uiState.atmState == ATMState.CARD_INSERTED && uiState.pinInput.isNotEmpty()) {
                    viewModel.submitPin()
                }
            },
            isEnterEnabled = uiState.atmState == ATMState.CARD_INSERTED && uiState.pinInput.length == 4
        )
    }

    // Dialogs
    if (showDepositDialog) {
        DepositDialog(
            onDismiss = { showDepositDialog = false },
            onConfirm = { amount ->
                viewModel.deposit(amount)
                showDepositDialog = false
            }
        )
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            accountType = uiState.currentAccount?.getAccountType() ?: AccountType.SAVINGS,
            onDismiss = { showWithdrawDialog = false },
            onConfirm = { amount ->
                viewModel.withdraw(amount)
                showWithdrawDialog = false
            }
        )
    }

    if (showTransferDialog) {
        TransferDialog(
            currentAccountNo = uiState.currentAccount?.accountNumber ?: "",
            onDismiss = { showTransferDialog = false },
            onConfirm = { targetAcc, amount ->
                viewModel.transferMoney(targetAcc, amount)
                showTransferDialog = false
            }
        )
    }

    if (showChangePinDialog) {
        ChangePinDialog(
            onDismiss = { showChangePinDialog = false },
            onConfirm = { oldPin, newPin ->
                viewModel.changePin(oldPin, newPin)
                showChangePinDialog = false
            }
        )
    }

    if (showMiniStatementDialog || uiState.miniStatement != null) {
        MiniStatementDialog(
            accountNumber = uiState.currentAccount?.accountNumber ?: "",
            accountType = uiState.currentAccount?.getAccountType()?.name ?: "",
            balance = uiState.currentAccount?.getBalance() ?: 0.0,
            transactions = uiState.miniStatement ?: emptyList(),
            onDismiss = {
                showMiniStatementDialog = false
                viewModel.clearMiniStatement()
                viewModel.clearMessages()
            }
        )
    }

    if (uiState.activeReceipt != null) {
        ReceiptSlipDialog(
            receiptText = uiState.activeReceipt,
            onDismiss = {
                viewModel.clearReceipt()
                viewModel.clearMessages()
            }
        )
    }

    if (showCardPickerSheet) {
        CardPickerModal(
            onDismiss = { showCardPickerSheet = false },
            onCardSelected = { cardNo ->
                viewModel.insertCard(cardNo)
                showCardPickerSheet = false
            }
        )
    }
}

@Composable
private fun AtmHeaderBezel(
    atmState: ATMState,
    insertedCardNumber: String?,
    onCardSlotClick: () -> Unit,
    onEjectClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtmNavy800),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AtmEmerald.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "Bank",
                        tint = AtmEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "APEX NATIONAL BANK",
                        fontWeight = FontWeight.Bold,
                        color = AtmTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Terminal ATM-042 • Main Branch",
                        color = AtmEmeraldGlow,
                        fontSize = 11.sp
                    )
                }
            }

            // Card Insertion Status Slot Button
            Surface(
                onClick = {
                    if (atmState == ATMState.IDLE) onCardSlotClick()
                    else onEjectClick()
                },
                shape = RoundedCornerShape(20.dp),
                color = if (insertedCardNumber != null) AtmEmerald.copy(alpha = 0.2f) else AtmNavy700,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (insertedCardNumber != null) AtmEmerald else AtmCyan.copy(alpha = 0.5f)
                ),
                modifier = Modifier.testTag("card_slot_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (insertedCardNumber != null) Icons.Default.CreditCard else Icons.Default.ArrowDownward,
                        contentDescription = "Card Slot",
                        tint = if (insertedCardNumber != null) AtmEmeraldGlow else AtmCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (insertedCardNumber != null) "Eject Card" else "Insert Card",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (insertedCardNumber != null) AtmEmeraldGlow else AtmTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmMainScreenDisplay(
    uiState: AtmUiState,
    viewModel: AtmViewModel,
    onActionSelected: (Int) -> Unit,
    onCardPickerClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TerminalBg),
        border = androidx.compose.foundation.BorderStroke(2.dp, AtmNavy700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Screen Header Scanline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (uiState.atmState) {
                                    ATMState.AUTHENTICATED -> TerminalGreen
                                    ATMState.CARD_BLOCKED -> AtmRed
                                    ATMState.CARD_INSERTED -> AtmAmber
                                    else -> AtmCyan
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE CRT v2.4",
                        color = TerminalGreen.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = when (uiState.atmState) {
                        ATMState.IDLE -> "[STANDBY]"
                        ATMState.CARD_INSERTED -> "[AUTH REQUIRED]"
                        ATMState.ACCOUNT_SELECTION -> "[SELECT A/C]"
                        ATMState.AUTHENTICATED -> "[SESSION ACTIVE]"
                        ATMState.CARD_BLOCKED -> "[BLOCKED]"
                    },
                    color = when (uiState.atmState) {
                        ATMState.AUTHENTICATED -> TerminalGreen
                        ATMState.CARD_BLOCKED -> AtmRed
                        else -> AtmAmber
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = TerminalGreen.copy(alpha = 0.2f),
                thickness = 1.dp
            )

            // Screen Dynamic Content
            when (uiState.atmState) {
                ATMState.IDLE -> {
                    AtmIdleScreen(onInsertCard = onCardPickerClick)
                }
                ATMState.CARD_INSERTED -> {
                    AtmPinEntryScreen(
                        card = uiState.insertedCard,
                        pinInput = uiState.pinInput,
                        errorMessage = uiState.errorMessage,
                        onSubmitPin = { viewModel.submitPin() },
                        onCancel = { viewModel.ejectCard() }
                    )
                }
                ATMState.ACCOUNT_SELECTION -> {
                    AtmAccountSelectionScreen(
                        customer = uiState.currentCustomer,
                        onSelectAccount = { accNo -> viewModel.selectAccount(accNo) }
                    )
                }
                ATMState.AUTHENTICATED -> {
                    AtmAuthenticatedMenuScreen(
                        customer = uiState.currentCustomer,
                        account = uiState.currentAccount,
                        successMessage = uiState.successMessage,
                        errorMessage = uiState.errorMessage,
                        onActionSelected = onActionSelected
                    )
                }
                ATMState.CARD_BLOCKED -> {
                    AtmCardBlockedScreen(
                        cardNumber = uiState.insertedCard?.cardNumber,
                        errorMessage = uiState.errorMessage,
                        onUnblock = {
                            uiState.insertedCard?.let { viewModel.unblockCard(it.cardNumber) }
                        },
                        onEject = { viewModel.ejectCard() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmIdleScreen(onInsertCard: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(AtmNavy800)
                .border(1.dp, AtmCyan.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = AtmCyan,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "WELCOME TO APEX BANK",
            color = AtmTextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Text(
            text = "Please insert your ATM card to start",
            color = AtmTextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onInsertCard,
            colors = ButtonDefaults.buttonColors(containerColor = AtmEmerald),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("insert_card_preset_button")
        ) {
            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Select & Insert Demo Card", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AtmPinEntryScreen(
    card: com.example.atm.core.model.Card?,
    pinInput: String,
    errorMessage: String?,
    onSubmitPin: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SECURITY AUTHENTICATION",
            color = AtmAmber,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Card: ${card?.cardNumber ?: "Unknown"}",
            color = AtmTextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )

        if (card != null && card.getFailedAttempts() > 0) {
            Text(
                text = "Warning: ${card.getFailedAttempts()}/3 Failed attempts logged!",
                color = AtmRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ENTER 4-DIGIT PIN",
            color = TerminalGreen,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Masked PIN dots display
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until 4) {
                val isFilled = i < pinInput.length
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isFilled) TerminalGreen.copy(alpha = 0.3f) else AtmNavy800)
                        .border(
                            1.5.dp,
                            if (isFilled) TerminalGreen else AtmNavy700,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(TerminalGreen)
                        )
                    }
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtmRed.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AtmRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = errorMessage, color = AtmRed, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmTextSecondary)
            ) {
                Text("Cancel & Eject")
            }

            Button(
                onClick = onSubmitPin,
                modifier = Modifier
                    .weight(1f)
                    .testTag("submit_pin_button"),
                enabled = pinInput.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen)
            ) {
                Text("Enter PIN", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AtmAccountSelectionScreen(
    customer: com.example.atm.core.model.Customer?,
    onSelectAccount: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = "SELECT ACCOUNT TO ACCESS",
            color = AtmEmeraldGlow,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Customer: ${customer?.name ?: "Valued Customer"}",
            color = AtmTextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        customer?.accounts?.forEach { acc ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelectAccount(acc.accountNumber) },
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
                        Text(
                            text = "${acc.getAccountType()} Account",
                            fontWeight = FontWeight.Bold,
                            color = AtmTextPrimary,
                            fontSize = 14.sp
                        )
                        Text(
                            text = acc.accountNumber,
                            color = AtmTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "Rs. ${"%,.2f".format(acc.getBalance())}",
                        fontWeight = FontWeight.Bold,
                        color = AtmEmeraldGlow,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AtmAuthenticatedMenuScreen(
    customer: com.example.atm.core.model.Customer?,
    account: com.example.atm.core.model.Account?,
    successMessage: String?,
    errorMessage: String?,
    onActionSelected: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Customer & Account Header Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AtmNavy800, RoundedCornerShape(10.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = customer?.name ?: "Customer",
                    fontWeight = FontWeight.Bold,
                    color = AtmTextPrimary,
                    fontSize = 13.sp
                )
                Text(
                    text = "${account?.accountNumber} • ${account?.getAccountType()}",
                    color = AtmEmeraldGlow,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Balance",
                    color = AtmTextMuted,
                    fontSize = 10.sp
                )
                Text(
                    text = "Rs. ${"%,.2f".format(account?.getBalance() ?: 0.0)}",
                    fontWeight = FontWeight.Bold,
                    color = AtmTextPrimary,
                    fontSize = 14.sp
                )
            }
        }

        if (successMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage,
                color = TerminalGreen,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalGreen.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = AtmRed,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AtmRed.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "SELECT TRANSACTION:",
            color = AtmCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 7 Menu Options Grid
        val menuOptions = listOf(
            MenuOption(1, "1. Check Balance", Icons.Default.AccountBalanceWallet, AtmCyan),
            MenuOption(2, "2. Deposit", Icons.Default.ArrowUpward, AtmEmerald),
            MenuOption(3, "3. Withdraw", Icons.Default.ArrowDownward, AtmAmber),
            MenuOption(4, "4. Transfer Money", Icons.Default.Send, AtmBlue),
            MenuOption(5, "5. Change PIN", Icons.Default.Password, AtmCyan),
            MenuOption(6, "6. Mini Statement", Icons.Default.Receipt, AtmEmeraldGlow),
            MenuOption(7, "7. Exit / Eject", Icons.Default.ExitToApp, AtmRed)
        )

        for (i in 0 until menuOptions.size step 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val opt1 = menuOptions[i]
                AtmMenuButton(
                    option = opt1,
                    modifier = Modifier.weight(1f),
                    onClick = { onActionSelected(opt1.id) }
                )

                if (i + 1 < menuOptions.size) {
                    val opt2 = menuOptions[i + 1]
                    AtmMenuButton(
                        option = opt2,
                        modifier = Modifier.weight(1f),
                        onClick = { onActionSelected(opt2.id) }
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private data class MenuOption(
    val id: Int,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun AtmMenuButton(
    option: MenuOption,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = AtmNavy800,
        border = androidx.compose.foundation.BorderStroke(1.dp, option.color.copy(alpha = 0.4f)),
        modifier = modifier
            .height(48.dp)
            .testTag("atm_menu_btn_${option.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = option.color,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = option.title,
                color = AtmTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AtmCardBlockedScreen(
    cardNumber: String?,
    errorMessage: String?,
    onUnblock: () -> Unit,
    onEject: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(AtmRed.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = AtmRed, modifier = Modifier.size(32.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "CARD IS BLOCKED",
            color = AtmRed,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Card: $cardNumber\n3 Failed PIN attempts exceeded. This card has been disabled to protect your account.",
            color = AtmTextSecondary,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onUnblock,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AtmEmerald)
            ) {
                Text("Unblock (Demo Admin)")
            }

            Button(
                onClick = onEject,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AtmNavy700)
            ) {
                Text("Eject Card")
            }
        }
    }
}

@Composable
private fun CashDispenserSlot(
    lastDispensedNotes: Map<Int, Int>?,
    onClearNotes: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AtmNavy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (lastDispensedNotes != null) TerminalGreen else AtmCyan.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CASH DISPENSER TRAY",
                        color = AtmTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (lastDispensedNotes != null) {
                    TextButton(onClick = onClearNotes) {
                        Text("Take Cash", color = AtmEmeraldGlow, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Physical cash exit slit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black)
                    .border(
                        1.dp,
                        if (lastDispensedNotes != null) AtmEmeraldGlow else AtmNavy700,
                        RoundedCornerShape(6.dp)
                    )
            )

            // Dispensed Notes presentation
            AnimatedVisibility(
                visible = lastDispensedNotes != null && lastDispensedNotes.isNotEmpty(),
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200))
            ) {
                if (lastDispensedNotes != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Cash Dispensed Successfully! Please Collect Notes:",
                            color = AtmEmeraldGlow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            lastDispensedNotes.entries.sortedByDescending { it.key }.forEach { (denom, count) ->
                                if (count > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (denom) {
                                            5000 -> Color(0xFF14532D)
                                            1000 -> Color(0xFF1E3A8A)
                                            else -> Color(0xFF78350F)
                                        },
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AtmEmeraldGlow.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = "Rs. $denom × $count",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AtmHardwareKeypad(
    pinInput: String,
    onDigitClick: (String) -> Unit,
    onClearClick: () -> Unit,
    onCancelClick: () -> Unit,
    onEnterClick: () -> Unit,
    isEnterEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AtmNavy800),
        border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ATM HARDWARE KEYPAD",
                color = AtmTextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val keypadRows = listOf(
                listOf("1", "2", "3", "CANCEL"),
                listOf("4", "5", "6", "CLEAR"),
                listOf("7", "8", "9", "ENTER"),
                listOf("", "0", "", "")
            )

            for (row in keypadRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (key in row) {
                        if (key.isEmpty()) {
                            Spacer(modifier = Modifier.weight(1f))
                        } else {
                            KeypadButton(
                                label = key,
                                modifier = Modifier.weight(1f),
                                isAction = key in listOf("CANCEL", "CLEAR", "ENTER"),
                                actionColor = when (key) {
                                    "CANCEL" -> AtmRed
                                    "CLEAR" -> AtmAmber
                                    "ENTER" -> TerminalGreen
                                    else -> AtmTextPrimary
                                },
                                isEnabled = if (key == "ENTER") isEnterEnabled else true,
                                onClick = {
                                    when (key) {
                                        "CANCEL" -> onCancelClick()
                                        "CLEAR" -> onClearClick()
                                        "ENTER" -> onEnterClick()
                                        else -> onDigitClick(key)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    label: String,
    modifier: Modifier = Modifier,
    isAction: Boolean = false,
    actionColor: Color = AtmTextPrimary,
    isEnabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = isEnabled,
        shape = RoundedCornerShape(8.dp),
        color = if (isAction) actionColor.copy(alpha = if (isEnabled) 0.2f else 0.05f) else AtmNavy700,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isAction) actionColor.copy(alpha = if (isEnabled) 0.6f else 0.2f) else AtmNavy900
        ),
        modifier = modifier
            .height(44.dp)
            .testTag("keypad_$label")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = if (isAction) actionColor else AtmTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = if (isAction) 11.sp else 16.sp,
                fontFamily = if (!isAction) FontFamily.Monospace else FontFamily.Default
            )
        }
    }
}

// ==========================================
// ACTION DIALOGS (Deposit, Withdraw, Transfer, PIN, Receipts)
// ==========================================

@Composable
fun DepositDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    val quickAmounts = listOf(5000.0, 10000.0, 20000.0, 50000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Deposit Cash", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column {
                Text("Enter amount to deposit into your account:", color = AtmTextSecondary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) amountInput = it },
                    label = { Text("Amount (Rs.)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmEmerald,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Quick Select:", fontSize = 12.sp, color = AtmTextMuted)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickAmounts.forEach { amt ->
                        Surface(
                            onClick = { amountInput = amt.toInt().toString() },
                            shape = RoundedCornerShape(6.dp),
                            color = AtmNavy800,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "${amt.toInt() / 1000}k",
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 11.sp,
                                color = AtmEmeraldGlow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull()
                    if (amt != null && amt > 0) onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtmEmerald),
                modifier = Modifier.testTag("confirm_deposit_btn")
            ) {
                Text("Deposit", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun WithdrawDialog(
    accountType: AccountType,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountInput by remember { mutableStateOf("") }
    val quickAmounts = listOf(500.0, 1000.0, 2500.0, 5000.0, 7500.0, 10000.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Withdraw Cash", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column {
                Text(
                    text = "Withdrawal Rules:\n• Multiples of Rs. 500 only (Notes: 5000, 1000, 500)\n" +
                            (if (accountType == AccountType.SAVINGS) "• Savings Min Balance: Rs. 5,000 | Max Txn: Rs. 50,000 | Fee: Rs. 50"
                            else "• Current Overdraft: Rs. 50,000 | Max Txn: Rs. 100,000 | Fee: Rs. 0"),
                    color = AtmTextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) amountInput = it },
                    label = { Text("Amount (Rs.)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmAmber,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("Quick Denomination Presets:", fontSize = 11.sp, color = AtmTextMuted)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 0 until quickAmounts.size step 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (j in i until minOf(i + 3, quickAmounts.size)) {
                                val amt = quickAmounts[j]
                                Surface(
                                    onClick = { amountInput = amt.toInt().toString() },
                                    shape = RoundedCornerShape(6.dp),
                                    color = AtmNavy800,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Rs. ${amt.toInt()}",
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 11.sp,
                                        color = AtmAmber,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountInput.toDoubleOrNull()
                    if (amt != null && amt > 0) onConfirm(amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtmAmber),
                modifier = Modifier.testTag("confirm_withdraw_btn")
            ) {
                Text("Withdraw", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun TransferDialog(
    currentAccountNo: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var targetAccount by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }

    val presetBeneficiaries = listOf(
        Pair("ACC-20005678", "Bob Henderson"),
        Pair("ACC-30009876", "Charlie Vance"),
        Pair("ACC-10002346", "Alice (Biz Account)")
    ).filter { it.first != currentAccountNo }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Transfer Funds", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column {
                Text("Select or enter destination bank account:", color = AtmTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                presetBeneficiaries.forEach { (accNo, name) ->
                    Surface(
                        onClick = { targetAccount = accNo },
                        shape = RoundedCornerShape(6.dp),
                        color = if (targetAccount == accNo) AtmBlue.copy(alpha = 0.2f) else AtmNavy800,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (targetAccount == accNo) AtmBlue else AtmNavy700
                        ),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = name, fontSize = 12.sp, color = AtmTextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(text = accNo, fontSize = 11.sp, color = AtmTextMuted, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = targetAccount,
                    onValueChange = { targetAccount = it },
                    label = { Text("Target Account Number") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmBlue,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { if (it.all { ch -> ch.isDigit() }) amountInput = it },
                    label = { Text("Amount (Rs.)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmBlue,
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
                    val amt = amountInput.toDoubleOrNull()
                    if (targetAccount.isNotBlank() && amt != null && amt > 0) onConfirm(targetAccount, amt)
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtmBlue)
            ) {
                Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun ChangePinDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmNewPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change ATM PIN", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column {
                Text("Enter existing PIN and choose a new 4-digit PIN:", color = AtmTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) oldPin = it },
                    label = { Text("Current 4-Digit PIN") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmCyan,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) newPin = it },
                    label = { Text("New 4-Digit PIN") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmCyan,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirmNewPin,
                    onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) confirmNewPin = it },
                    label = { Text("Confirm New PIN") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmCyan,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = errorMsg!!, color = AtmRed, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (oldPin.length != 4 || newPin.length != 4) {
                        errorMsg = "PIN must be 4 digits"
                    } else if (newPin != confirmNewPin) {
                        errorMsg = "New PIN confirmation does not match"
                    } else {
                        onConfirm(oldPin, newPin)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AtmCyan)
            ) {
                Text("Change PIN", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun MiniStatementDialog(
    accountNumber: String,
    accountType: String,
    balance: Double,
    transactions: List<com.example.atm.core.model.Transaction>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Mini Statement (Last 5)", fontWeight = FontWeight.Bold, color = AtmTextPrimary)
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AtmTextSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TerminalBg, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text("Account: $accountNumber ($accountType)", color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text("Current Balance: Rs. ${"%,.2f".format(balance)}", color = AtmTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = TerminalGreen.copy(alpha = 0.3f))

                if (transactions.isEmpty()) {
                    Text("No transactions found.", color = AtmTextMuted, fontSize = 12.sp)
                } else {
                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${tx.getFormattedDateTime()} • ${tx.getDisplayType()}",
                                    color = AtmTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (tx.fee > 0) {
                                    Text(
                                        text = "Fee: Rs. ${tx.fee}",
                                        color = AtmTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            Text(
                                text = "${tx.getSignPrefix()}Rs. ${"%,.2f".format(tx.amount)}",
                                color = if (tx.getSignPrefix() == "+") TerminalGreen else AtmAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AtmNavy700)
            ) {
                Text("Close", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = AtmTextSecondary)
            }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun ReceiptSlipDialog(
    receiptText: String,
    onDismiss: () -> Unit
) {
    val isBalanceSlip = receiptText.contains("BALANCE INQUIRY")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isBalanceSlip) "Balance Slip" else "Transaction Receipt",
                    fontWeight = FontWeight.Bold,
                    color = AtmTextPrimary
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = AtmTextSecondary
                    )
                }
            }
        },
        text = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC)
            ) {
                Text(
                    text = receiptText,
                    color = Color(0xFF0F172A),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AtmEmerald)
            ) {
                Text("Close Slip", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = AtmTextSecondary)
            }
        },
        containerColor = AtmNavy800
    )
}

@Composable
fun CardPickerModal(
    onDismiss: () -> Unit,
    onCardSelected: (String) -> Unit
) {
    var customCardNo by remember { mutableStateOf("") }

    val presetCards = listOf(
        PresetCardItem("4532-8800-1234-5678", "Alice Morgan", "Savings A/C", "PIN: 1984", "Active"),
        PresetCardItem("4532-8800-1234-9999", "Alice Morgan", "Current A/C", "PIN: 1984", "Active"),
        PresetCardItem("5421-9900-8765-4321", "Bob Henderson", "Savings A/C", "PIN: 2468", "Active"),
        PresetCardItem("4000-1100-3333-7788", "Charlie Vance", "Current A/C", "PIN: 7788", "2 Failed Attempts")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Card to Insert", fontWeight = FontWeight.Bold, color = AtmTextPrimary) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Choose a demo customer card:", color = AtmTextSecondary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                presetCards.forEach { card ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clickable { onCardSelected(card.number) },
                        colors = CardDefaults.cardColors(containerColor = AtmNavy700),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = card.holder, fontWeight = FontWeight.Bold, color = AtmTextPrimary, fontSize = 13.sp)
                                Text(text = card.number, color = AtmCyan, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(text = "${card.accType} • ${card.pinHint}", color = AtmTextMuted, fontSize = 10.sp)
                            }
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (card.status.contains("Failed")) AtmAmber.copy(alpha = 0.2f) else AtmEmerald.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = card.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (card.status.contains("Failed")) AtmAmber else AtmEmeraldGlow,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text("Or enter manual Card Number:", color = AtmTextMuted, fontSize = 11.sp)
                OutlinedTextField(
                    value = customCardNo,
                    onValueChange = { customCardNo = it },
                    placeholder = { Text("Card Number") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AtmEmerald,
                        unfocusedBorderColor = AtmNavy700,
                        focusedTextColor = AtmTextPrimary,
                        unfocusedTextColor = AtmTextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            if (customCardNo.isNotBlank()) {
                Button(
                    onClick = { onCardSelected(customCardNo) },
                    colors = ButtonDefaults.buttonColors(containerColor = AtmEmerald)
                ) {
                    Text("Insert Manual Card", color = Color.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AtmTextSecondary) }
        },
        containerColor = AtmNavy800
    )
}

private data class PresetCardItem(
    val number: String,
    val holder: String,
    val accType: String,
    val pinHint: String,
    val status: String
)
