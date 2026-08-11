package com.wowstudio26.expensetracker.data

import android.content.Context

class FinanceRepository(private val context: Context) {

    fun transactions(): List<FinanceTransaction> {
        // TODO: Implement database access or API call
        return emptyList()
    }

    fun loans(): List<Loan> {
        // TODO: Implement database access or API call
        return emptyList()
    }

    fun categories(): List<String> {
        // TODO: Implement database access or API call
        return listOf(
            "Food",
            "Transport",
            "Entertainment",
            "Shopping",
            "Bills",
            "Healthcare",
            "Education"
        )
    }

    fun addTransaction(
        type: TransactionType,
        owner: String,
        amount: Double,
        category: String,
        description: String,
        date: Long
    ) {
        // TODO: Implement database insert
    }

    fun deleteTransaction(id: Long) {
        // TODO: Implement database delete
    }

    fun addCategory(name: String) {
        // TODO: Implement database insert
    }
}
