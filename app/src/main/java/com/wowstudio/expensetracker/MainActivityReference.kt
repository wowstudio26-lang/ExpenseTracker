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
        val repo: FinanceRepository = remember { FinanceRepository(this@MainActivityReference) }
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
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = RefBlue,
                selectedTextColor = RefBlue,
                indicatorColor = Color(0xFFEAF3FF),
                unselectedIconColor = RefMuted,
                unselectedTextColor = RefMuted
            )
        )
    }

    @Composable
    private fun Home(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double, transactions: List<FinanceTransaction>, add: () -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 12.dp, 18.dp, 30.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { AccountCard() }
            item { PremiumCard() }
            item { BalanceCard(income, expenses, contribution, add) }
            item { SectionTitle("QUICK ACTIONS") }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    QuickCard("AI Coach", "Money coaching", RefPurple, Icons.Default.AutoAwesome, Modifier.weight(1f))
                    QuickCard("Smart Insights", "Spend patterns", RefRed, Icons.Default.AutoGraph, Modifier.weight(1f))
                    QuickCard("Forecast", "Monthly forecast", RefGreen, Icons.Default.TrendingUp, Modifier.weight(1f))
                }
            }
            item { SectionTitle("FAMILY & GROUPS") }
            item { FeatureCard("Family & Group Expenses", "Split bills, track trips & settle up", Icons.Default.Groups, RefBlue, Color(0xFF16B887)) }
            item { SectionTitle("BUSINESS & TRIPS") }
            item { FeatureCard("Business Tracker", "Track business income and expenses", Icons.Default.BusinessCenter, RefBlue, RefGreen) }
            item { FeatureCard("Office Trips", "Trip expenses, advances & reports", Icons.Default.FlightTakeoff, RefGreen, Color(0xFF08A7E5)) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("RECENT ACTIVITY", Modifier.weight(1f))
                    Text("View Report →", color = RefBlue, fontSize = 10.sp)
                }
            }
            if (transactions.isEmpty()) item { EmptyJourney() }
            items(transactions.take(5), key = { it.id }) { ReferenceTransaction(it) }
            item { Text("Total debt  ${money(debt)}", color = RefMuted, fontSize = 11.sp) }
        }
    }

    @Composable
    private fun AccountCard() {
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).background(RefBlue, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, tint = Color.White)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text("wowstudio26", color = RefText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(RefGreen, androidx.compose.foundation.shape.CircleShape))
                        Spacer(Modifier.width(5.dp))
                        Text("Synced · tracking today", color = RefMuted, fontSize = 10.sp)
                    }
                }
                Icon(Icons.Default.ChevronRight, null, tint = RefMuted)
            }
        }
    }

    @Composable
    private fun PremiumCard() {
        Card(colors = CardDefaults.cardColors(Color(0xFFEAF4FF)), shape = RoundedCornerShape(18.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Get Premium", color = RefBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Unlock all features", color = RefMuted, fontSize = 10.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = RefBlue)
            }
        }
    }

    @Composable
    private fun BalanceCard(income: Double, expenses: Double, contribution: Double, add: () -> Unit) {
        Card(colors = CardDefaults.cardColors(RefCard), shape = RoundedCornerShape(22.dp)) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Balance", color = RefMuted, fontSize = 11.sp)
                        Text(money(income - expenses), color = RefText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = add, modifier = Modifier.height(40.dp)) {
                        Text("Add", fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(15.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Metric("Income", income, RefGreen)
                    Metric("Expenses", expenses, RefRed)
                    Metric("Contribution", contribution, RefPurple)
                }
            }
        }
    }

    @Composable
    private fun Metric(label: String, value: Double, color: Color) {
        Column {
            Text(label, color = RefMuted, fontSize = 9.sp)
            Text(money(value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
        Text(text, modifier, color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }

    @Composable
    private fun QuickCard(title: String, sub: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) {
        Card(modifier, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(15.dp)) {
            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(icon, null, Modifier.size(28.dp), tint = color)
                Spacer(Modifier.height(8.dp))
                Text(title, color = RefText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(sub, color = RefMuted, fontSize = 8.sp)
            }
        }
    }

    @Composable
    private fun FeatureCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, start: Color, end: Color = start) {
        Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().background(androidx.compose.ui.graphics.Brush.horizontalGradient(listOf(start, end))).padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Color.White) }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(sub, color = Color.White.copy(alpha = 0.82f), fontSize = 10.sp)
                }
                Icon(Icons.Default.ChevronRight, null, tint = Color.White)
            }
        }
    }

    @Composable
    private fun ReferenceTransaction(t: FinanceTransaction) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).background(Color(0xFFEAF3FF), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(t.category.take(1), color = RefBlue, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(t.category, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(t.description, color = RefMuted, fontSize = 10.sp)
            }
            Text(money(t.amount), color = if (t.type == TransactionType.INCOME) RefGreen else RefRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun EmptyJourney() {
        Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.RocketLaunch, null, tint = RefMuted, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text("No transactions yet", color = RefText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("Start by adding your first expense", color = RefMuted, fontSize = 11.sp)
        }
    }

    @Composable
    private fun History(pad: PaddingValues, list: List<FinanceTransaction>, delete: (Long) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp)) {
            item { SectionTitle("TRANSACTION HISTORY") }
            items(list, key = { it.id }) { transaction ->
                Row(Modifier.fillMaxWidth().clickable { delete(transaction.id) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(transaction.category, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(transaction.description, color = RefMuted, fontSize = 10.sp)
                    }
                    Text(money(transaction.amount), color = RefRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Divider(color = RefLine)
            }
        }
    }

    @Composable
    private fun Stats(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("FINANCIAL OVERVIEW") }
            item { StatBlock("Total Income", income, RefGreen) }
            item { StatBlock("Total Expenses", expenses, RefRed) }
            item { StatBlock("Total Contribution", contribution, RefPurple) }
            item { StatBlock("Total Debt", debt, RefRed) }
        }
    }

    @Composable
    private fun StatBlock(label: String, value: Double, color: Color) {
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(15.dp)) {
                Text(label, color = RefMuted, fontSize = 11.sp)
                Spacer(Modifier.height(8.dp))
                Text(money(value), color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun More(pad: PaddingValues, categories: List<String>, loans: List<Loan>, addCategory: (String) -> Unit) {
        var name by remember { mutableStateOf("") }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionTitle("CATEGORIES") }
            items(categories) { cat -> SettingCard(cat, "Manage category", Icons.Default.Tag) }
            item { SectionTitle("SETTINGS") }
            item { SettingCard("Manage Profile", "Edit your details", Icons.Default.Person) }
            item { SettingCard("Notifications", "Alerts and reminders", Icons.Default.Notifications, toggle = true) }
            item { SettingCard("About", "App information", Icons.Default.Info) }
        }
    }

    @Composable
    private fun SettingCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, toggle: Boolean = false) {
        Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(15.dp)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(24.dp), tint = RefBlue)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(title, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(sub, color = RefMuted, fontSize = 10.sp)
                }
                if (toggle) {
                    Switch(checked = false, onCheckedChange = {})
                } else {
                    Icon(Icons.Default.ChevronRight, null, tint = RefMuted)
                }
            }
        }
    }

    @Composable
    fun AddExpense(pad: PaddingValues, categories: List<String>, save: (FinanceTransaction) -> Unit) {
        var amount by rememberSaveable { mutableStateOf("") }
        var desc by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf(if (categories.isNotEmpty()) categories[0] else "") }
        var type by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }

        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextField(
                    amount,
                    { amount = it },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                TextField(
                    desc,
                    { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                if (categories.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                        TextField(
                            category,
                            {},
                            label = { Text("Category") },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            readOnly = true
                        )
                        ExposedDropdownMenu(expanded, { expanded = false }) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = { category = cat; expanded = false }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    {
                        if (amount.isNotBlank() && desc.isNotBlank()) {
                            save(FinanceTransaction(0, type, 1, amount.toDoubleOrNull() ?: 0.0, category, desc, System.currentTimeMillis()))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
            }
        }
    }

    @Composable
    fun AddExpenseDialog(categories: List<String>, dismiss: () -> Unit, save: (FinanceTransaction) -> Unit) {
        var amount by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        var category by remember { mutableStateOf(if (categories.isNotEmpty()) categories[0] else "") }
        var type by remember { mutableStateOf(TransactionType.EXPENSE) }

        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text("Add Expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        amount,
                        { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    TextField(
                        desc,
                        { desc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (categories.isNotEmpty()) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
                            TextField(
                                category,
                                {},
                                label = { Text("Category") },
                                modifier = Modifier.fillMaxWidth().menuAnchor(),
                                readOnly = true
                            )
                            ExposedDropdownMenu(expanded, { expanded = false }) {
                                categories.forEach { cat ->
                                    DropdownMenuItem(
                                        text = { Text(cat) },
                                        onClick = { category = cat; expanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button({
                    if (amount.isNotBlank() && desc.isNotBlank()) {
                        save(FinanceTransaction(0, type, 1, amount.toDoubleOrNull() ?: 0.0, category, desc, System.currentTimeMillis()))
                        dismiss()
                    }
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(dismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value).replace("₹", "₹")
    private fun formatCompact(value: Double): String = NumberFormat.getNumberInstance(Locale("en", "IN")).format(value)
}
