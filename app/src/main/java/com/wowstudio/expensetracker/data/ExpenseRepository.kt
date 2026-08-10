package com.wowstudio.expensetracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class ExpenseRepository(context: Context) {
    private val prefs = context.getSharedPreferences("expense_store", Context.MODE_PRIVATE)
    private val key = "expenses"

    fun getExpenses(): List<Expense> {
        val raw = prefs.getString(key, null) ?: return seed().also { save(it) }
        val array = JSONArray(raw)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Expense(o.getLong("id"), o.getDouble("amount"), o.getString("category"), o.optString("description"), o.getLong("date"))
        }.sortedByDescending { it.date }
    }

    fun add(amount: Double, category: String, description: String) {
        val all = getExpenses().toMutableList()
        all += Expense(System.currentTimeMillis(), amount, category, description, System.currentTimeMillis())
        save(all)
    }

    fun delete(id: Long) = save(getExpenses().filterNot { it.id == id })

    fun clear() = save(emptyList())

    fun total(): Double = getExpenses().sumOf { it.amount }

    fun topCategories(limit: Int = 4): List<Pair<String, Double>> = getExpenses()
        .groupBy { it.category }
        .mapValues { (_, v) -> v.sumOf { it.amount } }
        .toList().sortedByDescending { it.second }.take(limit)

    private fun save(items: List<Expense>) {
        val array = JSONArray()
        items.forEach { e ->
            array.put(JSONObject().apply {
                put("id", e.id); put("amount", e.amount); put("category", e.category)
                put("description", e.description); put("date", e.date)
            })
        }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun seed(): List<Expense> {
        val now = System.currentTimeMillis()
        return listOf(
            Expense(1, 18000.0, "CARE TAKER", "Monthly caretaker", now),
            Expense(2, 11000.0, "RENT", "House rent", now - 86400000),
            Expense(3, 10000.0, "LOAN", "EMI", now - 172800000),
            Expense(4, 8500.0, "HOME", "Household", now - 259200000),
            Expense(5, 1500.0, "EDUCATION", "Education", now - 345600000),
            Expense(6, 1350.0, "SHOPPING", "Shopping", now - 432000000),
            Expense(7, 600.0, "EB", "Electricity", now - 518400000)
        )
    }
}
