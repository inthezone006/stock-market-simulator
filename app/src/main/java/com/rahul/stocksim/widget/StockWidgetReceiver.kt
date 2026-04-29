package com.rahul.stocksim.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class StockWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = StockWidget()
}
