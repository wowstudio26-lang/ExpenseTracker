package com.wowstudio.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wowstudio.expensetracker.data.*
import java.text.NumberFormat
import java.util.Locale

private val RefBg = Color(0xFFF8F9FC)
private val RefCard = Color.White
private val RefBlue = Color(0xFF0878E8)
private val RefBlue2 = Color(0xFF0B62C8)
private val RefText = Color(0xFF242832)
private val RefMuted = Color(0xFF727985)
private val RefLine = Color(0xFFE8EAF0)
private val RefGreen = Color(0xFF19B79E)
private val RefPurple = Color(0xFF8457E8)
private val RefRed = Color(0xFFE85A6A)

class MainActivityReference : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ReferenceExpenseApp() }
    }

    @Composable
    private fun ReferenceExpenseApp() {
        val repo = remember { FinanceRepository(this@MainActivityReference) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var refresh by remember { mutableIntStateOf(0) }
        var showAdd by remember { mutableStateOf(false) }
        val transactions = remember(refresh) { repo.transactions() }
        val loans = remember(refresh) { repo.loans() }
        val categories = remember(refresh) { repo.categories() }
        val income = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val contribution = transactions.filter { it.type == TransactionType.CONTRIBUTION }.sumOf { it.amount }
        val debt = loans.sumOf { it.remainingAmount }

        MaterialTheme(colorScheme = lightColorScheme(primary = RefBlue, background = RefBg, surface = RefCard)) {
            Scaffold(
                containerColor = RefBg,
                bottomBar = { ReferenceBottomBar(tab) { tab = it } }
            ) { pad ->
                when (tab) {
                    0 -> Home(pad, income, expenses, contribution, debt, transactions, { showAdd = true })
                    1 -> History(pad, transactions, { repo.deleteTransaction(it); refresh++ })
                    2 -> AddExpense(pad, categories, { t -> repo.addTransaction(t.type, t.owner, t.amount, t.category, t.description, t.date); refresh++; tab = 0 })
                    3 -> Stats(pad, income, expenses, contribution, debt)
                    else -> More(pad, categories, loans, { repo.addCategory(it); refresh++ })
                }
            }
        }
        if (showAdd) {
            AddExpenseDialog(categories, { showAdd = false }, { t -> repo.addTransaction(t.type, t.owner, t.amount, t.category, t.description, t.date); showAdd = false; refresh++ })
        }
    }

    @Composable
    private fun ReferenceBottomBar(tab: Int, select: (Int) -> Unit) {
        NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
            RefNav(tab == 0, { select(0) }, Icons.Default.GridView, "Home")
            RefNav(tab == 1, { select(1) }, Icons.Default.List, "List")
            RefNav(tab == 2, { select(2) }, Icons.Default.AddCircleOutline, "Add")
            RefNav(tab == 3, { select(3) }, Icons.Default.PieChartOutline, "Stats")
            RefNav(tab == 4, { select(4) }, Icons.Default.Settings, "More")
        }
    }

    @Composable
    private fun RowScope.RefNav(selected: Boolean, click: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
        NavigationBarItem(
            selected = selected,
            onClick = click,
            icon = { Icon(icon, null) },
            label = { Text(label, fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(selectedIconColor = RefBlue, selectedTextColor = RefBlue, indicatorColor = Color(0xFFEAF3FF), unselectedIconColor = RefMuted, unselectedTextColor = RefMuted)
        )
    }

    @Composable
    private fun Home(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double, transactions: List<FinanceTransaction>, add: () -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 25.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("wowstudio26", color = RefText, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("● Synced · tracking today", color = RefGreen, fontSize = 10.sp) }; Icon(Icons.Default.NotificationsNone, null, tint = RefMuted) } }
            item { PremiumCard() }
            item { BalanceCard(income, expenses, contribution, add) }
            item { SectionTitle("QUICK ACTIONS") }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickCard("AI Coach", "Get a smart overview", RefPurple, Icons.Default.AutoAwesome, Modifier.weight(1f)); QuickCard("Smart Insights", "See spending trends", RefGreen, Icons.Default.Insights, Modifier.weight(1f)); QuickCard("Forecast", "Plan ahead", RefBlue, Icons.Default.ShowChart, Modifier.weight(1f)) } }
            item { FeatureCard("Family & Group Expenses", "Split bills, track trips and settle up", Icons.Default.Groups, RefBlue) }
            item { FeatureCard("Business Tracker", "Track business income and expenses", Icons.Default.BusinessCenter, RefGreen) }
            item { FeatureCard("Office Trips", "Trip expenses, advances & reports", Icons.Default.FlightTakeoff, RefBlue) }
            item { SectionTitle("RECENT ACTIVITY") }
            if (transactions.isEmpty()) item { EmptyJourney() }
            items(transactions.take(5), key = { it.id }) { ReferenceTransaction(it) }
            item { Text("Total debt  ${money(debt)}", color = RefMuted, fontSize = 11.sp) }
        }
    }

    @Composable private fun PremiumCard() {
        Card(colors = CardDefaults.cardColors(Color(0xFFEAF4FF)), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(Color(0xFFD9ECFF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.WorkspacePremium, null, tint = RefBlue) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("Upgrade to Premium", color = RefText, fontWeight = FontWeight.Bold); Text("50 free titles this month · +15 per short ad", color = RefMuted, fontSize = 9.sp) }; Text("Upgrade", color = RefBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold) } }

    @Composable private fun BalanceCard(income: Double, expenses: Double, contribution: Double, add: () -> Unit) {
        Card(colors = CardDefaults.cardColors(RefCard), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("AUGUST 2026", color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("₹${formatCompact(income - expenses)}", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Available balance", color = RefMuted, fontSize = 10.sp) }; Text("+ add income", color = RefGreen, fontSize = 10.sp) }; Spacer(Modifier.height(13.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Metric("Income", income, RefGreen); Metric("Expenses", expenses, RefRed); Metric("Contribution", contribution, RefPurple) }; Spacer(Modifier.height(12.dp)); Button(onClick = add, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(RefBlue), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Add expense") } } }

    @Composable private fun Metric(label: String, value: Double, color: Color) { Column { Text(label, color = RefMuted, fontSize = 9.sp); Text(money(value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }
    @Composable private fun SectionTitle(text: String) { Text(text, color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }

    @Composable private fun QuickCard(title: String, sub: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) { Column(Modifier.padding(11.dp)) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)); Spacer(Modifier.height(7.dp)); Text(title, color = RefText, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(sub, color = RefMuted, fontSize = 8.sp) } } }
    @Composable private fun FeatureCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color(0xFFEAF3FF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = color, modifier = Modifier.size(20.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(sub, color = RefMuted, fontSize = 9.sp) }; Icon(Icons.Default.ChevronRight, null, tint = RefMuted) } } }

    @Composable private fun ReferenceTransaction(t: FinanceTransaction) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).background(Color(0xFFF0F3F8), RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) { Icon(if (t.type == TransactionType.EXPENSE) Icons.Default.ReceiptLong else Icons.Default.AccountBalanceWallet, null, tint = RefBlue, modifier = Modifier.size(19.dp)) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(t.description.ifBlank { t.category }, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("${t.category} · ${t.owner}", color = RefMuted, fontSize = 9.sp) }; Text((if (t.type == TransactionType.EXPENSE) "−" else "+") + money(t.amount), color = if (t.type == TransactionType.EXPENSE) RefRed else RefGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }

    @Composable private fun EmptyJourney() { Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.RocketLaunch, null, tint = RefBlue, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(8.dp)); Text("Start your journey!", color = RefText, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text("Add your first expense to start tracking.", color = RefMuted, fontSize = 9.sp) } }

    @Composable private fun History(pad: PaddingValues, list: List<FinanceTransaction>, delete: (Long) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Text("Transactions", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Your complete spending history", color = RefMuted, fontSize = 11.sp); Spacer(Modifier.height(8.dp)) }; items(list, key = { it.id }) { t -> Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(15.dp)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { ReferenceTransaction(t); IconButton({ delete(t.id) }) { Icon(Icons.Default.DeleteOutline, null, tint = RefRed) } } } } } }

    @Composable private fun Stats(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double) { LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) { item { Text("Statistics", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("A clear view of your money", color = RefMuted, fontSize = 11.sp) }; item { StatBlock("Income", income, RefGreen) }; item { StatBlock("Expenses", expenses, RefRed) }; item { StatBlock("Contributions", contribution, RefPurple) }; item { StatBlock("Debt", debt, RefBlue) } } }
    @Composable private fun StatBlock(label: String, value: Double, color: Color) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(17.dp)) { Row { Text(label, color = RefText, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(money(value), color = color, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { if (value == 0.0) 0f else .65f }, modifier = Modifier.fillMaxWidth(), color = color, trackColor = RefLine) } } }

    @Composable private fun More(pad: PaddingValues, categories: List<String>, loans: List<Loan>, addCategory: (String) -> Unit) { var name by remember { mutableStateOf("") }; LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("More", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Preferences, tools and account", color = RefMuted, fontSize = 11.sp) }; item { SettingCard("Profile & Account", "wowstudio26", Icons.Default.Person) }; item { SettingCard("Unlock with Biometrics", "Use Face ID / fingerprint for app lock", Icons.Default.Fingerprint, true) }; item { SettingCard("Due Dates on Home", "Show due-date cards on Home", Icons.Default.Event) }; item { SettingCard("Budget Alerts", "Monthly, category, and daily budget nudges", Icons.Default.Notifications) }; item { SettingCard("Backed up", "Synced with cloud", Icons.Default.CloudDone) }; item { SettingCard("Export JSON (backup)", "Full backup of all expenses", Icons.Default.DataObject) }; item { SettingCard("New Savings Goal", "Plan for vacation, gadgets, emergencies", Icons.Default.Savings) }; item { Text("CATEGORIES", color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }; item { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) { Text("${categories.size} categories", color = RefText, fontWeight = FontWeight.Bold); Text("Budgets, icons & visibility", color = RefMuted, fontSize = 9.sp); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(name, { name = it }, placeholder = { Text("New category", fontSize = 10.sp) }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(7.dp)); Button(enabled = name.trim().isNotEmpty(), onClick = { addCategory(name.trim()); name = "" }) { Text("+ New") } } } } }; item { SettingCard("Loans & Debt", "${loans.size} loans · ${money(loans.sumOf { it.remainingAmount })} remaining", Icons.Default.AccountBalance) }; item { Text("ABOUT", color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp); Text("What's New · rate & share · support · legal & version", color = RefMuted, fontSize = 10.sp) } } }

    @Composable private fun SettingCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, toggle: Boolean = false) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(17.dp)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(Color(0xFFEAF3FF), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = RefBlue, modifier = Modifier.size(20.dp)) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(title, color = RefText, fontWeight = FontWeight.Bold, fontSize = 12.sp); Text(sub, color = RefMuted, fontSize = 9.sp) }; if (toggle) Switch(checked = false, onCheckedChange = {}) else Icon(Icons.Default.ChevronRight, null, tint = RefMuted) } } }

    @Composable private fun AddExpense(pad: PaddingValues, categories: List<String>, save: (FinanceTransaction) -> Unit) { var amount by rememberSaveable { mutableStateOf("") }; var desc by rememberSaveable { mutableStateOf("") }; var category by rememberSaveable { mutableStateOf(categories.firstOrNull() ?: "Other") }; LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 25.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) { item { Text("New Expense", color = RefText, fontSize = 28.sp, fontWeight = FontWeight.Bold) }; item { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(17.dp)) { Text("AMOUNT", color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("₹${if (amount.isBlank()) "0" else amount}", color = RefBlue, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth()); OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, placeholder = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth()) } } }; item { OutlinedTextField(desc, { desc = it }, label = { Text("DESCRIPTION") }, placeholder = { Text("e.g. Swiggy dinner") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }; item { Text("CATEGORY", color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { categories.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { row.forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c, fontSize = 9.sp) }) } } } } }; item { Button(enabled = amount.toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(FinanceTransaction(0, TransactionType.EXPENSE, "Household", amount.toDouble(), category, desc, System.currentTimeMillis(), System.currentTimeMillis(), false)) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(RefBlue), shape = RoundedCornerShape(16.dp)) { Text("Add Expense") } } } }

    @Composable private fun AddExpenseDialog(categories: List<String>, dismiss: () -> Unit, save: (FinanceTransaction) -> Unit) { var amount by remember { mutableStateOf("") }; var desc by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = dismiss, title = { Text("New Expense") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)); OutlinedTextField(desc, { desc = it }, label = { Text("Description") }) } }, confirmButton = { TextButton(enabled = amount.toDoubleOrNull()?.let { it > 0 } == true, onClick = { save(FinanceTransaction(0, TransactionType.EXPENSE, "Household", amount.toDouble(), categories.firstOrNull() ?: "Other", desc, System.currentTimeMillis(), System.currentTimeMillis(), false)) }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = dismiss) { Text("CANCEL") } }) }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value).replace("₹", "₹")
    private fun formatCompact(value: Double): String = NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
}
