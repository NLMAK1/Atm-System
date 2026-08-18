package com.example.atm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.atm.ui.AtmViewModel
import com.example.ui.theme.AtmCyan
import com.example.ui.theme.AtmEmerald
import com.example.ui.theme.AtmEmeraldGlow
import com.example.ui.theme.AtmNavy800
import com.example.ui.theme.AtmNavy900
import com.example.ui.theme.AtmTextPrimary
import com.example.ui.theme.AtmTextSecondary
import com.example.ui.theme.ElegantBorder
import com.example.ui.theme.ElegantPurpleDark

private data class NavItem(
    val title: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun AtmMainScreen(
    viewModel: AtmViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val navItems = listOf(
        NavItem("ATM Screen", Icons.Default.CreditCard, "nav_atm_screen"),
        NavItem("CLI Console", Icons.Default.Terminal, "nav_cli_console"),
        NavItem("Database & Logs", Icons.Default.Storage, "nav_db_logs"),
        NavItem("OOP Docs", Icons.Default.MenuBook, "nav_oop_docs")
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = AtmNavy800,
                contentColor = AtmEmeraldGlow,
                tonalElevation = 6.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ElegantPurpleDark,
                            selectedTextColor = AtmEmerald,
                            unselectedIconColor = AtmTextSecondary,
                            unselectedTextColor = AtmTextSecondary,
                            indicatorColor = AtmEmerald
                        ),
                        modifier = Modifier.testTag(item.testTag)
                    )
                }
            }
        },
        containerColor = AtmNavy900
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> AtmTerminalScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                1 -> CliConsoleScreen(
                    uiState = uiState,
                    viewModel = viewModel
                )
                2 -> DatabaseAuditScreen(
                    viewModel = viewModel
                )
                3 -> ArchitectureDocsScreen()
            }
        }
    }
}
