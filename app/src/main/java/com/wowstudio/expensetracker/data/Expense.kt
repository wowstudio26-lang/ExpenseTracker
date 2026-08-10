package com.wowstudio.expensetracker.data

data class Expense(
    val id: Long,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long
)
