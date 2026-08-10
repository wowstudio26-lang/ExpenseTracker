package com.wowstudio.expensetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.wowstudio.expensetracker.ExpenseTrackerApp
import com.wowstudio.expensetracker.MainActivity
import java.text.NumberFormat
import java.util.Locale

class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as ExpenseTrackerApp).repository
        val total = repo.total()
        val top = repo.topCategories(3)
        provideContent {
            WidgetContent(total, top)
        }
    }

    @Composable
    private fun WidgetContent(total: Double, top: List<Pair<String, Double>>) {
        val rupee = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(total)
        Column(
            modifier = GlanceModifier.fillMaxSize().background(ColorProvider(0xFF151922)).padding(14.dp),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text("MONTHLY EXPENSES", style = TextStyle(color = ColorProvider(0xFFFFFFFF), fontSize = 13.sp))
            Text(rupee, style = TextStyle(color = ColorProvider(0xFFFFFFFF), fontSize = 25.sp))
            Spacer(GlanceModifier.width(1.dp).padding(3.dp))
            top.forEach { (cat, value) ->
                Row(modifier = GlanceModifier.fillMaxSize().padding(vertical = 2.dp)) {
                    Text(cat, style = TextStyle(color = ColorProvider(0xFFB7BFCC), fontSize = 12.sp))
                    Spacer(GlanceModifier.width(8.dp))
                    Text(NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value), style = TextStyle(color = ColorProvider(0xFFFFFFFF), fontSize = 12.sp))
                }
            }
            Spacer(GlanceModifier.width(1.dp).padding(3.dp))
            Text("＋ ADD EXPENSE", modifier = GlanceModifier.padding(top = 6.dp).clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = ColorProvider(0xFFFFFFFF), fontSize = 12.sp))
        }
    }
}
