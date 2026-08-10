package com.wowstudio.expensetracker.data

import android.content.Context
import com.wowstudio.expensetracker.sync.FirebaseSyncClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

class ExpenseRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("expense_store", Context.MODE_PRIVATE)
    private val key = "expenses"
    private val sync = FirebaseSyncClient(appContext)
    private val deviceId = prefs.getString("device_id", null) ?: UUID.randomUUID().toString().also {
        prefs.edit().putString("device_id", it).apply()
    }

    fun isSyncConfigured() = sync.isConfigured()
    fun syncDatabaseUrl() = sync.databaseUrl()
    fun syncApiKey() = sync.apiKey()
    fun syncRoom() = sync.room()
    fun configureSync(databaseUrl: String, apiKey: String, room: String) = sync.setConfig(databaseUrl, apiKey, room)

    fun getExpenses(includeDeleted: Boolean = false): List<Expense> {
        val raw = prefs.getString(key, null) ?: return seed().also { save(it, seeded = true) }
        val array = JSONArray(raw)
        return List(array.length()) { i ->
            val o = array.getJSONObject(i)
            Expense(
                id = o.getLong("id"), amount = o.getDouble("amount"), category = o.getString("category"),
                description = o.optString("description"), date = o.getLong("date"),
                updatedAt = o.optLong("updatedAt", o.optLong("date")), deleted = o.optBoolean("deleted", false)
            )
        }.filter { includeDeleted || !it.deleted }.sortedByDescending { it.date }
    }

    fun getExpensesForMonth(year: Int, month: Int): List<Expense> = getExpenses().filter { isInMonth(it.date, year, month) }

    fun add(amount: Double, category: String, description: String, date: Long = System.currentTimeMillis()): Long {
        val id = newId()
        val all = getExpenses(true).toMutableList()
        all += Expense(id, amount, category.trim().uppercase(), description.trim(), date, System.currentTimeMillis(), false)
        save(all, seeded = false)
        return id
    }

    fun update(id: Long, amount: Double, category: String, description: String, date: Long) {
        save(getExpenses(true).map {
            if (it.id == id) it.copy(amount = amount, category = category.trim().uppercase(), description = description.trim(), date = date, updatedAt = System.currentTimeMillis(), deleted = false) else it
        }, seeded = false)
    }

    fun delete(id: Long) {
        save(getExpenses(true).map { if (it.id == id) it.copy(updatedAt = System.currentTimeMillis(), deleted = true) else it }, seeded = false)
    }

    fun clear() = save(getExpenses(true).map { it.copy(updatedAt = System.currentTimeMillis(), deleted = true) }, seeded = false)

    fun total(year: Int? = null, month: Int? = null): Double =
        if (year == null || month == null) getExpenses().sumOf { it.amount } else getExpensesForMonth(year, month).sumOf { it.amount }

    fun topCategories(limit: Int = 4, year: Int? = null, month: Int? = null): List<Pair<String, Double>> {
        val source = if (year == null || month == null) getExpenses() else getExpensesForMonth(year, month)
        return source.groupBy { it.category }.mapValues { (_, values) -> values.sumOf { it.amount } }.toList().sortedByDescending { it.second }.take(limit)
    }

    suspend fun syncNow(): SyncResult = withContext(Dispatchers.IO) {
        if (!sync.isConfigured()) return@withContext SyncResult.NotConfigured
        try {
            val remote = FirebaseSyncClient.normalizeRemote(sync.pull())
            val local = getExpenses(true)
            val seeded = prefs.getBoolean("seeded", false)
            if (seeded && remote.length() > 0) {
                val remoteList = parseArray(remote)
                save(remoteList, seeded = false)
                SyncResult.Synced(remoteList.count { !it.deleted })
            } else {
                val merged = merge(local, parseArray(remote))
                save(merged, seeded = false)
                sync.push(toObject(merged).toString())
                SyncResult.Synced(merged.count { !it.deleted })
            }
        } catch (e: Exception) {
            SyncResult.Error(e.message ?: "Sync failed")
        }
    }

    private fun merge(local: List<Expense>, remote: List<Expense>): List<Expense> =
        (local + remote).groupBy { it.id }.mapValues { (_, values) -> values.maxBy { it.updatedAt } }.values.sortedByDescending { it.date }

    private fun parseArray(array: JSONArray): List<Expense> = List(array.length()) { i ->
        val o = array.getJSONObject(i)
        Expense(o.getLong("id"), o.getDouble("amount"), o.optString("category"), o.optString("description"), o.getLong("date"), o.optLong("updatedAt", o.getLong("date")), o.optBoolean("deleted", false))
    }

    private fun toObject(items: List<Expense>): JSONObject = JSONObject().apply {
        items.forEach { e -> put(e.id.toString(), JSONObject().apply {
            put("id", e.id); put("amount", e.amount); put("category", e.category); put("description", e.description)
            put("date", e.date); put("updatedAt", e.updatedAt); put("deleted", e.deleted); put("deviceId", deviceId)
        }) }
    }

    private fun isInMonth(timestamp: Long, year: Int, month: Int): Boolean {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        return c.get(Calendar.YEAR) == year && c.get(Calendar.MONTH) == month
    }

    private fun save(items: List<Expense>, seeded: Boolean) {
        val array = JSONArray()
        items.forEach { e -> array.put(JSONObject().apply {
            put("id", e.id); put("amount", e.amount); put("category", e.category); put("description", e.description)
            put("date", e.date); put("updatedAt", e.updatedAt); put("deleted", e.deleted)
        }) }
        prefs.edit().putString(key, array.toString()).putBoolean("seeded", seeded).apply()
    }

    private fun newId(): Long {
        var id = System.currentTimeMillis()
        val existing = getExpenses(true).map { it.id }.toHashSet()
        while (existing.contains(id)) id++
        return id
    }

    private fun seed(): List<Expense> {
        val now = System.currentTimeMillis()
        return listOf(
            Expense(1, 18000.0, "CARE TAKER", "Monthly caretaker", now, now), Expense(2, 11000.0, "RENT", "House rent", now - 86400000, now - 86400000),
            Expense(3, 10000.0, "LOAN", "EMI", now - 172800000, now - 172800000), Expense(4, 8500.0, "HOME", "Household", now - 259200000, now - 259200000),
            Expense(5, 1500.0, "EDUCATION", "Education", now - 345600000, now - 345600000), Expense(6, 1350.0, "SHOPPING", "Shopping", now - 432000000, now - 432000000),
            Expense(7, 600.0, "EB", "Electricity", now - 518400000, now - 518400000)
        )
    }

    sealed class SyncResult { data object NotConfigured : SyncResult(); data class Synced(val count: Int) : SyncResult(); data class Error(val message: String) : SyncResult() }
}
