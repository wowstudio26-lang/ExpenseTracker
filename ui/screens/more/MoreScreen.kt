package com.wowstudio.expensetracker.ui.screens.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowstudio.expensetracker.domain.model.AppTheme
import com.wowstudio.expensetracker.ui.theme.*
import com.wowstudio.expensetracker.ui.viewmodel.SettingsViewModel

@Composable
fun MoreScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = settings.userName.ifEmpty { "wowstudio26" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Synced · tracking today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Upgrade Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Upgrade to Premium",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${settings.freeTransactionsLeft} free left this month · +15 per short ad",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null)
            }
        }

        // Family & Groups
        SectionTitle("FAMILY & GROUPS")
        GradientActionCard(
            title = "Family & Group Expenses",
            subtitle = "Split bills, track trips & settle up",
            icon = Icons.Default.People,
            gradient = listOf(GradientStart1, GradientEnd1),
            onClick = { }
        )

        // Business & Trips
        SectionTitle("BUSINESS & TRIPS")
        GradientActionCard(
            title = "Business Tracker",
            subtitle = "Track business income and expenses",
            icon = Icons.Default.Business,
            gradient = listOf(GradientStart2, GradientEnd2),
            onClick = { },
            isPremium = true
        )
        GradientActionCard(
            title = "Office Trips",
            subtitle = "Trip expenses, advances & reports",
            icon = Icons.Default.Flight,
            gradient = listOf(GradientStart1, GradientEnd1),
            onClick = { },
            isPremium = true
        )

        // Refer Friends
        SectionTitle("REFER FRIENDS & FAMILY")
        SettingItem(
            icon = Icons.Default.CardGiftcard,
            title = "Refer Friends & Family",
            subtitle = "3 active friends = 1 month Premium",
            onClick = { }
        )

        // Preferences
        SectionTitle("PREFERENCES")
        SettingItem(
            icon = Icons.Default.Palette,
            title = "Theme",
            trailing = {
                Row {
                    ThemeOption("System", settings.theme == AppTheme.SYSTEM) { viewModel.updateTheme(AppTheme.SYSTEM) }
                    ThemeOption("Light", settings.theme == AppTheme.LIGHT) { viewModel.updateTheme(AppTheme.LIGHT) }
                    ThemeOption("Dark", settings.theme == AppTheme.DARK) { viewModel.updateTheme(AppTheme.DARK) }
                }
            }
        )
        SettingItem(
            icon = Icons.Default.AttachMoney,
            title = "Currency",
            subtitle = "${settings.currency} · Indian Rupee",
            trailingText = settings.currencySymbol,
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.QrCode,
            title = "Default Payment Method",
            subtitle = "New expenses start as UPI",
            onClick = { }
        )
        ToggleSettingItem(
            icon = Icons.Default.Fingerprint,
            title = "Unlock with Biometrics",
            subtitle = "Use Face ID / fingerprint for app lock",
            checked = settings.biometricsEnabled,
            onCheckedChange = { viewModel.updateSettings(settings.copy(biometricsEnabled = it)) }
        )
        ToggleSettingItem(
            icon = Icons.Default.CalendarToday,
            title = "Due Dates on Home",
            subtitle = "Show the due-dates card on Home",
            checked = settings.dueDatesOnHome,
            onCheckedChange = { viewModel.updateSettings(settings.copy(dueDatesOnHome = it)) }
        )

        // Notifications
        SectionTitle("NOTIFICATIONS")
        ToggleSettingItem(
            icon = Icons.Default.Notifications,
            title = "Daily Log Reminder",
            subtitle = "Evening reminder to add today's expenses",
            checked = settings.dailyLogReminder,
            onCheckedChange = { viewModel.updateSettings(settings.copy(dailyLogReminder = it)) }
        )
        ToggleSettingItem(
            icon = Icons.Default.LocalFireDepartment,
            title = "Habit Gap Reminders",
            subtitle = "Nudge after 1 or 2 days without expenses",
            checked = settings.habitGapReminders,
            onCheckedChange = { viewModel.updateSettings(settings.copy(habitGapReminders = it)) }
        )
        ToggleSettingItem(
            icon = Icons.Default.AccountBalanceWallet,
            title = "Budget Alerts",
            subtitle = "Monthly, category, and daily budget nudges",
            checked = settings.budgetAlerts,
            onCheckedChange = { viewModel.updateSettings(settings.copy(budgetAlerts = it)) }
        )
        ToggleSettingItem(
            icon = Icons.Default.Groups,
            title = "Group Settlement Reminders",
            subtitle = "Always on for balances you need to settle",
            checked = settings.groupSettlementReminders,
            onCheckedChange = { viewModel.updateSettings(settings.copy(groupSettlementReminders = it)) }
        )
        ToggleSettingItem(
            icon = Icons.Default.Email,
            title = "Group Emails",
            subtitle = "Expense changes, settle-up receipts and balance reminders",
            checked = settings.groupEmails,
            onCheckedChange = { viewModel.updateSettings(settings.copy(groupEmails = it)) }
        )

        // Tools & Import
        SectionTitle("TOOLS & IMPORT")
        SettingItem(
            icon = Icons.Default.CloudDone,
            title = "Backed up",
            subtitle = "Synced with cloud 4m ago",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Payment,
            title = "GPay Statement Import",
            subtitle = "India only · UPI PDF/CSV",
            badge = "Premium",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Description,
            title = "Text Import",
            subtitle = "Paste & parse transactions",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Download,
            title = "Export CSV",
            subtitle = "0 transactions ready",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Code,
            title = "Export JSON (backup)",
            subtitle = "Full backup of all your expenses",
            onClick = { }
        )

        // Budget
        SectionTitle("BUDGET")
        SettingItem(
            icon = Icons.Default.Savings,
            title = "New Savings Goal",
            subtitle = "Plan for vacation, gadgets, emergencies",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.StickyNote2,
            title = "Scratch Pad",
            subtitle = "0 quick notes",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.TrendingUp,
            title = "Forecast",
            subtitle = "AI predictions for next month",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Assessment,
            title = "Weekly Report",
            subtitle = "AI insights · Health score",
            onClick = { }
        )

        // Categories
        SectionTitle("CATEGORIES")
        SettingItem(
            icon = Icons.Default.Category,
            title = "15 categories",
            subtitle = "budgets, icons & visibility",
            trailing = {
                TextButton(onClick = onNavigateToCategories) {
                    Text("+ New")
                }
            },
            onClick = onNavigateToCategories
        )

        // About
        SectionTitle("ABOUT")
        SettingItem(
            icon = Icons.Default.Info,
            title = "Profile & Account",
            subtitle = settings.userEmail.ifEmpty { "wowstudio26@gmail.com" },
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Restore,
            title = "Restore Purchases",
            subtitle = "Recover Premium bought with this account",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Tour,
            title = "App Tour",
            subtitle = "Replay feature walkthrough",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Lightbulb,
            title = "Tips & Features",
            subtitle = "Shortcuts + everything the app can do",
            badge = "NEW",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.BugReport,
            title = "Report a Bug / Feature Request",
            subtitle = "Send us your feedback or report an issue",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.PrivacyTip,
            title = "Privacy Policy",
            subtitle = "How we handle your data",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Description,
            title = "Terms of Use",
            subtitle = "Subscriptions, acceptable use & more",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.NewReleases,
            title = "What's New",
            subtitle = "Latest features in this update",
            badge = "NEW",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.Info,
            title = "App Version",
            subtitle = "2.23.0+107",
            onClick = { }
        )
        SettingItem(
            icon = Icons.Default.History,
            title = "Tracking since",
            subtitle = "today",
            onClick = { }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun GradientActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit,
    isPremium: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(Brush.horizontalGradient(colors = gradient))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (isPremium) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "Premium",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    trailingText: String? = null,
    badge: String? = null,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = badge,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            trailing?.invoke() ?: trailingText?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
            } ?: Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ToggleSettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ThemeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
