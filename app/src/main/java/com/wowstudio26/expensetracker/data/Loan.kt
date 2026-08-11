package com.wowstudio26.expensetracker.data

data class Loan(
    val id: Long,
    val name: String,
    val totalAmount: Double,
    val remainingAmount: Double,
    val issuedDate: Long,
    val dueDate: Long
)
