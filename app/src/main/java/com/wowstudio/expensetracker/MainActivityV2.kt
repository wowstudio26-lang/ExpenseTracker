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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wowstudio.expensetracker.data.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF0B0D10)
private val Panel = Color(0xFF15181D)
private val Panel2 = Color(0xFF1C2026)
private val Line = Color(0xFF2A2F36)
private val Accent = Color(0xFF6C63FF)
private val AccentSoft = Color(0xFFB8B3FF)
private val TextMain = Color(0xFFF2F3F5)
private val TextMuted = Color(0xFF8D949E)
private val Green = Color(0xFF4BD59A)
private val Red = Color(0xFFFF737F)

class MainActivityV2 : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { FinanceApp() } }

    @Composable private fun FinanceApp() {
        val repo = remember { FinanceRepository(this@MainActivityV2) }
        var tab by remember { mutableIntStateOf(0) }
        var refresh by remember { mutableIntStateOf(0) }
        var addTransaction by remember { mutableStateOf(false) }
        var editTransaction by remember { mutableStateOf<FinanceTransaction?>(null) }
        var addLoan by remember { mutableStateOf(false) }
        var editLoan by remember { mutableStateOf<Loan?>(null) }
        var categoriesVersion by remember { mutableIntStateOf(0) }
        val transactions = remember(refresh) { repo.transactions() }
        val loans = remember(refresh) { repo.loans() }
        val categories = remember(categoriesVersion, refresh) { repo.categories() }
        val myIncome = transactions.filter { it.type == TransactionType.INCOME && it.owner == "Mine" }.sumOf { it.amount }
        val wifeContribution = transactions.filter { it.type == TransactionType.CONTRIBUTION && it.owner == "Wife" }.sumOf { it.amount }
        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val debt = loans.sumOf { it.remainingAmount }
        val monthlyEmi = loans.filter { it.type != LoanType.PAY_LATER && it.remainingMonths > 0 }.sumOf { it.monthlyPayment }
        val balance = myIncome + wifeContribution - expenses - monthlyEmi

        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = Panel)) {
            Scaffold(containerColor = Bg, topBar = { TopBar(tab) }, bottomBar = { BottomBar(tab) { tab = it } }, floatingActionButton = {
                FloatingActionButton(onClick = { addTransaction = true }, containerColor = Accent, shape = CircleShape) { Icon(Icons.Default.Add, "Add", tint = Color.White) }
            }) { pad ->
                when (tab) {
                    0 -> Dashboard(pad, myIncome, wifeContribution, expenses, balance, debt, monthlyEmi, transactions.take(6), { tab = 1 })
                    1 -> History(pad, transactions, { editTransaction = it }, { repo.deleteTransaction(it); refresh++ })
                    2 -> Loans(pad, loans, debt, monthlyEmi, { addLoan = true }, { editLoan = it }, { repo.deleteLoan(it); refresh++ })
                    3 -> AiAdvisor(pad, myIncome, wifeContribution, expenses, balance, debt, monthlyEmi)
                    else -> Settings(pad, categories, { name -> repo.addCategory(name); categoriesVersion++ }, { name -> repo.deleteCategory(name); categoriesVersion++ })
                }
            }
        }
        if (addTransaction || editTransaction != null) TransactionDialog(repo, categories, editTransaction, { addTransaction = false; editTransaction = null }, { t ->
            if (t.id == 0L) repo.addTransaction(t.type, t.owner, t.amount, t.category, t.description, t.date) else repo.updateTransaction(t.id, t.type, t.owner, t.amount, t.category, t.description, t.date)
            addTransaction = false; editTransaction = null; refresh++
        })
        if (addLoan || editLoan != null) LoanDialog(editLoan, { addLoan = false; editLoan = null }, { l -> if (l.id == 0L) repo.addLoan(l) else repo.updateLoan(l); addLoan = false; editLoan = null; refresh++ })
    }

    @Composable private fun TopBar(tab: Int) {
        Row(Modifier.fillMaxWidth().background(Bg).padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("ExpenseTracker", color = TextMain, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(listOf("Dashboard","Transaction History","Loans","AI Advisor","Settings")[tab], color = TextMuted, fontSize = 11.sp) }
            Icon(Icons.Default.AccountBalanceWallet, null, tint = AccentSoft, modifier = Modifier.size(27.dp))
        }
    }

    @Composable private fun BottomBar(tab: Int, select: (Int) -> Unit) {
        NavigationBar(containerColor = Color(0xFF121519), tonalElevation = 0.dp) {
            Nav(tab == 0, { select(0) }, Icons.Default.Dashboard, "Home")
            Nav(tab == 1, { select(1) }, Icons.Default.ReceiptLong, "History")
            Nav(tab == 2, { select(2) }, Icons.Default.AccountBalance, "Loans")
            Nav(tab == 3, { select(3) }, Icons.Default.AutoAwesome, "AI")
            Nav(tab == 4, { select(4) }, Icons.Default.Settings, "Settings")
        }
    }
    @Composable private fun RowScope.Nav(selected: Boolean, click: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) { NavigationBarItem(selected, click, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 9.sp) }) }

    @Composable private fun Dashboard(pad: PaddingValues, income: Double, wife: Double, expenses: Double, balance: Double, debt: Double, emi: Double, recent: List<FinanceTransaction>, openHistory: () -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 105.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
            item { Card(colors = CardDefaults.cardColors(Panel2), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(19.dp)) { Text("AVAILABLE BALANCE", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(money(balance), color = TextMain, fontSize = 34.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); Text("My income + wife contribution − household expenses − monthly EMI", color = TextMuted, fontSize = 11.sp) } } }
            item { SummaryGrid(income, wife, expenses, balance) }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Total debt", money(debt), Red, Modifier.weight(1f)); StatCard("Monthly EMI", money(emi), AccentSoft, Modifier.weight(1f)) } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Recent transactions", Modifier.weight(1f), color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text("View all", Modifier.clickable(onClick = openHistory), color = AccentSoft, fontSize = 11.sp) } }
            items(recent, key = { it.id }) { TransactionRow(it) }
            item { Text("Wife contribution is tracked separately and is NOT counted as your income.", color = TextMuted, fontSize = 11.sp) }
        }
    }

    @Composable private fun SummaryGrid(income: Double, wife: Double, expenses: Double, balance: Double) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("My income", money(income), Green, Modifier.weight(1f)); StatCard("Wife contribution", money(wife), AccentSoft, Modifier.weight(1f)) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { StatCard("Household expenses", money(expenses), Red, Modifier.weight(1f)); StatCard("After EMI", money(balance), TextMain, Modifier.weight(1f)) } }
    }
    @Composable private fun StatCard(label: String, value: String, color: Color, modifier: Modifier) { Card(modifier, colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(14.dp)) { Text(label, color = TextMuted, fontSize = 10.sp); Spacer(Modifier.height(5.dp)); Text(value, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold) } } }

    @Composable private fun TransactionRow(t: FinanceTransaction) {
        Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            TypeIcon(t.type)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(t.description.ifBlank { t.category }, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("${t.category} • ${t.owner} • ${formatDate(t.date)}", color = TextMuted, fontSize = 10.sp, maxLines = 1)
            }
            Text(money(t.amount), color = if (t.type == TransactionType.EXPENSE) Red else if (t.type == TransactionType.CONTRIBUTION) AccentSoft else Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable private fun History(pad: PaddingValues, list: List<FinanceTransaction>, edit: (FinanceTransaction) -> Unit, delete: (Long) -> Unit) {
        var filter by remember { mutableStateOf("All") }
        val shown = list.filter { filter == "All" || it.type.name.equals(filter, true) }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 105.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Text("Transaction History", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { listOf("All","Expense","Income","Contribution").forEach { f -> FilterChip(filter == f, { filter = f }, label = { Text(f, fontSize = 10.sp) }) } } }
            items(shown, key = { it.id }) { t -> Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { TypeIcon(t.type); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(t.description.ifBlank { t.category }, color = TextMain, fontWeight = FontWeight.Bold); Text("${t.category} • ${t.owner} • ${formatDate(t.date)}", color = TextMuted, fontSize = 10.sp) }; Column(horizontalAlignment = Alignment.End) { Text(money(t.amount), color = if (t.type == TransactionType.EXPENSE) Red else if (t.type == TransactionType.CONTRIBUTION) AccentSoft else Green, fontWeight = FontWeight.Bold); Row { IconButton({ edit(t) }, Modifier.size(31.dp)) { Icon(Icons.Default.Edit, "Edit", tint = AccentSoft, modifier = Modifier.size(17.dp)) }; IconButton({ delete(t.id) }, Modifier.size(31.dp)) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Red, modifier = Modifier.size(17.dp)) } } } } } }
            if (shown.isEmpty()) item { Empty("No transactions found") }
        }
    }

    @Composable private fun Loans(pad: PaddingValues, loans: List<Loan>, debt: Double, emi: Double, add: () -> Unit, edit: (Loan) -> Unit, delete: (Long) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 105.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Loans & Debt", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Overall debt and monthly commitments", color = TextMuted, fontSize = 11.sp) }; Button(onClick = add, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Add loan") } } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) { StatCard("Overall debt", money(debt), Red, Modifier.weight(1f)); StatCard("Monthly EMI", money(emi), AccentSoft, Modifier.weight(1f)) } }
            items(loans, key = { it.id }) { loan -> LoanCard(loan, edit, delete) }
            if (loans.isEmpty()) item { Empty("No loans added yet") }
        }
    }

    @Composable private fun LoanCard(loan: Loan, edit: (Loan) -> Unit, delete: (Long) -> Unit) {
        Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (loan.type == LoanType.PAY_LATER) Icons.Default.Schedule else Icons.Default.AccountBalance, null, tint = AccentSoft, modifier = Modifier.size(30.dp)); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(loan.lender, color = TextMain, fontWeight = FontWeight.Bold); Text("${loan.type.name.replace('_',' ')} • ${loan.product.ifBlank { "General" }}", color = TextMuted, fontSize = 10.sp) }; Text(money(loan.remainingAmount), color = Red, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(9.dp)); if (loan.type != LoanType.PAY_LATER) { val progress = if (loan.tenureMonths > 0) loan.paidMonths.toFloat() / loan.tenureMonths else 0f; LinearProgressIndicator({ progress.coerceIn(0f,1f) }, Modifier.fillMaxWidth(), color = Accent); Spacer(Modifier.height(6.dp)); Text("${loan.paidMonths}/${loan.tenureMonths} EMIs paid • ${loan.remainingMonths} remaining • EMI ${money(loan.monthlyPayment)}", color = TextMuted, fontSize = 10.sp) } else { Text("Pay later outstanding • due ${formatDate(loan.nextDueDate)}", color = TextMuted, fontSize = 10.sp) }; Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ edit(loan) }) { Text("EDIT") }; TextButton({ delete(loan.id) }) { Text("DELETE", color = Red) } } } }
    }

    @Composable private fun AiAdvisor(pad: PaddingValues, income: Double, wife: Double, expenses: Double, balance: Double, debt: Double, emi: Double) {
        val ratio = if (income + wife > 0) (expenses + emi) / (income + wife) else 0.0
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 105.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("AI Advisor", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("A finance-coach style view of your current numbers", color = TextMuted, fontSize = 11.sp) }
            item { Card(colors = CardDefaults.cardColors(Panel2), shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(18.dp)) { Text("THIS MONTH", color = TextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text(when { ratio > .75 -> "Your committed spending is high."; ratio > .5 -> "Your commitments are moderate."; else -> "Your current cash position looks comfortable." }, color = TextMain, fontSize = 19.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(9.dp)); Text("Available after current expenses and monthly EMI: ${money(balance)}", color = AccentSoft, fontSize = 12.sp) } } }
            item { Advice("Debt", if (debt > 0) "You currently have ${money(debt)} of tracked outstanding debt. Prioritise high-cost debt before optional spending." else "No outstanding debt is currently recorded.") }
            item { Advice("EMI", if (emi > 0) "Your recurring EMI commitment is ${money(emi)} per month. Keep this amount reserved before discretionary spending." else "No recurring EMI is currently recorded.") }
            item { Advice("Contributions", "Your wife's contribution is tracked separately from your income, so household cash flow stays transparent without inflating your personal income.") }
            item { Text("AI recommendations will become data-driven after the final cloud/database integration.", color = TextMuted, fontSize = 10.sp) }
        }
    }
    @Composable private fun Advice(title: String, body: String) { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(15.dp)) { Text(title, color = AccentSoft, fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text(body, color = TextMain, fontSize = 12.sp, lineHeight = 18.sp) } } }

    @Composable private fun Settings(pad: PaddingValues, categories: List<String>, add: (String) -> Unit, remove: (String) -> Unit) {
        var newCategory by remember { mutableStateOf("") }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 105.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Settings", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Manage categories and app behaviour", color = TextMuted, fontSize = 11.sp) }
            item { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) { Text("Categories", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(8.dp)); Row(verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(newCategory, { newCategory = it }, label = { Text("New category") }, singleLine = true, modifier = Modifier.weight(1f)); Spacer(Modifier.width(8.dp)); Button(onClick = { if (newCategory.isNotBlank()) { add(newCategory); newCategory = "" } }) { Text("Add") } }; Spacer(Modifier.height(10.dp)); categories.forEach { c -> Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Text(c, Modifier.weight(1f), color = TextMain, fontSize = 13.sp); IconButton({ remove(c) }) { Icon(Icons.Default.Close, "Remove", tint = TextMuted) } } } } } }
            item { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(15.dp)) { Text("Cloud sync", color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("Database URL + Web API key + Room Code will be connected in the final integration step.", color = TextMuted, fontSize = 11.sp, lineHeight = 17.sp) } } }
        }
    }

    @Composable private fun TransactionDialog(repo: FinanceRepository, categories: List<String>, original: FinanceTransaction?, dismiss: () -> Unit, save: (FinanceTransaction) -> Unit) {
        var type by remember(original) { mutableStateOf(original?.type ?: TransactionType.EXPENSE) }; var owner by remember(original) { mutableStateOf(original?.owner ?: "Household") }; var amount by remember(original) { mutableStateOf(original?.amount?.toString() ?: "") }; var category by remember(original) { mutableStateOf(original?.category ?: categories.firstOrNull() ?: "Other") }; var description by remember(original) { mutableStateOf(original?.description ?: "") }; var date by remember(original) { mutableLongStateOf(original?.date ?: System.currentTimeMillis()) }
        AlertDialog(onDismissRequest = dismiss, containerColor = Panel2, title = { Text(if (original == null) "Add transaction" else "Edit transaction", color = TextMain) }, text = { Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income", TransactionType.CONTRIBUTION to "Contribution").forEach { (t,label) -> FilterChip(type==t,{type=t},label={Text(label,fontSize=9.sp)}) } }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Household","Mine","Wife").forEach { o -> FilterChip(owner==o,{owner=o},label={Text(o,fontSize=9.sp)}) } }
            OutlinedTextField(amount,{amount=it.filter{c->c.isDigit()||c=='.'}},label={Text("Amount")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("Category",color=TextMuted,fontSize=10.sp); Column { categories.chunked(3).forEach { row -> Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) { row.forEach { c -> FilterChip(category==c,{category=c},label={Text(c,fontSize=8.sp)}) } } } }
            OutlinedTextField(description,{description=it},label={Text("Description")},singleLine=true,modifier=Modifier.fillMaxWidth())
            TextButton(onClick={pickDate(date){date=it}}){Text("Date: ${formatDate(date)}",color=AccentSoft)}
        } }, confirmButton={TextButton(enabled=amount.toDoubleOrNull()?.let{it>0}==true,onClick={save(FinanceTransaction(original?.id?:0,type,owner,amount.toDouble(),category,description,date,System.currentTimeMillis(),false))}){Text("SAVE",color=AccentSoft)}},dismissButton={TextButton(dismiss){Text("CANCEL",color=TextMuted)}})
    }

    @Composable private fun LoanDialog(original: Loan?, dismiss: () -> Unit, save: (Loan) -> Unit) {
        var lender by remember(original) { mutableStateOf(original?.lender ?: "") }; var type by remember(original) { mutableStateOf(original?.type ?: LoanType.EMI) }; var product by remember(original) { mutableStateOf(original?.product ?: "") }; var originalAmount by remember(original) { mutableStateOf(original?.originalAmount?.toString() ?: "") }; var monthly by remember(original) { mutableStateOf(original?.monthlyPayment?.toString() ?: "") }; var tenure by remember(original) { mutableStateOf(original?.tenureMonths?.toString() ?: "") }; var paid by remember(original) { mutableStateOf(original?.paidMonths?.toString() ?: "") }; var start by remember(original) { mutableLongStateOf(original?.startDate ?: System.currentTimeMillis()) }; var due by remember(original) { mutableLongStateOf(original?.nextDueDate ?: System.currentTimeMillis()) }
        val remaining = (tenure.toIntOrNull() ?: 0) - (paid.toIntOrNull() ?: 0)
        AlertDialog(onDismissRequest=dismiss,containerColor=Panel2,title={Text(if(original==null)"Add loan" else "Edit loan",color=TextMain)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(lender,{lender=it},label={Text("Lender / loan name")},singleLine=true,modifier=Modifier.fillMaxWidth()); Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf(LoanType.EMI to "EMI",LoanType.LOAN to "Loan",LoanType.PAY_LATER to "Pay Later").forEach{(t,l)->FilterChip(type==t,{type=t},label={Text(l,fontSize=9.sp)})}}
            OutlinedTextField(product,{product=it},label={Text("Product / purpose")},singleLine=true,modifier=Modifier.fillMaxWidth()); OutlinedTextField(originalAmount,{originalAmount=it},label={Text("Original amount")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth())
            if(type!=LoanType.PAY_LATER){OutlinedTextField(monthly,{monthly=it},label={Text("Monthly EMI")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,modifier=Modifier.fillMaxWidth()); Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){OutlinedTextField(tenure,{tenure=it.filter(Char::isDigit)},label={Text("Tenure months")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.weight(1f));OutlinedTextField(paid,{paid=it.filter(Char::isDigit)},label={Text("Paid EMIs")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,modifier=Modifier.weight(1f))}; Text("${remaining.coerceAtLeast(0)} EMI month(s) remaining • ${money((monthly.toDoubleOrNull()?:0.0)*remaining.coerceAtLeast(0))} remaining",color=AccentSoft,fontSize=10.sp)} else {Text("Pay Later is tracked as debt. EMI and tenure are optional.",color=TextMuted,fontSize=10.sp)}
            Row{TextButton({pickDate(start){start=it}}){Text("Start ${formatDate(start)}",color=AccentSoft,fontSize=9.sp)};TextButton({pickDate(due){due=it}}){Text("Due ${formatDate(due)}",color=AccentSoft,fontSize=9.sp)}}
        }},confirmButton={TextButton(enabled=lender.isNotBlank()&&originalAmount.toDoubleOrNull()?.let{it>0}==true,onClick={save(Loan(original?.id?:0,lender,type,product,originalAmount.toDouble(),monthly.toDoubleOrNull()?:0.0,tenure.toIntOrNull()?:0,paid.toIntOrNull()?:0,start,due))}){Text("SAVE LOAN",color=AccentSoft)}},dismissButton={TextButton(dismiss){Text("CANCEL",color=TextMuted)}})
    }

    private fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val c = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(this, { _, y, m, d ->
            onPicked(Calendar.getInstance().apply {
                set(y, m, d, 12, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis)
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }

    @Composable private fun TypeIcon(type: TransactionType) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Panel2),contentAlignment=Alignment.Center){Icon(when(type){TransactionType.EXPENSE->Icons.Default.ShoppingCart;TransactionType.INCOME->Icons.Default.ArrowDownward;TransactionType.CONTRIBUTION->Icons.Default.Handshake},null,tint=AccentSoft)} }
    @Composable private fun Empty(text:String){Box(Modifier.fillMaxWidth().padding(45.dp),contentAlignment=Alignment.Center){Text(text,color=TextMuted,fontSize=12.sp,textAlign=TextAlign.Center)}}
    private fun money(v:Double)=NumberFormat.getCurrencyInstance(Locale("en","IN")).format(v)
    private fun formatDate(t:Long)=SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(t))
}
