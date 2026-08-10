package com.wowstudio.expensetracker.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class ExpenseWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = ExpenseWidget()
}
