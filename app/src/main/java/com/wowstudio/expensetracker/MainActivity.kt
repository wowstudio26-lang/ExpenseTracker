package com.wowstudio.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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

private val AppBackground = Color(0xFF080A0F)
private val Surface = Color(0xFF121620)
private val Surface2 = Color(0xFF171C27)
private val Border = Color(0xFF232A38)
private val Primary = Color(0xFF8B5CF6)
private val PrimarySoft = Color(0xFFB9A3FF)
private val TextPrimary = Color(0xFFF5F7FB)
private val TextSecondary = Color(0xFF98A1B2)
private val Positive = Color(0xFF45D483)
private val Negative = Color(0xFFFF7180)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseApp() }
    }

    private fun refreshWidget() {
        lifecycleScope.launch { ExpenseWidget().updateAll(this@MainActivity) }
    }

    @Composable
    private fun ExpenseApp() {
        val repo = (application as ExpenseTrackerApp).repository
        val scope = rememberCoroutineScope()
        var monthOffset by remember { mutableIntStateOf(0) }
        var tab by remember { mutableIntStateOf(0) }
        var showEditor by remember { mutableStateOf(false) }
        var showSync by remember { mutableStateOf(false) }
        var showDetails by remember { mutableStateOf<Expense?>(null) }
        var editingExpense by remember { mutableStateOf<Expense?>(null) }
        var refreshKey by remember { mutableIntStateOf(0) }
        var syncStatus by remember { mutableStateOf(if (repo.isSyncConfigured()) "Synced" else "Local data") }

        LaunchedEffect(repo.isSyncConfigured()) {
            while (repo.isSyncConfigured()) {
                when (val result = repo.syncNow()) {
                    is ExpenseRepository.SyncResult.Synced -> {
                        syncStatus = "Synced"
                        refreshKey++
                    }
                    is ExpenseRepository.SyncResult.Error -> syncStatus = "Sync error"
                    ExpenseRepository.SyncResult.NotConfigured -> syncStatus = "Local data"
                }
                delay(5000)
            }
        }

        val selectedMonth = remember(monthOffset, refreshKey) { monthCalendar(monthOffset) }
        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)
        val expenses = repo.getExpensesForMonth(year, month)
        val total = expenses.sumOf { it.amount }
        val top = expenses.groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
        val previousTotal = repo.getExpensesForMonth(year, month - 1).sumOf { it.amount }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Primary,
                secondary = PrimarySoft,
                background = AppBackground,
                surface = Surface,
                onBackground = TextPrimary,
                onSurface = TextPrimary
            )
        ) {
            Scaffold(
                containerColor = AppBackground,
                topBar = {
                    AppTopBar(
                        syncStatus = syncStatus,
                        onSync = {
                            scope.launch {
                                syncStatus = "Syncing..."
                                when (repo.syncNow()) {
                                    is ExpenseRepository.SyncResult.Synced -> syncStatus = "Synced"
                                    is ExpenseRepository.SyncResult.Error -> syncStatus = "Sync error"
                                    ExpenseRepository.SyncResult.NotConfigured -> syncStatus = "Local data"
                                }
                                refreshKey++
                            }
                        },
                        onSettings = { showSync = true }
                    )
                },
                bottomBar = {
                    AppBottomBar(tab = tab, onTabSelected = { tab = it })
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { editingExpense = null; showEditor = true },
                        containerColor = Primary,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add expense", modifier = Modifier.size(28.dp))
                    }
                }
            ) { pad ->
                when (tab) {
                    0 -> HomeScreen(
                        pad = pad,
                        month = selectedMonth,
                        total = total,
                        count = expenses.size,
                        top = top,
                        previousTotal = previousTotal,
                        onPrevious = { monthOffset--; refreshKey++ },
                        onNext = { if (monthOffset < 0) { monthOffset++; refreshKey++ } },
                        onOpenExpenses = { tab = 1 },
                        onExpenseClick = { showDetails = it }
                    )
                    1 -> ExpenseListScreen(
                        pad = pad,
                        month = selectedMonth,
                        expenses = expenses,
                        onEdit = { editingExpense = it; showEditor = true },
                        onDelete = { id ->
                            repo.delete(id)
                            refreshKey++
                            refreshWidget()
                            scope.launch { repo.syncNow() }
                        },
                        onDetails = { showDetails = it }
                    )
                    2 -> CalendarScreen(
                        pad = pad,
                        month = selectedMonth,
                        expenses = expenses,
                        onPrevious = { monthOffset--; refreshKey++ },
                        onNext = { if (monthOffset < 0) { monthOffset++; refreshKey++ } },
                        onDetails = { showDetails = it }
                    )
                    else -> ChartsScreen(
                        pad = pad,
                        month = selectedMonth,
                        total = total,
                        expenses = expenses,
                        top = top,
                        previousTotal = previousTotal
                    )
                }
            }
        }

        if (showEditor) {
            ExpenseEditorDialog(
                expense = editingExpense,
                defaultDate = selectedMonth.timeInMillis,
                onDismiss = { showEditor = false }
            ) { id, amount, category, description, date ->
                if (id == null) repo.add(amount, category, description, date)
                else repo.update(id, amount, category, description, date)
                showEditor = false
                refreshKey++
                refreshWidget()
                scope.launch { repo.syncNow() }
            }
        }

        if (showDetails != null) {
            ExpenseDetailsDialog(
                expense = showDetails!!,
                onDismiss = { showDetails = null },
                onEdit = {
                    editingExpense = showDetails
                    showDetails = null
                    showEditor = true
                }
            )
        }

        if (showSync) {
            SyncDialog(repo, { showSync = false }) {
                syncStatus = "Syncing..."
                scope.launch { repo.syncNow(); refreshKey++ }
            }
        }
    }

    @Composable
    private fun AppTopBar(syncStatus: String, onSync: () -> Unit, onSettings: () -> Unit) {
        Row(
            Modifier.fillMaxWidth().background(AppBackground).padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Expense Tracker", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (syncStatus == "Synced") Positive else TextSecondary))
                    Spacer(Modifier.width(6.dp))
                    Text(syncStatus, color = TextSecondary, fontSize = 11.sp)
                }
            }
            IconButton(onClick = onSync) { Icon(Icons.Default.CloudSync, "Sync", tint = PrimarySoft) }
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "Settings", tint = TextSecondary) }
        }
    }

    @Composable
    private fun AppBottomBar(tab: Int, onTabSelected: (Int) -> Unit) {
        NavigationBar(containerColor = Color(0xFF10141C), tonalElevation = 0.dp) {
            NavigationBarItem(tab == 0, { onTabSelected(0) }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationBarItem(tab == 1, { onTabSelected(1) }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Expenses") })
            NavigationBarItem(tab == 2, { onTabSelected(2) }, icon = { Icon(Icons.Default.CalendarMonth, null) }, label = { Text("Calendar") })
            NavigationBarItem(tab == 3, { onTabSelected(3) }, icon = { Icon(Icons.Default.PieChart, null) }, label = { Text("Charts") })
        }
    }

    @Composable
    private fun HomeScreen(
        pad: PaddingValues,
        month: Calendar,
        total: Double,
        count: Int,
        top: List<Pair<String, Double>>,
        previousTotal: Double,
        onPrevious: () -> Unit,
        onNext: () -> Unit,
        onOpenExpenses: () -> Unit,
        onExpenseClick: (Expense) -> Unit
    ) {
        val recent = top.take(4)
        LazyColumn(
            Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                MonthSwitcher(month, onPrevious, onNext)
            }
            item {
                SummaryCard(total, count, previousTotal)
            }
            item {
                SectionHeader("This month", "View all", onOpenExpenses)
            }
            item {
                CategoryStrip(recent, total)
            }
            item {
                SectionHeader("Spending by category", null, null)
            }
            items(top.take(7), key = { it.first }) { (category, value) ->
                CategoryAmountRow(category, value, total)
            }
        }
    }

    @Composable
    private fun MonthSwitcher(month: Calendar, onPrevious: () -> Unit, onNext: () -> Unit) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Previous month", tint = TextPrimary) }
            Text(monthTitle(month), Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onNext, enabled = month.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR) || month.get(Calendar.MONTH) != Calendar.getInstance().get(Calendar.MONTH)) {
                Icon(Icons.Default.ChevronRight, "Next month", tint = TextPrimary)
            }
        }
    }

    @Composable
    private fun SummaryCard(total: Double, count: Int, previousTotal: Double) {
        val change = if (previousTotal > 0) ((total - previousTotal) / previousTotal * 100.0) else 0.0
        Card(colors = CardDefaults.cardColors(containerColor = Surface2), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("TOTAL SPENT", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(money(total), color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("$count transactions", color = TextSecondary, fontSize = 12.sp)
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF211B37)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.AccountBalanceWallet, null, tint = PrimarySoft)
                    }
                }
                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(13.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (change <= 0) Icons.Default.TrendingDown else Icons.Default.TrendingUp, null, tint = if (change <= 0) Positive else Negative, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(7.dp))
                    Text(if (previousTotal > 0) "${kotlin.math.abs(change).toInt()}% vs previous month" else "No previous month data", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }

    @Composable
    private fun CategoryStrip(top: List<Pair<String, Double>>, total: Double) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(end = 4.dp)) {
            items(top) { (category, value) ->
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(18.dp), modifier = Modifier.width(142.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        CategoryIcon(category, Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(category, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text(money(value), color = PrimarySoft, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(percent(value, total), color = TextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun CategoryAmountRow(category: String, value: Double, total: Double) {
        val ratio = if (total > 0) (value / total).toFloat() else 0f
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            CategoryIcon(category, Modifier.size(40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(category, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(money(value), color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(10.dp)).background(Border)) {
                    Box(Modifier.fillMaxWidth(ratio.coerceIn(0f, 1f)).fillMaxHeight().clip(RoundedCornerShape(10.dp)).background(Primary))
                }
            }
        }
    }

    @Composable
    private fun SectionHeader(title: String, action: String?, onAction: (() -> Unit)?) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f), color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (action != null && onAction != null) Text(action, Modifier.clickable(onClick = onAction), color = PrimarySoft, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }

    @Composable
    private fun ExpenseListScreen(pad: PaddingValues, month: Calendar, expenses: List<Expense>, onEdit: (Expense) -> Unit, onDelete: (Long) -> Unit, onDetails: (Expense) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Transactions", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(monthTitle(month), color = TextSecondary, fontSize = 13.sp) }
                    Surface(shape = RoundedCornerShape(14.dp), color = Surface2) { Icon(Icons.Default.Tune, null, tint = TextSecondary, modifier = Modifier.padding(11.dp).size(18.dp)) }
                }
            }
            item { Spacer(Modifier.height(2.dp)) }
            if (expenses.isEmpty()) item { EmptyState("No expenses for this month") }
            items(expenses, key = { it.id }) { e ->
                ExpenseCard(e, onEdit, onDelete, onDetails)
            }
        }
    }

    @Composable
    private fun ExpenseCard(e: Expense, onEdit: (Expense) -> Unit, onDelete: (Long) -> Unit, onDetails: (Expense) -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable { onDetails(e) }) {
            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                CategoryIcon(e.category, Modifier.size(48.dp))
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(e.category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(e.description.ifBlank { "No description" }, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    Text(formatDate(e.date), color = Color(0xFF6F7787), fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(e.amount), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row {
                        IconButton(onClick = { onEdit(e) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Edit, "Edit", tint = PrimarySoft, modifier = Modifier.size(17.dp)) }
                        IconButton(onClick = { onDelete(e.id) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.DeleteOutline, "Delete", tint = Negative, modifier = Modifier.size(18.dp)) }
                    }
                }
            }
        }
    }

    @Composable
    private fun CalendarScreen(pad: PaddingValues, month: Calendar, expenses: List<Expense>, onPrevious: () -> Unit, onNext: () -> Unit, onDetails: (Expense) -> Unit) {
        val days = remember(month.timeInMillis, expenses) { calendarDays(month) }
        val byDay = expenses.groupBy { dayKey(it.date) }
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { MonthSwitcher(month, onPrevious, onNext) }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.padding(15.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { Text(it, Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(Modifier.height(8.dp))
                        days.chunked(7).forEach { week ->
                            Row(Modifier.fillMaxWidth()) {
                                week.forEach { day ->
                                    Box(Modifier.weight(1f).height(54.dp), contentAlignment = Alignment.Center) {
                                        if (day > 0) {
                                            val key = dayKey(month, day)
                                            val dayExpenses = byDay[key].orEmpty()
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(Modifier.size(30.dp).clip(CircleShape).background(if (dayExpenses.isNotEmpty()) Color(0xFF211B37) else Color.Transparent), contentAlignment = Alignment.Center) {
                                                    Text(day.toString(), color = TextPrimary, fontSize = 12.sp)
                                                }
                                                if (dayExpenses.isNotEmpty()) Box(Modifier.size(4.dp).clip(CircleShape).background(Primary))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { SectionHeader("Transactions", null, null) }
            items(expenses.sortedByDescending { it.date }.take(12), key = { "cal-${it.id}" }) { ExpenseCard(it, {}, {}, onDetails) }
            if (expenses.isEmpty()) item { EmptyState("No transactions this month") }
        }
    }

    @Composable
    private fun ChartsScreen(pad: PaddingValues, month: Calendar, total: Double, expenses: List<Expense>, top: List<Pair<String, Double>>, previousTotal: Double) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 110.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Insights", color = TextPrimary, fontSize = 25.sp, fontWeight = FontWeight.Bold); Text(monthTitle(month), color = TextSecondary, fontSize = 13.sp) }
            item { SpendingChart(expenses) }
            item { DonutCard(top, total) }
            item { ComparisonCard(total, previousTotal) }
        }
    }

    @Composable
    private fun SpendingChart(expenses: List<Expense>) {
        val points = remember(expenses) {
            val grouped = expenses.groupBy { dayOfMonth(it.date) }.mapValues { (_, v) -> v.sumOf { it.amount } }
            val max = grouped.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            (1..7).map { i -> (grouped.filterKeys { it >= i }.values.sum().coerceAtMost(max * 7) / (max * 7)).toFloat() }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Spending trend", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Daily activity", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(14.dp))
                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val width = size.width
                    val height = size.height
                    for (i in 1..4) {
                        val y = height * i / 5f
                        drawLine(Border, Offset(0f, y), Offset(width, y), 1f)
                    }
                    if (points.size > 1) {
                        val step = width / (points.size - 1)
                        for (i in 0 until points.lastIndex) {
                            drawLine(Primary, Offset(i * step, height * (1f - points[i])), Offset((i + 1) * step, height * (1f - points[i + 1])), 5f, StrokeCap.Round)
                        }
                        points.forEachIndexed { i, p -> drawCircle(PrimarySoft, 4f, Offset(i * step, height * (1f - p))) }
                    }
                }
            }
        }
    }

    @Composable
    private fun DonutCard(top: List<Pair<String, Double>>, total: Double) {
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Expenses by category", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                        Canvas(Modifier.fillMaxSize()) {
                            var start = -90f
                            top.take(6).forEachIndexed { index, item ->
                                val sweep = if (total > 0) (item.second / total * 360f).toFloat() else 0f
                                drawArc(categoryColor(index), start, sweep, false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 26f))
                                start += sweep
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Total", color = TextSecondary, fontSize = 11.sp); Text(money(total), color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        top.take(6).forEachIndexed { index, (category, value) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(categoryColor(index)))
                                Spacer(Modifier.width(7.dp))
                                Text(category, Modifier.weight(1f), color = TextSecondary, fontSize = 11.sp, maxLines = 1)
                                Text(percent(value, total), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ComparisonCard(total: Double, previous: Double) {
        val delta = total - previous
        Card(colors = CardDefaults.cardColors(containerColor = Surface), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Month comparison", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ComparisonValue("This month", total, PrimarySoft)
                    ComparisonValue("Previous", previous, TextSecondary)
                    ComparisonValue("Difference", kotlin.math.abs(delta), if (delta <= 0) Positive else Negative)
                }
            }
        }
    }

    @Composable
    private fun ComparisonValue(label: String, value: Double, color: Color) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextSecondary, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text(money(value), color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }

    @Composable
    private fun CategoryIcon(category: String, modifier: Modifier = Modifier) {
        val icon = when (category.uppercase()) {
            "FOOD" -> Icons.Default.Restaurant
            "RENT", "HOME", "HOUSING" -> Icons.Default.HomeWork
            "LOAN" -> Icons.Default.AccountBalance
            "SHOPPING" -> Icons.Default.ShoppingCart
            "EDUCATION" -> Icons.Default.School
            "EB", "ELECTRICITY" -> Icons.Default.Bolt
            "TRAVEL", "TRANSPORT" -> Icons.Default.DirectionsCar
            "MEDICAL" -> Icons.Default.MedicalServices
            "ENTERTAINMENT" -> Icons.Default.Movie
            "CARE TAKER" -> Icons.Default.Person
            else -> Icons.Default.Category
        }
        Box(modifier.clip(RoundedCornerShape(14.dp)).background(Color(0xFF211B37)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = PrimarySoft, modifier = Modifier.size(22.dp))
        }
    }

    @Composable
    private fun EmptyState(text: String) {
        Box(Modifier.fillMaxWidth().padding(vertical = 70.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(58.dp).clip(CircleShape).background(Surface2), contentAlignment = Alignment.Center) { Icon(Icons.Default.ReceiptLong, null, tint = TextSecondary) }
                Spacer(Modifier.height(12.dp))
                Text(text, color = TextSecondary, fontSize = 13.sp)
            }
        }
    }

    @Composable
    private fun ExpenseDetailsDialog(expense: Expense, onDismiss: () -> Unit, onEdit: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Surface2,
            title = { Text("Transaction details", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryIcon(expense.category, Modifier.size(52.dp))
                        Spacer(Modifier.width(12.dp))
                        Column { Text(expense.category, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(money(expense.amount), color = PrimarySoft, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                    }
                    DetailLine("Date", formatDate(expense.date))
                    DetailLine("Description", expense.description.ifBlank { "No description" })
                    DetailLine("Category", expense.category)
                }
            },
            confirmButton = { TextButton(onClick = onEdit) { Text("EDIT", color = PrimarySoft) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CLOSE", color = TextSecondary) } }
        )
    }

    @Composable
    private fun DetailLine(label: String, value: String) {
        Column {
            Text(label.uppercase(), color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(value, color = TextPrimary, fontSize = 13.sp)
        }
    }

    @Composable
    private fun ExpenseEditorDialog(expense: Expense?, defaultDate: Long, onDismiss: () -> Unit, onSave: (Long?, Double, String, String, Long) -> Unit) {
        var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
        var category by remember(expense) { mutableStateOf(expense?.category ?: "FOOD") }
        var description by remember(expense) { mutableStateOf(expense?.description ?: "") }
        val date = expense?.date ?: defaultDate
        val categories = listOf("FOOD", "RENT", "LOAN", "HOME", "CARE TAKER", "EDUCATION", "SHOPPING", "EB", "TRAVEL", "OTHER")
        val validAmount = amount.toDoubleOrNull()?.takeIf { it > 0 } != null
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Surface2,
            title = { Text(if (expense == null) "Add expense" else "Edit expense", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    Text("Category", color = TextSecondary, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(categories) { c -> FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c) }) } }
                    OutlinedTextField(category, { category = it.uppercase() }, label = { Text("Category name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Date: ${formatDate(date)}", color = TextSecondary, fontSize = 11.sp)
                }
            },
            confirmButton = { TextButton(enabled = validAmount && category.isNotBlank(), onClick = { onSave(expense?.id, amount.toDouble(), category, description, date) }) { Text("SAVE", color = PrimarySoft) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) } }
        )
    }

    @Composable
    private fun SyncDialog(repo: ExpenseRepository, onDismiss: () -> Unit, onSaved: () -> Unit) {
        var url by remember { mutableStateOf(repo.syncDatabaseUrl()) }
        var key by remember { mutableStateOf(repo.syncApiKey()) }
        var room by remember { mutableStateOf(repo.syncRoom()) }
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = Surface2,
            title = { Text("Cloud sync", color = TextPrimary) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sync settings are unchanged. This screen is only restyled.", color = TextSecondary, fontSize = 11.sp)
                OutlinedTextField(url, { url = it }, label = { Text("Database URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(key, { key = it }, label = { Text("Web API key") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(room, { room = it }, label = { Text("Room code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            } },
            confirmButton = { TextButton(enabled = url.isNotBlank() && key.isNotBlank() && room.isNotBlank(), onClick = { repo.configureSync(url, key, room); onSaved(); onDismiss() }) { Text("SAVE & SYNC", color = PrimarySoft) } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL", color = TextSecondary) } }
        )
    }

    private fun calendarMonth(offset: Int): Calendar = Calendar.getInstance().apply { add(Calendar.MONTH, offset); set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
    private fun monthCalendar(offset: Int): Calendar = calendarMonth(offset)
    private fun monthTitle(calendar: Calendar): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
    private fun percent(value: Double, total: Double): String = if (total > 0) "${(value / total * 100).toInt()}%" else "0%"
    private fun dayOfMonth(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
    private fun dayKey(timestamp: Long): Int = Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.DAY_OF_MONTH)
    private fun dayKey(month: Calendar, day: Int): Int = day

    private fun calendarDays(month: Calendar): List<Int> {
        val first = month.clone() as Calendar
        first.set(Calendar.DAY_OF_MONTH, 1)
        val leading = first.get(Calendar.DAY_OF_WEEK) - 1
        val max = first.getActualMaximum(Calendar.DAY_OF_MONTH)
        return List(leading) { 0 } + (1..max).toList()
    }

    private fun categoryColor(index: Int): Color = when (index % 6) {
        0 -> Color(0xFF8B5CF6)
        1 -> Color(0xFF4F8CFF)
        2 -> Color(0xFF26C6A8)
        3 -> Color(0xFFFFB454)
        4 -> Color(0xFFFF7180)
        else -> Color(0xFF7C8A9E)
    }
}
