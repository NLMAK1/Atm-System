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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.atm.ui.AtmUiState
import com.example.atm.ui.AtmViewModel
import com.example.atm.ui.TerminalLog
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
import com.example.ui.theme.TerminalCyan
import com.example.ui.theme.TerminalGreen
import com.example.ui.theme.TerminalRed
import com.example.ui.theme.TerminalYellow

@Composable
fun CliConsoleScreen(
    uiState: AtmUiState,
    viewModel: AtmViewModel,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.terminalLogs.size) {
        if (uiState.terminalLogs.isNotEmpty()) {
            listState.animateScrollToItem(uiState.terminalLogs.size - 1)
        }
    }

    val quickCommands = listOf(
        "run-demo",
        "help",
        "cards",
        "accounts",
        "insert 4532-8800-1234-5678",
        "pin 1984",
        "balance",
        "withdraw 7500",
        "deposit 10000",
        "mini",
        "atm-cash",
        "eject",
        "clear"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AtmNavy900)
            .padding(12.dp)
    ) {
        // Terminal Window Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = AtmNavy800)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(AtmRed))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TerminalYellow))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TerminalGreen))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ATM Bash Terminal • /dev/ttyATM0",
                        color = AtmTextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    onClick = { viewModel.executeCliCommand("run-demo") },
                    shape = RoundedCornerShape(14.dp),
                    color = AtmEmerald.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AtmEmeraldGlow)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = AtmEmeraldGlow, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Auto Test Demo", color = AtmEmeraldGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Terminal Output Screen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
            colors = CardDefaults.cardColors(containerColor = TerminalBg),
            border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                items(uiState.terminalLogs) { log ->
                    TerminalLogItem(log)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Command Suggestions Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                Surface(
                    onClick = { viewModel.executeCliCommand(cmd) },
                    shape = RoundedCornerShape(6.dp),
                    color = AtmNavy800,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AtmNavy700)
                ) {
                    Text(
                        text = "$ $cmd",
                        color = AtmCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Interactive Command Input Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.commandInput,
                onValueChange = { viewModel.onCommandInputChanged(it) },
                placeholder = { Text("Type a command (e.g. 'help', 'withdraw 5000', 'run-demo')...", fontSize = 12.sp, color = AtmTextMuted) },
                leadingIcon = {
                    Text(
                        text = "$",
                        color = TerminalGreen,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = { viewModel.executeCliCommand(uiState.commandInput) },
                        modifier = Modifier.testTag("cli_send_button")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Run", tint = TerminalGreen)
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.executeCliCommand(uiState.commandInput) }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TerminalGreen,
                    unfocusedBorderColor = AtmNavy700,
                    focusedTextColor = AtmTextPrimary,
                    unfocusedTextColor = AtmTextPrimary,
                    cursorColor = TerminalGreen
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("cli_input_field")
            )
        }
    }
}

@Composable
private fun TerminalLogItem(log: TerminalLog) {
    val color = when {
        log.isCommand -> TerminalCyan
        log.isError -> TerminalRed
        log.isSuccess -> TerminalGreen
        log.isPrompt -> TerminalYellow
        else -> AtmTextPrimary
    }

    val fontWeight = if (log.isCommand || log.isPrompt || log.isSuccess) FontWeight.Bold else FontWeight.Normal

    Text(
        text = log.text,
        color = color,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        fontWeight = fontWeight,
        lineHeight = 16.sp,
        modifier = Modifier.padding(vertical = 1.dp)
    )
}
