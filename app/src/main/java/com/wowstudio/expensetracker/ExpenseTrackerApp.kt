package com.wowstudio.expensetracker

import android.app.Application
import com.wowstudio.expensetracker.data.ExpenseRepository

class ExpenseTrackerApp : Application() {
    lateinit var repository: ExpenseRepository
        private set
    override fun onCreate() {
        super.onCreate()
        repository = ExpenseRepository(this)
    }
}
