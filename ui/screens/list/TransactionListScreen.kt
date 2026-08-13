package com.wowstudio.expensetracker.ui.screens.list

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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wowstudio.expensetracker.domain.model.ExpenseCategory
import com.wowstudio.expensetracker.domain.model.Transaction
import com.wowstudio.expensetracker.ui.components.TransactionItem
import com.wowstudio.expensetracker.ui.theme.*
import com.wowstudio.expensetracker.ui.viewmodel.ExpenseFilter
import com.wowstudio.expensetracker.ui.viewmodel.TransactionListViewModel
import com.wowstudio.expensetracker.utils.toCurrencyString
import java.time.LocalDateTime

@Composable
fun TransactionListScreen(
    onNavigateBack: () -> Unit,
    viewModel: TransactionListViewModel = hiltViewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Analytics")
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                }
            }
            
            Text(
                text = "${transactions.size} transactions · ₹0 · ${LocalDateTime.now().month} ${LocalDateTime.now().year}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Search Bar
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    placeholder = { Text("Search transactions...") },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == ExpenseFilter.ALL,
                onClick = { viewModel.setFilter(ExpenseFilter.ALL) },
                label = { Text("All") }
            )
            FilterChip(
                selected = selectedFilter == ExpenseFilter.ESSENTIALS,
                onClick = { viewModel.setFilter(ExpenseFilter.ESSENTIALS) },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(EssentialsColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Essentials")
                    }
                }
            )
            FilterChip(
                selected = selectedFilter == ExpenseFilter.LIFESTYLE,
                onClick = { viewModel.setFilter(ExpenseFilter.LIFESTYLE) },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(LifestyleColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lifestyle")
                    }
                }
            )
            FilterChip(
                selected = selectedFilter == ExpenseFilter.SAVINGS,
                onClick = { viewModel.setFilter(ExpenseFilter.SAVINGS) },
                label = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SavingsColor))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Savings")
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transaction List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val grouped = transactions.groupBy { it.date.toLocalDate() }
            
            grouped.forEach { (date, items) ->
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (date == LocalDateTime.now().toLocalDate()) "TODAY" 
                                   else if (date == LocalDateTime.now().minusDays(1).toLocalDate()) "YESTERDAY"
                                   else date.toString().uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = items.sumOf { it.amount }.toCurrencyString(),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                items(items) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { }
                    )
                }
            }
        }
    }
}
