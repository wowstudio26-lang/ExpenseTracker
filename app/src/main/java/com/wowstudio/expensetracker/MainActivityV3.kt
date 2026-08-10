package com.wowstudio.expensetracker

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0E1012)
private val Surface = Color(0xFF171A1E)
private val Surface2 = Color(0xFF202328)
private val Stroke = Color(0xFF30343A)
private val RedAccent = Color(0xFFE50914)
private val RedSoft = Color(0xFFFFB4AA)
private val Green = Color(0xFF63D39B)
private val TextMain = Color(0xFFF3F4F6)
private val TextMuted = Color(0xFF969CA5)

class MainActivityV3 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseTrackerV3() }
    }

    @Composable
    private fun ExpenseTrackerV3() {
        val repo = remember { FinanceRepository(this@MainActivityV3) }
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var refresh by remember { mutableIntStateOf(0) }
        var showTransaction by remember { mutableStateOf(false) }
        var editingTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
        var showLoan by remember { mutableStateOf(false) }
        var editingLoan by remember { mutableStateOf<Loan?>(null) }
        var categoryVersion by remember { mutableIntStateOf(0) }

        val transactions = remember(refresh) { repo.transactions() }
        val loans = remember(refresh) { repo.loans() }
        val categories = remember(refresh, categoryVersion) { repo.categories() }

        val myIncome = transactions.filter { it.type == TransactionType.INCOME && it.owner == "Mine" }.sumOf { it.amount }
        val wifeContribution = transactions.filter { it.type == TransactionType.CONTRIBUTION && it.owner == "Wife" }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val debt = loans.sumOf { it.remainingAmount }
        val monthlyEmi = loans.filter { it.type != LoanType.PAY_LATER && it.remainingMonths > 0 }.sumOf { it.monthlyPayment }
        val available = myIncome + wifeContribution - expenses - monthlyEmi

        MaterialTheme(colorScheme = darkColorScheme(
            primary = RedAccent,
            onPrimary = Color.White,
            background = Bg,
            surface = Surface,
            onSurface = TextMain
        )) {
            Scaffold(
                containerColor = Bg,
                topBar = { Header(tab) },
                bottomBar = { BottomNav(tab) { tab = it } },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            if (tab == 2) showLoan = true else showTransaction = true
                        },
                        containerColor = RedAccent,
                        contentColor = Color.White,
                        shape = CircleShape
                    ) { Icon(Icons.Default.Add, "Add") }
                }
            ) { padding ->
                when (tab) {
                    0 -> Dashboard(padding, myIncome, wifeContribution, expenses, available, debt, monthlyEmi, transactions.take(6)) { tab = 1 }
                    1 -> History(padding, transactions, { editingTransaction = it }, { repo.deleteTransaction(it); refresh++ })
                    2 -> LoansScreen(padding, loans, debt, monthlyEmi, { showLoan = true }, { editingLoan = it }, { repo.deleteLoan(it); refresh++ })
                    3 -> Advisor(padding, myIncome, wifeContribution, expenses, available, debt, monthlyEmi)
                    else -> SettingsScreen(padding, categories, { repo.addCategory(it); categoryVersion++ }, { repo.deleteCategory(it); categoryVersion++ })
                }
            }
        }

        if (showTransaction || editingTransaction != null) {
            TransactionDialog(
                initial = editingTransaction,
                categories = categories,
                onDismiss = { showTransaction = false; editingTransaction = null },
                onSave = { t ->
                    if (t.id == 0L) repo.addTransaction(t.type, t.owner, t.amount, t.category, t.description, t.date)
                    else repo.updateTransaction(t.id, t.type, t.owner, t.amount, t.category, t.description, t.date)
                    showTransaction = false
                    editingTransaction = null
                    refresh++
                }
            )
        }

        if (showLoan || editingLoan != null) {
            LoanDialog(
                initial = editingLoan,
                onDismiss = { showLoan = false; editingLoan = null },
                onSave = { loan ->
                    if (loan.id == 0L) repo.addLoan(loan) else repo.updateLoan(loan)
                    showLoan = false
                    editingLoan = null
                    refresh++
                }
            )
        }
    }

    @Composable
    private fun Header(tab: Int) {
        Row(
            Modifier.fillMaxWidth().background(Bg).padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("ExpenseTracker", color = TextMain, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(listOf("Dashboard", "Transaction History", "Loans & Debt", "AI Advisor", "Settings")[tab], color = TextMuted, fontSize = 12.sp)
            }
            Icon(Icons.Default.AccountBalanceWallet, null, tint = RedSoft, modifier = Modifier.size(26.dp))
        }
    }

    @Composable
    private fun BottomNav(tab: Int, select: (Int) -> Unit) {
        NavigationBar(containerColor = Color(0xFF121519), tonalElevation = 0.dp) {
            NavItem(tab == 0, { select(0) }, Icons.Default.Home, "Home")
            NavItem(tab == 1, { select(1) }, Icons.Default.ReceiptLong, "History")
            NavItem(tab == 2, { select(2) }, Icons.Default.AccountBalance, "Loans")
            NavItem(tab == 3, { select(3) }, Icons.Default.AutoAwesome, "AI")
            NavItem(tab == 4, { select(4) }, Icons.Default.Settings, "Settings")
        }
    }

    @Composable
    private fun RowScope.NavItem(selected: Boolean, click: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
        NavigationBarItem(
            selected = selected,
            onClick = click,
            icon = { Icon(icon, null) },
            label = { Text(label, fontSize = 9.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
                indicatorColor = Color(0xFF2B2E34),
                unselectedIconColor = TextMuted,
                unselectedTextColor = TextMuted
            )
        )
    }

    @Composable
    private fun Dashboard(
        pad: PaddingValues,
        income: Double,
        wife: Double,
        expenses: Double,
        balance: Double,
        debt: Double,
        emi: Double,
        recent: List<FinanceTransaction>,
        openHistory: () -> Unit
    ) {
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(colors = CardDefaults.cardColors(Surface2), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text("TOTAL BALANCE", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(money(balance), color = TextMain, fontSize = 35.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("My income + wife contribution  expenses  monthly EMI", color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("My income", money(income), Green, Modifier.weight(1f))
                    StatCard("Wife contribution", money(wife), RedSoft, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Expenses", money(expenses), RedSoft, Modifier.weight(1f))
                    StatCard("After EMI", money(balance), TextMain, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Overall debt", money(debt), RedAccent, Modifier.weight(1f))
                    StatCard("Monthly EMI", money(emi), RedSoft, Modifier.weight(1f))
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Recent transactions", color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("VIEW ALL", color = RedSoft, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { openHistory() })
                }
            }
            if (recent.isEmpty()) item { EmptyState("No transactions yet") }
            items(recent, key = { it.id }) { TransactionRow(it) }
        }
    }

    @Composable
    private fun StatCard(label: String, value: String, valueColor: Color, modifier: Modifier) {
        Card(modifier, colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text(label, color = TextMuted, fontSize = 10.sp)
                Spacer(Modifier.height(5.dp))
                Text(value, color = valueColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    private fun TransactionRow(t: FinanceTransaction) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).background(Surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(if (t.type == TransactionType.EXPENSE) Icons.Default.ShoppingBag else if (t.type == TransactionType.CONTRIBUTION) Icons.Default.People else Icons.Default.AccountBalance, null, tint = RedSoft, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(t.description.ifBlank { t.category }, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${t.category}  ${t.owner}  ${formatDate(t.date)}", color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
            Text(
                (if (t.type == TransactionType.EXPENSE) "" else "+") + money(t.amount),
                color = if (t.type == TransactionType.EXPENSE) RedSoft else Green,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    @Composable
    private fun History(pad: PaddingValues, list: List<FinanceTransaction>, edit: (FinanceTransaction) -> Unit, delete: (Long) -> Unit) {
        var filter by remember { mutableStateOf("All") }
        val shown = list.filter { filter == "All" || it.type.name.equals(filter, true) }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text("Transaction History", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text("All household transactions in one place", color = TextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("All", "Expense", "Income", "Contribution").forEach { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f, fontSize = 10.sp) })
                    }
                }
            }
            items(shown, key = { it.id }) { t ->
                Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                        TypeIcon(t.type)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.description.ifBlank { t.category }, color = TextMain, fontWeight = FontWeight.Bold)
                            Text("${t.category}  ${t.owner}", color = TextMuted, fontSize = 10.sp)
                            Text(formatDate(t.date), color = TextMuted, fontSize = 10.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text((if (t.type == TransactionType.EXPENSE) "" else "+") + money(t.amount), color = if (t.type == TransactionType.EXPENSE) RedSoft else Green, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton({ edit(t) }, Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", tint = RedSoft, modifier = Modifier.size(17.dp)) }
                                IconButton({ delete(t.id) }, Modifier.size(32.dp)) { Icon(Icons.Default.DeleteOutline, "Delete", tint = RedSoft, modifier = Modifier.size(17.dp)) }
                            }
                        }
                    }
                }
            }
            if (shown.isEmpty()) item { EmptyState("No transactions found") }
        }
    }

    @Composable
    private fun LoansScreen(pad: PaddingValues, loans: List<Loan>, debt: Double, emi: Double, add: () -> Unit, edit: (Loan) -> Unit, delete: (Long) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Loans & Debt", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        Text("Overall debt and monthly commitments", color = TextMuted, fontSize = 11.sp)
                    }
                    Button(onClick = add, colors = ButtonDefaults.buttonColors(RedAccent), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Add loan")
                    }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Overall debt", money(debt), RedSoft, Modifier.weight(1f))
                    StatCard("Monthly EMI", money(emi), RedSoft, Modifier.weight(1f))
                }
            }
            if (loans.isEmpty()) item { EmptyState("No loans added yet") }
            items(loans, key = { it.id }) { LoanCard(it, edit, delete) }
        }
    }

    @Composable
    private fun LoanCard(loan: Loan, edit: (Loan) -> Unit, delete: (Long) -> Unit) {
        Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).background(Surface2, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(if (loan.type == LoanType.PAY_LATER) Icons.Default.Schedule else Icons.Default.AccountBalance, null, tint = RedSoft)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(loan.lender, color = TextMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("${loan.type.name.replace('_', ' ')}  ${loan.product.ifBlank { "General" }}", color = TextMuted, fontSize = 10.sp)
                    }
                    Text(money(loan.remainingAmount), color = RedSoft, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                if (loan.type == LoanType.PAY_LATER) {
                    Text("Pay later  due ${formatDate(loan.nextDueDate)}", color = TextMuted, fontSize = 11.sp)
                } else {
                    val progress = if (loan.tenureMonths > 0) loan.paidMonths.toFloat() / loan.tenureMonths else 0f
                    LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = RedAccent, trackColor = Stroke)
                    Spacer(Modifier.height(7.dp))
                    Text("${loan.paidMonths}/${loan.tenureMonths} EMIs paid  ${loan.remainingMonths} remaining", color = TextMuted, fontSize = 10.sp)
                    Text("Monthly EMI ${money(loan.monthlyPayment)}  Remaining ${money(loan.remainingAmount)}", color = TextMuted, fontSize = 10.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton({ edit(loan) }) { Text("EDIT", color = RedSoft) }
                    TextButton({ delete(loan.id) }) { Text("DELETE", color = RedSoft) }
                }
            }
        }
    }

    @Composable
    private fun Advisor(pad: PaddingValues, income: Double, wife: Double, expenses: Double, balance: Double, debt: Double, emi: Double) {
        val totalIncome = income + wife
        val commitment = if (totalIncome > 0) (expenses + emi) / totalIncome else 0.0
        val message = when {
            totalIncome == 0.0 -> "Add your income and household contribution first."
            commitment > .80 -> "Your monthly commitments are very high. Avoid adding new EMI debt until the ratio improves."
            commitment > .60 -> "Your commitments are moderate-high. Keep a cash buffer before taking another loan."
            else -> "Your current commitments are manageable. Prioritise building a cash reserve and reducing expensive debt."
        }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("AI Advisor", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Finance-coach style insights", color = TextMuted, fontSize = 11.sp) }
            item { 
                Card(colors = CardDefaults.cardColors(Surface2), shape = RoundedCornerShape(22.dp)) { 
                    Column(Modifier.padding(18.dp)) { 
                        Text("MONTHLY SNAPSHOT", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(message, color = TextMain, fontSize = 13.sp)
                    }
                }
            }
            item { AdvisorLine("1", if (emi > 0) "Track every EMI and its remaining months." else "Add your loans so monthly debt commitments can be calculated.") }
            item { AdvisorLine("2", if (balance > 0) "Keep part of the available balance as an emergency buffer." else "Your current calculated balance is low; avoid unnecessary new commitments.") }
            item { AdvisorLine("3", "Wife contributions stay separate from your personal income in the dashboard.") }
        }
    }

    @Composable
    private fun AdvisorLine(number: String, text: String) {
        Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(16.dp)) { 
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { 
                Box(Modifier.size(28.dp).background(RedAccent, CircleShape), contentAlignment = Alignment.Center) { 
                    Text(number, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(text, color = TextMain, fontSize = 12.sp, modifier = Modifier.weight(1f))
            }
        }
    }

    @Composable
    private fun SettingsScreen(pad: PaddingValues, categories: List<String>, add: (String) -> Unit, delete: (String) -> Unit) {
        var newCategory by remember { mutableStateOf("") }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Settings", color = TextMain, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("Manage your categories without leaving the app", color = TextMuted, fontSize = 11.sp) }
            item {
                Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Categories", color = TextMain, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(newCategory, { newCategory = it }, Modifier.weight(1f), label = { Text("New category") }, singleLine = true)
                            Spacer(Modifier.width(8.dp))
                            IconButton({ if (newCategory.isNotBlank()) { add(newCategory); newCategory = "" } }) { Icon(Icons.Default.AddCircle, "Add category", tint = RedSoft) }
                        }
                    }
                }
            }
            items(categories) { category ->
                Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(14.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(category, color = TextMain, modifier = Modifier.weight(1f))
                        IconButton({ delete(category) }) { Icon(Icons.Default.DeleteOutline, "Delete", tint = TextMuted) }
                    }
                }
            }
        }
    }

    @Composable
    private fun TransactionDialog(initial: FinanceTransaction?, categories: List<String>, onDismiss: () -> Unit, onSave: (FinanceTransaction) -> Unit) {
        var type by remember(initial?.id) { mutableStateOf(initial?.type ?: TransactionType.EXPENSE) }
        var owner by remember(initial?.id) { mutableStateOf(initial?.owner ?: "Mine") }
        var amount by remember(initial?.id) { mutableStateOf(if (initial == null) "" else initial.amount.toString()) }
        var category by remember(initial?.id) { mutableStateOf(initial?.category ?: categories.firstOrNull() ?: "Other") }
        var description by remember(initial?.id) { mutableStateOf(initial?.description ?: "") }
        var date by remember(initial?.id) { mutableLongStateOf(initial?.date ?: System.currentTimeMillis()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Surface2,
            title = { Text(if (initial == null) "Add transaction" else "Edit transaction", color = TextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                        listOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.CONTRIBUTION).forEach { t -> 
                            FilterChip(type == t, { type = t }, label = { Text(t.name.replace('_', ' '), fontSize = 9.sp) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                        listOf("Mine", "Wife", "Household").forEach { o -> 
                            FilterChip(owner == o, { owner = o }, label = { Text(o, fontSize = 9.sp) })
                        }
                    }
                    OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Category", color = TextMuted, fontSize = 11.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        categories.take(4).forEach { c -> FilterChip(category == c, { category = c }, label = { Text(c, fontSize = 9.sp) }) }
                    }
                    OutlinedButton(onClick = { showDatePicker(date) { date = it } }, modifier = Modifier.fillMaxWidth()) { Text("Date: ${formatDate(date)}") }
                }
            },
            confirmButton = { 
                Button(onClick = { 
                    val value = amount.toDoubleOrNull() ?: 0.0
                    if (value > 0) onSave(FinanceTransaction(initial?.id ?: 0L, type, owner, value, category, description, date, System.currentTimeMillis(), false))
                }, colors = ButtonDefaults.buttonColors(RedAccent)) { Text("SAVE") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) } }
        )
    }

    @Composable
    private fun LoanDialog(initial: Loan?, onDismiss: () -> Unit, onSave: (Loan) -> Unit) {
        var lender by remember(initial?.id) { mutableStateOf(initial?.lender ?: "") }
        var type by remember(initial?.id) { mutableStateOf(initial?.type ?: LoanType.EMI) }
        var product by remember(initial?.id) { mutableStateOf(initial?.product ?: "") }
        var original by remember(initial?.id) { mutableStateOf(initial?.originalAmount?.toString() ?: "") }
        var monthly by remember(initial?.id) { mutableStateOf(initial?.monthlyPayment?.toString() ?: "") }
        var tenure by remember(initial?.id) { mutableStateOf(initial?.tenureMonths?.toString() ?: "") }
        var paid by remember(initial?.id) { mutableStateOf(initial?.paidMonths?.toString() ?: "0") }
        var start by remember(initial?.id) { mutableLongStateOf(initial?.startDate ?: System.currentTimeMillis()) }
        var due by remember(initial?.id) { mutableLongStateOf(initial?.nextDueDate ?: System.currentTimeMillis()) }
        val total = original.toDoubleOrNull() ?: 0.0
        val emi = monthly.toDoubleOrNull() ?: 0.0
        val months = tenure.toIntOrNull() ?: 0
        val paidCount = paid.toIntOrNull() ?: 0
        val remainingMonths = if (type == LoanType.PAY_LATER) 0 else (months - paidCount).coerceAtLeast(0)
        val remaining = if (type == LoanType.PAY_LATER) total else emi * remainingMonths

        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Surface2,
            title = { Text(if (initial == null) "Add loan" else "Edit loan", color = TextMain) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(lender, { lender = it }, label = { Text("Lender / loan name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { 
                        LoanType.values().forEach { t -> 
                            FilterChip(type == t, { type = t }, label = { Text(t.name.replace('_', ' '), fontSize = 9.sp) })
                        }
                    }
                    OutlinedTextField(product, { product = it }, label = { Text("Product / purpose") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(original, { original = it }, label = { Text("Original amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    if (type != LoanType.PAY_LATER) {
                        OutlinedTextField(monthly, { monthly = it }, label = { Text("Monthly EMI") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(tenure, { tenure = it }, label = { Text("Tenure months") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            OutlinedTextField(paid, { paid = it }, label = { Text("Paid EMIs") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        }
                        Text("$remainingMonths EMI month(s) remaining  ${money(remaining)} remaining", color = RedSoft, fontSize = 11.sp)
                    } else {
                        Text("Pay Later has no EMI schedule. The full outstanding amount stays visible until you edit or clear it.", color = TextMuted, fontSize = 11.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showDatePicker(start) { start = it } }, modifier = Modifier.weight(1f)) { Text("Start ${formatDate(start)}", fontSize = 10.sp) }
                        OutlinedButton(onClick = { showDatePicker(due) { due = it } }, modifier = Modifier.weight(1f)) { Text("Due ${formatDate(due)}", fontSize = 10.sp) }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (lender.isNotBlank() && total > 0) {
                        onSave(Loan(initial?.id ?: 0L, lender, type, product, total, if (type == LoanType.PAY_LATER) 0.0 else emi, if (type == LoanType.PAY_LATER) 0 else months, paidCount, start, due))
                    }
                }, colors = ButtonDefaults.buttonColors(RedAccent)) { Text("SAVE LOAN") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextMuted) } }
        )
    }

    private fun showDatePicker(current: Long, onDate: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(this, { _, year, month, day -> onDate(Calendar.getInstance().apply { set(year, month, day, 12, 0, 0) }.timeInMillis) }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    @Composable
    private fun TypeIcon(type: TransactionType) {
        Box(Modifier.size(40.dp).background(Surface2, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
            Icon(if (type == TransactionType.EXPENSE) Icons.Default.ShoppingBag else if (type == TransactionType.CONTRIBUTION) Icons.Default.People else Icons.Default.AccountBalance, null, tint = RedSoft, modifier = Modifier.size(18.dp))
        }
    }

    @Composable
    private fun EmptyState(text: String) {
        Card(colors = CardDefaults.cardColors(Surface), shape = RoundedCornerShape(18.dp)) { 
            Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) { 
                Text(text, color = TextMuted, fontSize = 14.sp)
            }
        }
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply { maximumFractionDigits = 2; minimumFractionDigits = 2 }.format(value)
    private fun formatDate(value: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(value))
}
