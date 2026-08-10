package com.wowstudio.expensetracker.security

import android.content.Context
import androidx.core.content.edit

/** User-selectable app-lock method. PIN/pattern values are never stored here. */
enum class AppLockMethod { NONE, BIOMETRIC, PIN, PATTERN }

class AppLockStore(context: Context) {
    private val prefs = context.getSharedPreferences("app_lock", Context.MODE_PRIVATE)

    var method: AppLockMethod
        get() = runCatching {
            AppLockMethod.valueOf(prefs.getString(KEY_METHOD, AppLockMethod.NONE.name)!!)
        }.getOrDefault(AppLockMethod.NONE)
        set(value) = prefs.edit { putString(KEY_METHOD, value.name) }

    fun isEnabled(): Boolean = method != AppLockMethod.NONE

    fun clear() = prefs.edit { clear() }

    companion object {
        private const val KEY_METHOD = "method"
    }
}
