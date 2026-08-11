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
            colors = NavigationBarItemDefaults.colors(selectedIconColor = RefBlue, selectedTextColor = RefBlue, indicatorColor = Color(0xFFEAF3FF), unselectedIconColor = RefMuted, unselectedTextColor = RefMuted)
        )
    }

    @Composable
    private fun Home(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double, transactions: List<FinanceTransaction>, add: () -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 25.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("wowstudio26", color = RefText, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Premium") } } }
            item { PremiumCard() }
            item { BalanceCard(income, expenses, contribution, add) }
            item { SectionTitle("QUICK ACTIONS") }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { QuickCard("AI Coach", "Get a smart overview", RefPurple, Icons.Default.AutoAwesome, Modifier.weight(1f)) } }
            item { FeatureCard("Family & Group Expenses", "Split bills, track trips and settle up", Icons.Default.Groups, RefBlue) }
            item { FeatureCard("Business Tracker", "Track business income and expenses", Icons.Default.BusinessCenter, RefGreen) }
            item { FeatureCard("Office Trips", "Trip expenses, advances & reports", Icons.Default.FlightTakeoff, RefBlue) }
            item { SectionTitle("RECENT ACTIVITY") }
            if (transactions.isEmpty()) item { EmptyJourney() }
            items(transactions.take(5), key = { it.id }) { ReferenceTransaction(it) }
            item { Text("Total debt  ${money(debt)}", color = RefMuted, fontSize = 11.sp) }
        }
    }

    @Composable
    private fun PremiumCard() {
        Card(colors = CardDefaults.cardColors(Color(0xFFEAF4FF)), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Text("Premium") } }
    }

    @Composable
    private fun BalanceCard(income: Double, expenses: Double, contribution: Double, add: () -> Unit) {
        Card(colors = CardDefaults.cardColors(RefCard), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Balance", color = RefMuted, fontSize = 11.sp); Text(money(income - expenses), color = RefBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold) } } } }
    }

    @Composable
    private fun Metric(label: String, value: Double, color: Color) { Column { Text(label, color = RefMuted, fontSize = 9.sp); Text(money(value), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }

    @Composable
    private fun SectionTitle(text: String) { Text(text, color = RefMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }

    @Composable
    private fun QuickCard(title: String, sub: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) { Icon(icon, null, tint = color); Text(title, color = RefText, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(sub, color = RefMuted, fontSize = 10.sp) } } }

    @Composable
    private fun FeatureCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = color, modifier = Modifier.size(32.dp)); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(title, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(sub, color = RefMuted, fontSize = 10.sp) } Icon(Icons.Default.ArrowForward, null, tint = RefMuted, modifier = Modifier.size(20.dp)) } } }

    @Composable
    private fun ReferenceTransaction(t: FinanceTransaction) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(40.dp).background(RefBlue.copy(0.1f), RoundedCornerShape(10.dp))) { Icon(Icons.Default.ShoppingCart, null, tint = RefBlue, modifier = Modifier.align(Alignment.Center).size(20.dp)) } Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(t.description, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(t.category, color = RefMuted, fontSize = 10.sp) } Text(money(t.amount), color = RefRed, fontSize = 12.sp, fontWeight = FontWeight.Bold) } }

    @Composable
    private fun EmptyJourney() { Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.RocketLaunch, null, tint = RefMuted, modifier = Modifier.size(48.dp)); Text("No transactions yet", color = RefMuted, fontSize = 14.sp) } }

    @Composable
    private fun History(pad: PaddingValues, list: List<FinanceTransaction>, delete: (Long) -> Unit) { LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp)) { items(list, key = { it.id }) { item { Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(item.description, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(item.category, color = RefMuted, fontSize = 10.sp) } Text(money(item.amount), color = RefRed, fontSize = 12.sp, fontWeight = FontWeight.Bold); IconButton({ delete(item.id) }) { Icon(Icons.Default.Delete, null, tint = RefRed) } } } } } }

    @Composable
    private fun Stats(pad: PaddingValues, income: Double, expenses: Double, contribution: Double, debt: Double) { LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { StatBlock("Income", income, RefGreen) } item { StatBlock("Expenses", expenses, RefRed) } item { StatBlock("Contribution", contribution, RefPurple) } item { StatBlock("Debt", debt, RefRed) } } }

    @Composable
    private fun StatBlock(label: String, value: Double, color: Color) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Column(Modifier.fillMaxWidth().padding(20.dp)) { Text(label, color = RefMuted, fontSize = 11.sp); Text(money(value), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold) } } }

    @Composable
    private fun More(pad: PaddingValues, categories: List<String>, loans: List<Loan>, addCategory: (String) -> Unit) { var name by remember { mutableStateOf("") }; LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { item { Text("Categories", color = RefText, fontSize = 16.sp, fontWeight = FontWeight.Bold) } items(categories, key = { it }) { SettingCard(it, "Category", Icons.Default.Label) } item { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { TextField(name, { name = it }, modifier = Modifier.weight(1f), placeholder = { Text("Add category") }); Button({ addCategory(name); name = "" }) { Text("Add") } } } } }

    @Composable
    private fun SettingCard(title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector, toggle: Boolean = false) { Card(colors = CardDefaults.cardColors(Color.White), shape = RoundedCornerShape(18.dp)) { Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = RefBlue, modifier = Modifier.size(24.dp)); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(title, color = RefText, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text(sub, color = RefMuted, fontSize = 10.sp) } } } }

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
