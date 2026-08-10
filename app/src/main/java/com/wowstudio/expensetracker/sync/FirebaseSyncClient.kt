package com.wowstudio.expensetracker.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class FirebaseSyncClient(context: Context) {
    private val prefs = context.getSharedPreferences("expense_sync", Context.MODE_PRIVATE)

    fun isConfigured(): Boolean = databaseUrl().isNotBlank() && apiKey().isNotBlank() && room().isNotBlank()
    fun databaseUrl() = prefs.getString("database_url", "")!!.trim().trimEnd('/')
    fun apiKey() = prefs.getString("api_key", "")!!.trim()
    fun room() = prefs.getString("room", "")!!.trim()

    fun setConfig(databaseUrl: String, apiKey: String, room: String) {
        prefs.edit().putString("database_url", databaseUrl.trim().trimEnd('/')).putString("api_key", apiKey.trim()).putString("room", room.trim()).remove("id_token").apply()
    }

    suspend fun pull(): String? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        authenticatedRequest("GET")
    }

    // PATCH updates only the supplied expense children. Unlike PUT, it cannot wipe a concurrent device's new expense.
    suspend fun push(json: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        authenticatedRequest("PATCH", json) != null
    }

    private fun authenticatedRequest(method: String, body: String? = null): String? {
        return try {
            request(method, authenticatedEndpoint(), body)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("HTTP 401") == true || e.message?.contains("HTTP 403") == true) {
                prefs.edit().remove("id_token").apply()
                request(method, authenticatedEndpoint(), body)
            } else throw e
        }
    }

    private fun authenticatedEndpoint(): String {
        val token = prefs.getString("id_token", null) ?: anonymousSignIn()
        return "${databaseUrl()}/expenseRooms/${room()}/expenses.json?auth=$token"
    }

    private fun anonymousSignIn(): String {
        val response = request("POST", "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey()}", "{\"returnSecureToken\":true}")
            ?: throw IllegalStateException("Anonymous authentication failed")
        val token = JSONObject(response).optString("idToken")
        if (token.isBlank()) throw IllegalStateException("Firebase did not return an auth token")
        prefs.edit().putString("id_token", token).apply()
        return token
    }

    private fun request(method: String, urlString: String, body: String? = null): String? {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10000
            readTimeout = 10000
            setRequestProperty("Content-Type", "application/json")
            doInput = true
            if (body != null) doOutput = true
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }
            if (code in 200..299) text else throw IllegalStateException("Sync server returned HTTP $code")
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        fun normalizeRemote(raw: String?): JSONArray {
            if (raw.isNullOrBlank() || raw == "null") return JSONArray()
            return try {
                val root = JSONObject(raw)
                val array = JSONArray()
                root.keys().forEach { key ->
                    root.optJSONObject(key)?.let { obj ->
                        obj.put("id", key.toLongOrNull() ?: obj.optLong("id"))
                        array.put(obj)
                    }
                }
                array
            } catch (_: Exception) { JSONArray() }
        }
    }
}
