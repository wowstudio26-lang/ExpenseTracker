package com.wowstudio.expensetracker.ui.screens.add

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowstudio.expensetracker.domain.model.*
import com.wowstudio.expensetracker.ui.theme.*
import com.wowstudio.expensetracker.ui.viewmodel.AddTransactionViewModel
import java.time.LocalDateTime

@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSubcategoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "New Expense",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personal / Business Toggle
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                ToggleButton(
                    selected = !uiState.isBusiness,
                    onClick = { viewModel.toggleBusiness() },
                    icon = Icons.Default.Person,
                    text = "Personal",
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    selected = uiState.isBusiness,
                    onClick = { viewModel.toggleBusiness() },
                    icon = Icons.Default.Business,
                    text = "Business",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Expense / Income Toggle
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                ToggleButton(
                    selected = uiState.transactionType == TransactionType.EXPENSE,
                    onClick = { viewModel.updateTransactionType(TransactionType.EXPENSE) },
                    icon = Icons.Default.TrendingDown,
                    text = "Expense",
                    modifier = Modifier.weight(1f)
                )
                ToggleButton(
                    selected = uiState.transactionType == TransactionType.INCOME,
                    onClick = { viewModel.updateTransactionType(TransactionType.INCOME) },
                    icon = Icons.Default.TrendingUp,
                    text = "Income",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Amount
        Text(
            text = "AMOUNT",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "₹",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = uiState.amount.ifEmpty { "0" },
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Quick Amount Buttons
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(listOf("50", "100", "200", "500", "1000")) { amount ->
                OutlinedButton(
                    onClick = { viewModel.updateAmount((uiState.amount.toIntOrNull() ?: 0) + amount.toInt()) },
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("₹$amount")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Date and Scan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Today")
                }
            }
            OutlinedButton(
                onClick = { },
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Scan")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        OutlinedTextField(
            value = uiState.description,
            onValueChange = viewModel::updateDescription,
            placeholder = { Text("e.g. Swiggy dinner") },
            leadingIcon = { Icon(Icons.Default.Menu, contentDescription = null) },
            trailingIcon = { Icon(Icons.Default.Mic, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Type (Essentials / Lifestyle / Savings)
        Text(
            text = "TYPE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TypeChip(
                label = "Essentials",
                selected = uiState.expenseType == ExpenseCategory.ESSENTIALS,
                color = EssentialsColor,
                onClick = { viewModel.updateExpenseType(ExpenseCategory.ESSENTIALS) },
                modifier = Modifier.weight(1f)
            )
            TypeChip(
                label = "Lifestyle",
                selected = uiState.expenseType == ExpenseCategory.LIFESTYLE,
                color = LifestyleColor,
                onClick = { viewModel.updateExpenseType(ExpenseCategory.LIFESTYLE) },
                modifier = Modifier.weight(1f)
            )
            TypeChip(
                label = "Savings",
                selected = uiState.expenseType == ExpenseCategory.SAVINGS,
                color = SavingsColor,
                onClick = { viewModel.updateExpenseType(ExpenseCategory.SAVINGS) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Category
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CATEGORY",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = { }) {
                Text("+ New")
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        val categories = CategoriesData.allCategories
        categories.chunked(2).forEach { rowCategories ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowCategories.forEach { category ->
                    CategoryChip(
                        category = category,
                        selected = uiState.category == category.id,
                        onClick = { 
                            viewModel.updateCategory(category.id)
                            showSubcategoryDialog = true
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Note
        Text(
            text = "NOTE",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = uiState.note,
            onValueChange = viewModel::updateNote,
            placeholder = { Text("Add a note (optional)") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Payment Method
        Text(
            text = "PAYMENT METHOD",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PaymentMethodChip(
                label = "Cash",
                icon = Icons.Default.Money,
                selected = uiState.paymentMethod == PaymentMethod.CASH,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.CASH) },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodChip(
                label = "UPI",
                icon = Icons.Default.QrCode,
                selected = uiState.paymentMethod == PaymentMethod.UPI,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.UPI) },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodChip(
                label = "Debit",
                icon = Icons.Default.AccountBalance,
                selected = uiState.paymentMethod == PaymentMethod.DEBIT,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.DEBIT) },
                modifier = Modifier.weight(1f)
            )
            PaymentMethodChip(
                label = "Credit",
                icon = Icons.Default.CreditCard,
                selected = uiState.paymentMethod == PaymentMethod.CREDIT,
                onClick = { viewModel.updatePaymentMethod(PaymentMethod.CREDIT) },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recurring Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Repeat, contentDescription = null)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Recurring",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Switch(
                checked = uiState.isRecurring,
                onCheckedChange = { viewModel.toggleRecurring() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Add Button
        Button(
            onClick = {
                viewModel.addTransaction()
                onNavigateBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Expense", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ToggleButton(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(vertical = 12.dp),
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CategoryChip(
    category: com.wowstudio.expensetracker.domain.model.Category,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
