package com.wowstudio.expensetracker.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Local V2 store. Cloud sync is intentionally left for the final integration step. */
class FinanceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("finance_v2", Context.MODE_PRIVATE)
    private val transactionKey = "transactions"
    private val loanKey = "loans"
    private val categoryKey = "categories"

    fun transactions(): List<FinanceTransaction> = readTransactions().filter { !it.deleted }.sortedByDescending { it.date }
    fun loans(): List<Loan> = readLoans().filter { !it.deleted }.sortedByDescending { it.nextDueDate }

    fun addTransaction(type: TransactionType, owner: String, amount: Double, category: String, description: String, date: Long): Long {
        val id = newId()
        val all = readTransactions().toMutableList()
        all += FinanceTransaction(id, type, owner, amount, category.trim(), description.trim(), date, System.currentTimeMillis(), false)
        saveTransactions(all)
        return id
    }

    fun updateTransaction(id: Long, type: TransactionType, owner: String, amount: Double, category: String, description: String, date: Long) {
        saveTransactions(readTransactions().map {
            if (it.id == id) it.copy(type = type, owner = owner, amount = amount, category = category.trim(), description = description.trim(), date = date, updatedAt = System.currentTimeMillis(), deleted = false) else it
        })
    }

    fun deleteTransaction(id: Long) = saveTransactions(readTransactions().map { if (it.id == id) it.copy(deleted = true, updatedAt = System.currentTimeMillis()) else it })

    fun addLoan(loan: Loan): Long {
        val id = newId()
        saveLoans(readLoans() + loan.copy(id = id))
        return id
    }

    fun updateLoan(loan: Loan) = saveLoans(readLoans().map { if (it.id == loan.id) loan.copy(updatedAt = System.currentTimeMillis()) else it })
    fun deleteLoan(id: Long) = saveLoans(readLoans().map { if (it.id == id) it.copy(deleted = true, updatedAt = System.currentTimeMillis()) else it })

    fun categories(): List<String> = readCategories()
    fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty() && !readCategories().contains(clean)) prefs.edit().putStringSet(categoryKey, (readCategories() + clean).toSet()).apply()
    }
    fun deleteCategory(name: String) = prefs.edit().putStringSet(categoryKey, readCategories().filterNot { it == name }.toSet()).apply()

    private fun readTransactions(): List<FinanceTransaction> {
        val raw = prefs.getString(transactionKey, null) ?: return seedTransactions().also { saveTransactions(it) }
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            FinanceTransaction(o.getLong("id"), TransactionType.valueOf(o.optString("type", "EXPENSE")), o.optString("owner", "Mine"), o.getDouble("amount"), o.optString("category", "Other"), o.optString("description"), o.getLong("date"), o.optLong("updatedAt", o.getLong("date")), o.optBoolean("deleted"))
        }
    }

    private fun saveTransactions(items: List<FinanceTransaction>) {
        val a = JSONArray(); items.forEach { t -> a.put(JSONObject().apply { put("id", t.id); put("type", t.type.name); put("owner", t.owner); put("amount", t.amount); put("category", t.category); put("description", t.description); put("date", t.date); put("updatedAt", t.updatedAt); put("deleted", t.deleted) }) }
        prefs.edit().putString(transactionKey, a.toString()).apply()
    }

    private fun readLoans(): List<Loan> {
        val raw = prefs.getString(loanKey, null) ?: return emptyList()
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Loan(o.getLong("id"), o.optString("lender"), LoanType.valueOf(o.optString("type", "EMI")), o.optString("product"), o.getDouble("originalAmount"), o.optDouble("monthlyPayment"), o.optInt("tenureMonths"), o.optInt("paidMonths"), o.optLong("startDate"), o.optLong("nextDueDate"), o.optLong("updatedAt"), o.optBoolean("deleted"))
        }
    }

    private fun saveLoans(items: List<Loan>) {
        val a = JSONArray(); items.forEach { l -> a.put(JSONObject().apply { put("id", l.id); put("lender", l.lender); put("type", l.type.name); put("product", l.product); put("originalAmount", l.originalAmount); put("monthlyPayment", l.monthlyPayment); put("tenureMonths", l.tenureMonths); put("paidMonths", l.paidMonths); put("startDate", l.startDate); put("nextDueDate", l.nextDueDate); put("updatedAt", l.updatedAt); put("deleted", l.deleted) }) }
        prefs.edit().putString(loanKey, a.toString()).apply()
    }

    private fun readCategories(): List<String> = prefs.getStringSet(categoryKey, null)?.toList()?.sorted() ?: listOf("Food", "Home", "Shopping", "Travel", "Medical", "Education", "Utilities", "Fuel", "Other")

    private fun seedTransactions(): List<FinanceTransaction> {
        val now = System.currentTimeMillis()
        return listOf(
            FinanceTransaction(1, TransactionType.INCOME, "Mine", 85000.0, "Salary", "Monthly salary", now, now, false),
            FinanceTransaction(2, TransactionType.CONTRIBUTION, "Wife", 20000.0, "Contribution", "Household contribution", now - 86400000, now - 86400000, false),
            FinanceTransaction(3, TransactionType.EXPENSE, "Household", 6250.0, "Groceries", "Monthly groceries", now - 172800000, now - 172800000, false),
            FinanceTransaction(4, TransactionType.EXPENSE, "Household", 3200.0, "Home", "Household shopping", now - 259200000, now - 259200000, false),
            FinanceTransaction(5, TransactionType.EXPENSE, "Household", 2500.0, "Fuel", "Fuel", now - 345600000, now - 345600000, false)
        )
    }

    private fun newId(): Long { var id = System.currentTimeMillis(); val used = (readTransactions().map { it.id } + readLoans().map { it.id }).toHashSet(); while (used.contains(id)) id++; return id }
}

data class FinanceTransaction(val id: Long, val type: TransactionType, val owner: String, val amount: Double, val category: String, val description: String, val date: Long, val updatedAt: Long, val deleted: Boolean) {
    /** Compatibility constructor for the reference UI, which historically used owner=1 for the local user. */
    constructor(id: Long, type: TransactionType, owner: Int, amount: Double, category: String, description: String, date: Long) : this(
        id,
        type,
        if (owner == 1) "Mine" else owner.toString(),
        amount,
        category,
        description,
        date,
        System.currentTimeMillis(),
        false
    )
}
enum class TransactionType { INCOME, CONTRIBUTION, EXPENSE }
enum class LoanType { EMI, LOAN, PAY_LATER }
data class Loan(val id: Long = 0L, val lender: String, val type: LoanType, val product: String, val originalAmount: Double, val monthlyPayment: Double, val tenureMonths: Int, val paidMonths: Int, val startDate: Long, val nextDueDate: Long, val updatedAt: Long = System.currentTimeMillis(), val deleted: Boolean = false) {
    val remainingMonths: Int get() = if (type == LoanType.PAY_LATER) 0 else (tenureMonths - paidMonths).coerceAtLeast(0)
    val remainingAmount: Double get() = if (type == LoanType.PAY_LATER) originalAmount else monthlyPayment * remainingMonths
}
