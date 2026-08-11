package com.wowstudio26.expensetracker.data

data class FinanceTransaction(
    val id: Long,
    val type: TransactionType,
    val owner: String,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long,
    val createdAt: Long,
    val isRecurring: Boolean
)
