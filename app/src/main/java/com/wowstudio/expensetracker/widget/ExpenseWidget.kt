package com.wowstudio.expensetracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
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
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import com.wowstudio.expensetracker.ExpenseTrackerApp
import com.wowstudio.expensetracker.MainActivity
import java.text.NumberFormat
import java.util.Locale

private val White = ColorProvider(Color.White, Color.White)
private val Muted = ColorProvider(Color(0xFFB7BFCC), Color(0xFFB7BFCC))
private val Surface = ColorProvider(Color(0xFF151922), Color(0xFF151922))

class ExpenseWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = (context.applicationContext as ExpenseTrackerApp).repository
        provideContent { WidgetContent(repo.total(), repo.topCategories(3)) }
    }

    @Composable
    private fun WidgetContent(total: Double, top: List<Pair<String, Double>>) {
        Column(
            modifier = GlanceModifier.fillMaxSize().background(Surface).padding(14.dp),
            verticalAlignment = Alignment.Vertical.Top,
            horizontalAlignment = Alignment.Horizontal.Start
        ) {
            Text("MONTHLY EXPENSES", style = TextStyle(color = White, fontSize = 13.sp))
            Text(money(total), style = TextStyle(color = White, fontSize = 25.sp))
            Spacer(GlanceModifier.width(1.dp).padding(3.dp))
            top.forEach { (cat, value) ->
                Row(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(cat, modifier = GlanceModifier.defaultWeight(), style = TextStyle(color = Muted, fontSize = 12.sp))
                    Text(money(value), style = TextStyle(color = White, fontSize = 12.sp))
                }
            }
            Text("＋ ADD EXPENSE", modifier = GlanceModifier.padding(top = 8.dp).clickable(actionStartActivity<MainActivity>()), style = TextStyle(color = White, fontSize = 12.sp))
        }
    }

    private fun money(value: Double) = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(value)
}
