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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.updateAll
import com.wowstudio.expensetracker.data.Expense
import com.wowstudio.expensetracker.widget.ExpenseWidget
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ExpenseApp() }
    }

    private fun refreshWidget() {
        ExpenseWidget().updateAll(this)
    }

    @Composable
    private fun ExpenseApp() {
        val repo = (application as ExpenseTrackerApp).repository
        var expenses by remember { mutableStateOf(repo.getExpenses()) }
        var showAdd by remember { mutableStateOf(false) }
        var tab by remember { mutableIntStateOf(0) }
        val total = expenses.sumOf { it.amount }
        val top = expenses.groupBy { it.category }.mapValues { it.value.sumOf(Expense::amount) }.toList().sortedByDescending { it.second }

        MaterialTheme(colorScheme = darkColorScheme()) {
            Scaffold(
                containerColor = Color(0xFF0B0D12),
                floatingActionButton = { FloatingActionButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, "Add expense") } },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF12151C)) {
                        NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Dashboard") })
                        NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ReceiptLong, null) }, label = { Text("Expenses") })
                    }
                }
            ) { pad ->
                if (tab == 0) Dashboard(pad, total, top, expenses.size)
                else ExpenseList(pad, expenses, onDelete = { id -> repo.delete(id); expenses = repo.getExpenses(); refreshWidget() })
            }
        }
        if (showAdd) {
            AddExpenseDialog(
                onDismiss = { showAdd = false },
                onSave = { amount, category, desc ->
                    repo.add(amount, category, desc)
                    expenses = repo.getExpenses()
                    showAdd = false
                    refreshWidget()
                }
            )
        }
    }

    @Composable
    private fun Dashboard(pad: PaddingValues, total: Double, top: List<Pair<String, Double>>, count: Int) {
        Column(Modifier.fillMaxSize().padding(pad).padding(20.dp)) {
            Text("Expense Tracker", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()), color = Color(0xFF9AA3B2), fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171B24)), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Text("TOTAL SPENT", color = Color(0xFF9AA3B2), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(money(total), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                    Text("$count transactions", color = Color(0xFF9AA3B2), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("Top categories", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            top.forEach { (category, value) ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141820)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(category, color = Color.White, fontWeight = FontWeight.Medium)
                        Text(money(value), color = Color(0xFFE7EBF2), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    @Composable
    private fun ExpenseList(pad: PaddingValues, expenses: List<Expense>, onDelete: (Long) -> Unit) {
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp)) {
            Text("Expenses", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 18.dp))
            LazyColumn { items(expenses, key = { it.id }) { e ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF141820)), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(e.category, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(e.description.ifBlank { "No description" }, color = Color(0xFF9AA3B2), fontSize = 12.sp)
                        }
                        Text(money(e.amount), color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { onDelete(e.id) }) { Icon(Icons.Default.Delete, "Delete", tint = Color(0xFFFF7B7B)) }
                    }
                }
            } }
        }
    }

    @Composable
    private fun AddExpenseDialog(onDismiss: () -> Unit, onSave: (Double, String, String) -> Unit) {
        var amount by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("FOOD") }
        var description by remember { mutableStateOf("") }
        val categories = listOf("FOOD", "RENT", "LOAN", "HOME", "CARE TAKER", "EDUCATION", "SHOPPING", "EB", "TRAVEL", "OTHER")
        AlertDialog(onDismissRequest = onDismiss, title = { Text("Add expense") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true)
                OutlinedTextField(category, { category = it.uppercase() }, label = { Text("Category") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Description") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { categories.take(4).forEach { c -> AssistChip(onClick = { category = c }, label = { Text(c) }) } }
            }
        }, confirmButton = { TextButton(onClick = { amount.toDoubleOrNull()?.takeIf { it > 0 }?.let { onSave(it, category, description) } }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } })
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}
