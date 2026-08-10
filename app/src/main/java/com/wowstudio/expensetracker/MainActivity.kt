package com.wowstudio.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.lifecycleScope
import com.wowstudio.expensetracker.data.Expense
import com.wowstudio.expensetracker.data.ExpenseRepository
import com.wowstudio.expensetracker.widget.ExpenseWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val Bg = Color(0xFF080A0F)
private val Panel = Color(0xFF121722)
private val Panel2 = Color(0xFF171D29)
private val Line = Color(0xFF252C39)
private val Accent = Color(0xFF7C5CFF)
private val AccentSoft = Color(0xFFB8A8FF)
private val TextMain = Color(0xFFF4F6FA)
private val TextMuted = Color(0xFF8F99AA)
private val Green = Color(0xFF48D597)
private val Red = Color(0xFFFF6E7D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseApp() }
    }

    private fun refreshWidget() { lifecycleScope.launch { ExpenseWidget().updateAll(this@MainActivity) } }

    @Composable
    private fun ExpenseApp() {
        val repo = (application as ExpenseTrackerApp).repository
        val scope = rememberCoroutineScope()
        var offset by remember { mutableIntStateOf(0) }
        var tab by remember { mutableIntStateOf(0) }
        var editor by remember { mutableStateOf(false) }
        var syncDialog by remember { mutableStateOf(false) }
        var details by remember { mutableStateOf<Expense?>(null) }
        var editing by remember { mutableStateOf<Expense?>(null) }
        var refresh by remember { mutableIntStateOf(0) }
        var syncStatus by remember { mutableStateOf(if (repo.isSyncConfigured()) "Synced" else "Local") }

        LaunchedEffect(repo.isSyncConfigured()) {
            while (repo.isSyncConfigured()) {
                when (repo.syncNow()) {
                    is ExpenseRepository.SyncResult.Synced -> { syncStatus = "Synced"; refresh++ }
                    is ExpenseRepository.SyncResult.Error -> syncStatus = "Sync error"
                    ExpenseRepository.SyncResult.NotConfigured -> syncStatus = "Local"
                }
                delay(5000)
            }
        }

        val month = remember(offset, refresh) { Calendar.getInstance().apply { add(Calendar.MONTH, offset); set(Calendar.DAY_OF_MONTH, 1) } }
        val year = month.get(Calendar.YEAR)
        val mon = month.get(Calendar.MONTH)
        val expenses = repo.getExpensesForMonth(year, mon)
        val total = expenses.sumOf { it.amount }
        val previous = previousMonthTotal(repo, month)
        val categories = expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }.toList().sortedByDescending { it.second }

        MaterialTheme(colorScheme = darkColorScheme(primary = Accent, secondary = AccentSoft, background = Bg, surface = Panel)) {
            Scaffold(
                containerColor = Bg,
                topBar = { Header(syncStatus, { scope.launch { syncStatus = "Syncing..."; repo.syncNow(); refresh++; syncStatus = if (repo.isSyncConfigured()) "Synced" else "Local" } }, { syncDialog = true }) },
                bottomBar = { BottomTabs(tab) { tab = it } },
                floatingActionButton = {
                    FloatingActionButton(onClick = { editing = null; editor = true }, containerColor = Accent, shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Default.Add, "Add", tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
            ) { pad ->
                when (tab) {
                    0 -> Dashboard(pad, month, total, expenses, previous, categories, { offset--; refresh++ }, { if (offset < 0) { offset++; refresh++ } }, { tab = 1 }, { details = it })
                    1 -> ExpensesScreen(pad, month, expenses, { editing = it; editor = true }, { repo.delete(it); refresh++; refreshWidget(); scope.launch { repo.syncNow() } }, { details = it })
                    2 -> CalendarScreen(pad, month, expenses, { offset--; refresh++ }, { if (offset < 0) { offset++; refresh++ } }, { details = it })
                    else -> ChartsScreen(pad, month, expenses, categories, total, previous)
                }
            }
        }

        if (editor) {
            ExpenseEditor(editing, month.timeInMillis, { editor = false }) { id, amount, category, description, date ->
                if (id == null) repo.add(amount, category, description, date) else repo.update(id, amount, category, description, date)
                editor = false; refresh++; refreshWidget(); scope.launch { repo.syncNow() }
            }
        }
        details?.let { e -> DetailDialog(e, { details = null }) { editing = e; details = null; editor = true } }
        if (syncDialog) SyncDialog(repo, { syncDialog = false }) { syncStatus = "Syncing..."; scope.launch { repo.syncNow(); refresh++ } }
    }

    @Composable private fun Header(status: String, sync: () -> Unit, settings: () -> Unit) {
        Row(Modifier.fillMaxWidth().background(Bg).padding(18.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Expense Tracker", color = TextMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(if (status == "Synced") Green else TextMuted)); Spacer(Modifier.width(6.dp)); Text(status, color = TextMuted, fontSize = 11.sp) }
            }
            IconButton(sync) { Icon(Icons.Default.CloudSync, "Sync", tint = AccentSoft) }
            IconButton(settings) { Icon(Icons.Default.Settings, "Settings", tint = TextMuted) }
        }
    }

    @Composable private fun BottomTabs(tab: Int, select: (Int) -> Unit) {
        NavigationBar(containerColor = Color(0xFF10151E), tonalElevation = 0.dp) {
            NavigationBarItem(tab == 0, { select(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Dashboard") })
            NavigationBarItem(tab == 1, { select(1) }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Expenses") })
            NavigationBarItem(tab == 2, { select(2) }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Calendar") })
            NavigationBarItem(tab == 3, { select(3) }, icon = { Icon(Icons.Default.PieChart, null) }, label = { Text("Charts") })
        }
    }

    @Composable private fun Dashboard(pad: PaddingValues, month: Calendar, total: Double, expenses: List<Expense>, previous: Double, cats: List<Pair<String, Double>>, prev: () -> Unit, next: () -> Unit, openExpenses: () -> Unit, openDetail: (Expense) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 6.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { MonthHeader(month, prev, next) }
            item { BalanceCard(total, expenses.size, previous) }
            item { Section("Top categories", "All", openExpenses) }
            item { CategoryCards(cats.take(5), total) }
            item { Section("Recent transactions", "View all", openExpenses) }
            items(expenses.take(8), key = { it.id }) { ExpenseRow(it, openDetail) }
            if (expenses.isEmpty()) item { Empty("No transactions this month") }
        }
    }

    @Composable private fun MonthHeader(month: Calendar, prev: () -> Unit, next: () -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(prev) { Icon(Icons.Default.ChevronLeft, "Previous", tint = TextMain) }
            Text(monthTitle(month), Modifier.weight(1f), textAlign = TextAlign.Center, color = TextMain, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = next, enabled = !isCurrentMonth(month)) { Icon(Icons.Default.ChevronRight, "Next", tint = TextMain) }
        }
    }

    @Composable private fun BalanceCard(total: Double, count: Int, previous: Double) {
        val delta = if (previous > 0) ((total - previous) / previous) * 100 else 0.0
        Card(colors = CardDefaults.cardColors(Panel2), shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text("MONTHLY SPENDING", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp)); Text(money(total), color = TextMain, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("$count transactions", color = TextMuted, fontSize = 12.sp)
                    }
                    Box(Modifier.size(50.dp).clip(CircleShape).background(Color(0xFF241E3C)), contentAlignment = Alignment.Center) { Icon(Icons.Default.AccountBalanceWallet, null, tint = AccentSoft) }
                }
                Spacer(Modifier.height(18.dp)); HorizontalDivider(color = Line); Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (delta <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp, null, tint = if (delta <= 0) Green else Red, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp)); Text(if (previous > 0) "${kotlin.math.abs(delta).toInt()}% vs previous month" else "No previous month data", color = TextMuted, fontSize = 12.sp)
                }
            }
        }
    }

    @Composable private fun CategoryCards(cats: List<Pair<String, Double>>, total: Double) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cats) { (cat, value) ->
                Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(145.dp)) {
                    Column(Modifier.padding(14.dp)) { CategoryIcon(cat, Modifier.size(38.dp)); Spacer(Modifier.height(10.dp)); Text(cat, color = TextMain, fontWeight = FontWeight.SemiBold, maxLines = 1); Text(money(value), color = AccentSoft, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(percent(value, total), color = TextMuted, fontSize = 11.sp) }
                }
            }
        }
    }

    @Composable private fun Section(title: String, action: String?, onClick: (() -> Unit)?) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, Modifier.weight(1f), color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold); if (action != null && onClick != null) Text(action, Modifier.clickable(onClick = onClick), color = AccentSoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }

    @Composable private fun ExpensesScreen(pad: PaddingValues, month: Calendar, expenses: List<Expense>, edit: (Expense) -> Unit, delete: (Long) -> Unit, detail: (Expense) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Expenses", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text(monthTitle(month), color = TextMuted, fontSize = 13.sp) }; Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Tune, null, tint = TextMuted, modifier = Modifier.padding(10.dp)) } } }
            items(expenses, key = { it.id }) { ExpenseRow(it, detail, edit, delete) }
            if (expenses.isEmpty()) item { Empty("No expenses this month") }
        }
    }

    @Composable private fun ExpenseRow(e: Expense, detail: (Expense) -> Unit, edit: ((Expense) -> Unit)? = null, delete: ((Long) -> Unit)? = null) {
        Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { detail(e) }) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(e.category, Modifier.size(50.dp)); Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) { Text(e.category, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold); Text(e.description.ifBlank { "No description" }, color = TextMuted, fontSize = 12.sp, maxLines = 1); Text(formatDate(e.date), color = TextMuted, fontSize = 11.sp) }
                Column(horizontalAlignment = Alignment.End) { Text(money(e.amount), color = TextMain, fontWeight = FontWeight.Bold); if (edit != null && delete != null) Row { IconButton({ edit(e) }, Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", tint = AccentSoft, modifier = Modifier.size(17.dp)) }; IconButton({ delete(e.id) }, Modifier.size(32.dp)) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Red, modifier = Modifier.size(18.dp)) } } }
            }
        }
    }

    @Composable private fun CalendarScreen(pad: PaddingValues, month: Calendar, expenses: List<Expense>, prev: () -> Unit, next: () -> Unit, detail: (Expense) -> Unit) {
        val grouped = expenses.groupBy { dayOfMonth(it.date) }
        val days = calendarDays(month)
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MonthHeader(month, prev, next) }
            item {
                Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth()) { listOf("S","M","T","W","T","F","S").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) } }
                    Spacer(Modifier.height(8.dp))
                    days.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) { week.forEach { d -> Box(Modifier.weight(1f).height(48.dp), contentAlignment = Alignment.Center) { if (d > 0) { val active = grouped[d].orEmpty().isNotEmpty(); Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(32.dp).clip(CircleShape).background(if (active) Color(0xFF251F40) else Color.Transparent), contentAlignment = Alignment.Center) { Text(d.toString(), color = TextMain, fontSize = 12.sp) }; if (active) Box(Modifier.size(4.dp).clip(CircleShape).background(Accent)) } } } } } }
                } }
            }
            item { Section("Transactions", null, null) }
            items(expenses.sortedByDescending { it.date }.take(12), key = { "c${it.id}" }) { ExpenseRow(it, detail) }
            if (expenses.isEmpty()) item { Empty("No transactions this month") }
        }
    }

    @Composable private fun ChartsScreen(pad: PaddingValues, month: Calendar, expenses: List<Expense>, cats: List<Pair<String, Double>>, total: Double, previous: Double) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Charts", color = TextMain, fontSize = 27.sp, fontWeight = FontWeight.Bold); Text(monthTitle(month), color = TextMuted, fontSize = 13.sp) }
            item { LineChartCard(month, expenses) }
            item { DonutCard(cats, total) }
            item { Comparison(total, previous) }
        }
    }

    @Composable private fun LineChartCard(month: Calendar, expenses: List<Expense>) {
        val maxDay = month.getActualMaximum(Calendar.DAY_OF_MONTH)
        val daily = remember(expenses, month.timeInMillis) { (1..maxDay).map { d -> expenses.filter { dayOfMonth(it.date) == d }.sumOf { it.amount } } }
        val max = daily.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp)) {
            Text("Income / Expense activity", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text("Actual daily expense values", color = TextMuted, fontSize = 11.sp); Spacer(Modifier.height(12.dp))
            Canvas(Modifier.fillMaxWidth().height(170.dp)) {
                for (i in 1..4) { val y = size.height * i / 5f; drawLine(Line, Offset(0f,y), Offset(size.width,y), 1f) }
                if (daily.isNotEmpty()) { val step = size.width / kotlin.math.max(1, daily.lastIndex); for (i in 0 until daily.lastIndex) { val a = Offset(i * step, size.height * (1f - (daily[i] / max).toFloat())); val b = Offset((i+1)*step, size.height * (1f - (daily[i+1] / max).toFloat())); drawLine(Accent, a, b, 4f, StrokeCap.Round) }; daily.forEachIndexed { i,v -> drawCircle(AccentSoft, 3.5f, Offset(i*step, size.height*(1f-(v/max).toFloat()))) } }
            }
        } }
    }

    @Composable private fun DonutCard(cats: List<Pair<String, Double>>, total: Double) {
        Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp)) {
            Text("Expenses by category", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(155.dp), contentAlignment = Alignment.Center) { Canvas(Modifier.fillMaxSize()) { var start = -90f; cats.take(7).forEachIndexed { i, item -> val sweep = if (total > 0) (item.second / total * 360f).toFloat() else 0f; drawArc(chartColor(i), start, sweep, false, style = androidx.compose.ui.graphics.drawscope.Stroke(27f)); start += sweep } }; Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Total", color = TextMuted, fontSize = 10.sp); Text(money(total), color = TextMain, fontSize = 14.sp, fontWeight = FontWeight.Bold) } }
                Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) { cats.take(7).forEachIndexed { i,(cat,v) -> Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(7.dp).clip(CircleShape).background(chartColor(i))); Spacer(Modifier.width(7.dp)); Text(cat, Modifier.weight(1f), color = TextMuted, fontSize = 11.sp, maxLines = 1); Text(percent(v,total), color = TextMain, fontSize = 11.sp, fontWeight = FontWeight.Bold) } } }
            }
        } }
    }

    @Composable private fun Comparison(total: Double, previous: Double) { Card(colors = CardDefaults.cardColors(Panel), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(18.dp)) { Text("Month comparison", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.height(12.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Stat("This month", total, AccentSoft); Stat("Previous", previous, TextMuted); Stat("Difference", kotlin.math.abs(total-previous), if (total <= previous) Green else Red) } } } }
    @Composable private fun Stat(label: String, value: Double, color: Color) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(label, color = TextMuted, fontSize = 10.sp); Text(money(value), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold) } }

    @Composable private fun CategoryIcon(category: String, modifier: Modifier) { val icon = when(category.uppercase()) { "FOOD" -> Icons.Default.Restaurant; "RENT","HOME","HOUSING" -> Icons.Default.HomeWork; "LOAN" -> Icons.Default.AccountBalance; "SHOPPING" -> Icons.Default.ShoppingCart; "EDUCATION" -> Icons.Default.School; "EB","ELECTRICITY" -> Icons.Default.Bolt; "TRAVEL","TRANSPORT" -> Icons.Default.DirectionsCar; "MEDICAL" -> Icons.Default.MedicalServices; "ENTERTAINMENT" -> Icons.Default.Movie; "CARE TAKER" -> Icons.Default.Person; else -> Icons.Default.Category }; Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF241E3C)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = AccentSoft, modifier = Modifier.size(23.dp)) } }

    @Composable private fun ExpenseEditor(expense: Expense?, defaultDate: Long, dismiss: () -> Unit, save: (Long?,Double,String,String,Long) -> Unit) {
        var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
        var category by remember(expense) { mutableStateOf(expense?.category ?: "FOOD") }
        var description by remember(expense) { mutableStateOf(expense?.description ?: "") }
        val cats = listOf("FOOD","RENT","LOAN","HOME","CARE TAKER","EDUCATION","SHOPPING","EB","TRAVEL","MEDICAL","OTHER")
        AlertDialog(onDismissRequest = dismiss, containerColor = Panel2, title = { Text(if (expense == null) "Add expense" else "Edit expense", color = TextMain) }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' } }, label = { Text("Amount") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Category", color = TextMuted, fontSize = 11.sp)
            cats.chunked(4).forEach { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) { row.forEach { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c, fontSize = 10.sp, maxLines = 1) }) } } }
            OutlinedTextField(description, { description = it }, label = { Text("Description / note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Date: ${formatDate(expense?.date ?: defaultDate)}", color = TextMuted, fontSize = 11.sp)
        } }, confirmButton = { TextButton(enabled = amount.toDoubleOrNull()?.let { it > 0 } == true && category.isNotBlank(), onClick = { save(expense?.id, amount.toDouble(), category, description, expense?.date ?: defaultDate) }) { Text("SAVE", color = AccentSoft) } }, dismissButton = { TextButton(dismiss) { Text("CANCEL", color = TextMuted) } })
    }

    @Composable private fun DetailDialog(e: Expense, dismiss: () -> Unit, edit: () -> Unit) { AlertDialog(onDismissRequest = dismiss, containerColor = Panel2, title = { Text("Transaction details", color = TextMain) }, text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { CategoryIcon(e.category, Modifier.size(50.dp)); Spacer(Modifier.width(12.dp)); Column { Text(e.category, color = TextMain, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(money(e.amount), color = AccentSoft, fontSize = 20.sp, fontWeight = FontWeight.Bold) } }; Detail("Date", formatDate(e.date)); Detail("Description", e.description.ifBlank { "No description" }); Detail("Category", e.category) } }, confirmButton = { TextButton(edit) { Text("EDIT", color = AccentSoft) } }, dismissButton = { TextButton(dismiss) { Text("CLOSE", color = TextMuted) } }) }
    @Composable private fun Detail(label: String, value: String) { Column { Text(label.uppercase(), color = TextMuted, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(value, color = TextMain, fontSize = 13.sp) } }

    @Composable private fun SyncDialog(repo: ExpenseRepository, dismiss: () -> Unit, saved: () -> Unit) { var url by remember { mutableStateOf(repo.syncDatabaseUrl()) }; var key by remember { mutableStateOf(repo.syncApiKey()) }; var room by remember { mutableStateOf(repo.syncRoom()) }; AlertDialog(onDismissRequest = dismiss, containerColor = Panel2, title = { Text("Cloud sync", color = TextMain) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(url,{url=it},label={Text("Database URL")},singleLine=true,modifier=Modifier.fillMaxWidth()); OutlinedTextField(key,{key=it},label={Text("Web API key")},singleLine=true,modifier=Modifier.fillMaxWidth()); OutlinedTextField(room,{room=it},label={Text("Room code")},singleLine=true,modifier=Modifier.fillMaxWidth()) } }, confirmButton = { TextButton(enabled=url.isNotBlank()&&key.isNotBlank()&&room.isNotBlank(),onClick={repo.configureSync(url,key,room);saved();dismiss()}){Text("SAVE & SYNC",color=AccentSoft)} }, dismissButton={TextButton(dismiss){Text("CANCEL",color=TextMuted)}}) }

    @Composable private fun Empty(text: String) { Box(Modifier.fillMaxWidth().padding(50.dp),contentAlignment=Alignment.Center){Text(text,color=TextMuted,fontSize=13.sp)} }

    private fun previousMonthTotal(repo: ExpenseRepository, month: Calendar): Double { val c = month.clone() as Calendar; c.add(Calendar.MONTH,-1); return repo.getExpensesForMonth(c.get(Calendar.YEAR),c.get(Calendar.MONTH)).sumOf { it.amount } }
    private fun monthTitle(c: Calendar) = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(c.time)
    private fun formatDate(t: Long) = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(t))
    private fun money(v: Double) = NumberFormat.getCurrencyInstance(Locale("en","IN")).format(v)
    private fun percent(v: Double,total:Double) = if(total>0) "${(v/total*100).toInt()}%" else "0%"
    private fun dayOfMonth(t: Long) = Calendar.getInstance().apply { timeInMillis=t }.get(Calendar.DAY_OF_MONTH)
    private fun isCurrentMonth(c: Calendar) = c.get(Calendar.YEAR)==Calendar.getInstance().get(Calendar.YEAR)&&c.get(Calendar.MONTH)==Calendar.getInstance().get(Calendar.MONTH)
    private fun calendarDays(month: Calendar): List<Int> { val c=month.clone() as Calendar; c.set(Calendar.DAY_OF_MONTH,1); val lead=c.get(Calendar.DAY_OF_WEEK)-1; val max=c.getActualMaximum(Calendar.DAY_OF_MONTH); return List(lead){0}+(1..max).toList() }
    private fun chartColor(i:Int)=listOf(Color(0xFF7C5CFF),Color(0xFF4C8DFF),Color(0xFF27C7A7),Color(0xFFFFB45A),Color(0xFFFF6E7D),Color(0xFF78879B),Color(0xFFB68CFF))[i%7]
}