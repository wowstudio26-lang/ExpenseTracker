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
        require(amount > 0.0) { "Transaction amount must be greater than zero" }
        val id = newId()
        val all = readTransactions().toMutableList()
        all += FinanceTransaction(id, type, owner.trim().ifBlank { "Mine" }, amount, category.trim().ifBlank { "Other" }, description.trim(), date, System.currentTimeMillis(), false)
        saveTransactions(all)
        return id
    }

    fun updateTransaction(id: Long, type: TransactionType, owner: String, amount: Double, category: String, description: String, date: Long) {
        require(id > 0L) { "Transaction id must be positive" }
        require(amount > 0.0) { "Transaction amount must be greater than zero" }
        saveTransactions(readTransactions().map {
            if (it.id == id) it.copy(
                type = type,
                owner = owner.trim().ifBlank { "Mine" },
                amount = amount,
                category = category.trim().ifBlank { "Other" },
                description = description.trim(),
                date = date,
                updatedAt = System.currentTimeMillis(),
                deleted = false
            ) else it
        })
    }

    fun deleteTransaction(id: Long) = saveTransactions(readTransactions().map { if (it.id == id) it.copy(deleted = true, updatedAt = System.currentTimeMillis()) else it })

    fun addLoan(loan: Loan): Long {
        validateLoan(loan)
        val id = newId()
        saveLoans(readLoans() + loan.copy(id = id, paidMonths = loan.paidMonths.coerceIn(0, loan.tenureMonths.coerceAtLeast(0))))
        return id
    }

    fun updateLoan(loan: Loan) {
        validateLoan(loan)
        saveLoans(readLoans().map {
            if (it.id == loan.id) loan.copy(
                paidMonths = loan.paidMonths.coerceIn(0, loan.tenureMonths.coerceAtLeast(0)),
                updatedAt = System.currentTimeMillis()
            ) else it
        })
    }

    fun deleteLoan(id: Long) = saveLoans(readLoans().map { if (it.id == id) it.copy(deleted = true, updatedAt = System.currentTimeMillis()) else it })

    fun categories(): List<String> = readCategories()

    fun addCategory(name: String) {
        val clean = name.trim()
        if (clean.isNotEmpty() && !readCategories().contains(clean)) {
            prefs.edit().putStringSet(categoryKey, (readCategories() + clean).toSet()).apply()
        }
    }

    fun deleteCategory(name: String) = prefs.edit().putStringSet(categoryKey, readCategories().filterNot { it == name }.toSet()).apply()

    private fun validateLoan(loan: Loan) {
        require(loan.lender.isNotBlank()) { "Lender is required" }
        require(loan.originalAmount > 0.0) { "Loan amount must be greater than zero" }
        require(loan.tenureMonths >= 0) { "Loan tenure cannot be negative" }
        require(loan.paidMonths >= 0) { "Paid EMIs cannot be negative" }
        if (loan.type != LoanType.PAY_LATER) {
            require(loan.monthlyPayment > 0.0) { "Monthly EMI must be greater than zero" }
            require(loan.tenureMonths > 0) { "EMI/loan tenure must be greater than zero" }
        }
    }

    private fun readTransactions(): List<FinanceTransaction> {
        val raw = prefs.getString(transactionKey, null) ?: return seedTransactions().also { saveTransactions(it) }
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            FinanceTransaction(
                id = o.optLong("id"),
                type = parseTransactionType(o.optString("type")),
                owner = o.optString("owner", "Mine"),
                amount = o.optDouble("amount", 0.0),
                category = o.optString("category", "Other"),
                description = o.optString("description"),
                date = o.optLong("date", System.currentTimeMillis()),
                updatedAt = o.optLong("updatedAt", o.optLong("date", System.currentTimeMillis())),
                deleted = o.optBoolean("deleted")
            )
        }
    }

    private fun saveTransactions(items: List<FinanceTransaction>) {
        val a = JSONArray()
        items.forEach { t ->
            a.put(JSONObject().apply {
                put("id", t.id)
                put("type", t.type.name)
                put("owner", t.owner)
                put("amount", t.amount)
                put("category", t.category)
                put("description", t.description)
                put("date", t.date)
                put("updatedAt", t.updatedAt)
                put("deleted", t.deleted)
            })
        }
        prefs.edit().putString(transactionKey, a.toString()).apply()
    }

    private fun readLoans(): List<Loan> {
        val raw = prefs.getString(loanKey, null) ?: return emptyList()
        val a = JSONArray(raw)
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            val tenure = o.optInt("tenureMonths")
            val paid = o.optInt("paidMonths").coerceIn(0, tenure.coerceAtLeast(0))
            Loan(
                id = o.optLong("id"),
                lender = o.optString("lender"),
                type = parseLoanType(o.optString("type")),
                product = o.optString("product"),
                originalAmount = o.optDouble("originalAmount", 0.0),
                monthlyPayment = o.optDouble("monthlyPayment", 0.0),
                tenureMonths = tenure.coerceAtLeast(0),
                paidMonths = paid,
                startDate = o.optLong("startDate"),
                nextDueDate = o.optLong("nextDueDate"),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                deleted = o.optBoolean("deleted")
            )
        }
    }

    private fun saveLoans(items: List<Loan>) {
        val a = JSONArray()
        items.forEach { l ->
            a.put(JSONObject().apply {
                put("id", l.id)
                put("lender", l.lender)
                put("type", l.type.name)
                put("product", l.product)
                put("originalAmount", l.originalAmount)
                put("monthlyPayment", l.monthlyPayment)
                put("tenureMonths", l.tenureMonths)
                put("paidMonths", l.paidMonths)
                put("startDate", l.startDate)
                put("nextDueDate", l.nextDueDate)
                put("updatedAt", l.updatedAt)
                put("deleted", l.deleted)
            })
        }
        prefs.edit().putString(loanKey, a.toString()).apply()
    }

    private fun readCategories(): List<String> = prefs.getStringSet(categoryKey, null)?.toList()?.sorted()
        ?: listOf("Food", "Home", "Shopping", "Travel", "Medical", "Education", "Utilities", "Fuel", "Other")

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

    private fun parseTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.EXPENSE)

    private fun parseLoanType(value: String): LoanType =
        runCatching { LoanType.valueOf(value) }.getOrDefault(LoanType.EMI)

    private fun newId(): Long {
        var id = System.currentTimeMillis()
        val used = (readTransactions().map { it.id } + readLoans().map { it.id }).toHashSet()
        while (used.contains(id)) id++
        return id
    }
}

data class FinanceTransaction(
    val id: Long,
    val type: TransactionType,
    val owner: String,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long,
    val updatedAt: Long,
    val deleted: Boolean
)
enum class TransactionType { INCOME, CONTRIBUTION, EXPENSE }
enum class LoanType { EMI, LOAN, PAY_LATER }
data class Loan(
    val id: Long = 0L,
    val lender: String,
    val type: LoanType,
    val product: String,
    val originalAmount: Double,
    val monthlyPayment: Double,
    val tenureMonths: Int,
    val paidMonths: Int,
    val startDate: Long,
    val nextDueDate: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val deleted: Boolean = false
) {
    val remainingMonths: Int get() = if (type == LoanType.PAY_LATER) 0 else (tenureMonths - paidMonths).coerceAtLeast(0)
    val remainingAmount: Double get() = if (type == LoanType.PAY_LATER) originalAmount else monthlyPayment * remainingMonths
}
