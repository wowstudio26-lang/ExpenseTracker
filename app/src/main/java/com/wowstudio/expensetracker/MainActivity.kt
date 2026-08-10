package com.wowstudio.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.wowstudio.expensetracker.data.Expense
import com.wowstudio.expensetracker.widget.ExpenseWidget
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        var monthOffset by remember { mutableIntStateOf(0) }
        var tab by remember { mutableIntStateOf(0) }
        var showEditor by remember { mutableStateOf(false) }
        var editingExpense by remember { mutableStateOf<Expense?>(null) }
        var refreshKey by remember { mutableIntStateOf(0) }

        val selectedMonth = remember(monthOffset, refreshKey) { monthCalendar(monthOffset) }
        val year = selectedMonth.get(Calendar.YEAR)
        val month = selectedMonth.get(Calendar.MONTH)
        val expenses = repo.getExpensesForMonth(year, month)
        val total = expenses.sumOf { it.amount }
        val top = expenses.groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }

        MaterialTheme(colorScheme = darkColorScheme()) {
            Scaffold(
                containerColor = Color(0xFF0B0D12),
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { editingExpense = null; showEditor = true },
                        containerColor = Color(0xFF5E3AA8),
                        contentColor = Color.White
                    ) { Icon(Icons.Default.Add, "Add expense") }
                },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF12151C)) {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Dashboard") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Expenses") })
                    }
                }
            ) { pad ->
                if (tab == 0) {
                    Dashboard(pad, monthOffset, selectedMonth, total, top, expenses.size,
                        onPrevious = { monthOffset--; refreshKey++ },
                        onNext = { if (monthOffset < 0) { monthOffset++; refreshKey++ } })
                } else {
                    ExpenseList(pad, selectedMonth, expenses,
                        onEdit = { editingExpense = it; showEditor = true },
                        onDelete = { id -> repo.delete(id); refreshKey++; refreshWidget() })
                }
            }
        }

        if (showEditor) {
            ExpenseEditorDialog(
                expense = editingExpense,
                defaultDate = selectedMonth.timeInMillis,
                onDismiss = { showEditor = false },
                onSave = { id, amount, category, description, date ->
                    if (id == null) repo.add(amount, category, description, date)
                    else repo.update(id, amount, category, description, date)
                    showEditor = false
                    refreshKey++
                    refreshWidget()
                }
            )
        }
    }

    @Composable
    private fun Dashboard(pad: PaddingValues, monthOffset: Int, month: Calendar, total: Double,
                         top: List<Pair<String, Double>>, count: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(20.dp, 18.dp, 20.dp, 110.dp)) {
            item {
                Text("Expense Tracker", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "Previous month", tint = Color.White) }
                    Text(monthTitle(month), color = Color(0xFF9AA3B2), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    IconButton(onClick = onNext, enabled = monthOffset < 0) { Icon(Icons.Default.ChevronRight, "Next month", tint = if (monthOffset < 0) Color.White else Color(0xFF444955)) }
                }
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171B24)), shape = RoundedCornerShape(26.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp)) {
                        Text("TOTAL SPENT", color = Color(0xFF9AA3B2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(money(total), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                        Text("$count transactions", color = Color(0xFF9AA3B2), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text("Top categories", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(top, key = { it.first }) { (category, value) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141820)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 17.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(money(value), color = Color(0xFFE7EBF2), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    private fun ExpenseList(pad: PaddingValues, month: Calendar, expenses: List<Expense>, onEdit: (Expense) -> Unit, onDelete: (Long) -> Unit) {
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(16.dp, 10.dp, 16.dp, 110.dp)) {
            item {
                Text("Expenses", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(monthTitle(month), color = Color(0xFF9AA3B2), fontSize = 14.sp, modifier = Modifier.padding(top = 3.dp, bottom = 14.dp))
            }
            items(expenses, key = { it.id }) { e ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141820)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.category, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(e.description.ifBlank { "No description" }, color = Color(0xFF9AA3B2), fontSize = 12.sp)
                            Text(formatDate(e.date), color = Color(0xFF687180), fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(money(e.amount), color = Color.White, fontWeight = FontWeight.Bold)
                            Row {
                                IconButton(onClick = { onEdit(e) }) { Icon(Icons.Default.Edit, "Edit", tint = Color(0xFFBDB5FF)) }
                                IconButton(onClick = { onDelete(e.id) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF7B7B)) }
                            }
                        }
                    }
                }
            }
            if (expenses.isEmpty()) item {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) { Text("No expenses for this month", color = Color(0xFF7D8594)) }
            }
        }
    }

    @Composable
    private fun ExpenseEditorDialog(expense: Expense?, defaultDate: Long, onDismiss: () -> Unit,
                                    onSave: (Long?, Double, String, String, Long) -> Unit) {
        var amount by remember(expense) { mutableStateOf(expense?.amount?.toString() ?: "") }
        var category by remember(expense) { mutableStateOf(expense?.category ?: "FOOD") }
        var description by remember(expense) { mutableStateOf(expense?.description ?: "") }
        val date by remember(expense) { mutableLongStateOf(expense?.date ?: defaultDate) }
        val categories = listOf("FOOD", "RENT", "LOAN", "HOME", "CARE TAKER", "EDUCATION", "SHOPPING", "EB", "TRAVEL", "OTHER")
        val validAmount = amount.toDoubleOrNull()?.takeIf { it > 0 } != null
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(if (expense == null) "Add expense" else "Edit expense") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(amount, { amount = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Amount") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(category, { category = it.uppercase() }, label = { Text("Category") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categories) { c -> AssistChip(onClick = { category = c }, label = { Text(c) }) } }
                    OutlinedTextField(description, { description = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("Date: ${formatDate(date)}", color = Color(0xFF7B728E), fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(enabled = validAmount && category.isNotBlank(), onClick = { onSave(expense?.id, amount.toDouble(), category, description, date) }) { Text("SAVE") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
        )
    }

    private fun monthCalendar(offset: Int): Calendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, offset)
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    private fun monthTitle(calendar: Calendar): String = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
    private fun formatDate(timestamp: Long): String = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}
