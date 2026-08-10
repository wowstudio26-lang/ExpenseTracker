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
            Expense(
                id = o.getLong("id"),
                amount = o.getDouble("amount"),
                category = o.getString("category"),
                description = o.optString("description"),
                date = o.getLong("date")
            )
        }.sortedByDescending { it.date }
    }

    fun getExpensesForMonth(year: Int, month: Int): List<Expense> =
        getExpenses().filter { isInMonth(it.date, year, month) }

    fun add(amount: Double, category: String, description: String, date: Long = System.currentTimeMillis()): Long {
        val id = System.currentTimeMillis()
        val all = getExpenses().toMutableList()
        all += Expense(id, amount, category.trim().uppercase(), description.trim(), date)
        save(all)
        return id
    }

    fun update(id: Long, amount: Double, category: String, description: String, date: Long) {
        val updated = getExpenses().map {
            if (it.id == id) it.copy(
                amount = amount,
                category = category.trim().uppercase(),
                description = description.trim(),
                date = date
            ) else it
        }
        save(updated)
    }

    fun delete(id: Long) = save(getExpenses().filterNot { it.id == id })

    fun clear() = save(emptyList())

    fun total(year: Int? = null, month: Int? = null): Double =
        if (year == null || month == null) getExpenses().sumOf { it.amount }
        else getExpensesForMonth(year, month).sumOf { it.amount }

    fun topCategories(limit: Int = 4, year: Int? = null, month: Int? = null): List<Pair<String, Double>> {
        val source = if (year == null || month == null) getExpenses() else getExpensesForMonth(year, month)
        return source.groupBy { it.category }
            .mapValues { (_, values) -> values.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
    }

    private fun isInMonth(timestamp: Long, year: Int, month: Int): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }

    private fun save(items: List<Expense>) {
        val array = JSONArray()
        items.forEach { e ->
            array.put(JSONObject().apply {
                put("id", e.id)
                put("amount", e.amount)
                put("category", e.category)
                put("description", e.description)
                put("date", e.date)
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
