package com.example.expensetracker.ui.more

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.components.SettingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSync: () -> Unit,
    onNavigateToScan: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Tools & Import Section
            item {
                Text(
                    text = "Tools & Import",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Scan Receipt (Add this in Tools & Import section)
            item {
                SettingItem(
                    icon = Icons.Default.CameraAlt,
                    title = "Scan Receipt",
                    subtitle = "Auto-detect amount from receipt",
                    onClick = onNavigateToScan
                )
            }

            // Cloud Sync (Add this in Tools & Import section)
            item {
                SettingItem(
                    icon = Icons.Default.CloudSync,
                    title = "Cloud Sync",
                    subtitle = "Sync with Supabase",
                    onClick = onNavigateToSync
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.ImportExport,
                    title = "Export Data",
                    subtitle = "Export as CSV or JSON",
                    onClick = onNavigateToExport
                )
            }

            // Budget & Goals Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Budget & Goals",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Savings,
                    title = "Budget / Savings Goals",
                    subtitle = "Set and track your financial goals",
                    onClick = onNavigateToBudget
                )
            }

            // Preferences Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Preferences",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Settings,
                    title = "Settings",
                    subtitle = "Theme, currency, notifications",
                    onClick = onNavigateToSettings
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    subtitle = "Version, licenses, feedback",
                    onClick = onNavigateToAbout
                )
            }
        }
    }
}
