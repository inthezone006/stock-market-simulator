package com.rahul.stocksim.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TradingViewChart(
    symbol: String,
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    // TradingView uses "EXCHANGE:SYMBOL" format.
    // We may need to map your symbols (e.g., AAPL -> NASDAQ:AAPL)
    val formattedSymbol = when {
        symbol.contains(":") -> symbol
        symbol.length <= 4 -> "NASDAQ:$symbol" // Most US stocks
        else -> "BINANCE:${symbol}USDT" // Potential crypto fallback
    }

    val theme = if (isDarkMode) "dark" else "light"
    val backgroundColor = if (isDarkMode) "#121212" else "#ffffff"

    // This is the HTML snippet for the TradingView Advanced Real-Time Chart widget
    val html = """
        <!DOCTYPE html>
        <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                <style>
                    body { margin: 0; padding: 0; background-color: $backgroundColor; overflow: hidden; height: 100vh; width: 100vw; }
                    #tradingview_chart { height: 100vh; width: 100vw; }
                </style>
            </head>
            <body>
                <div class="tradingview-widget-container">
                    <div id="tradingview_chart"></div>
                    <script type="text/javascript" src="https://s3.tradingview.com/tv.js"></script>
                    <script type="text/javascript">
                        function initWidget() {
                            new TradingView.widget({
                                "autosize": true,
                                "symbol": "$formattedSymbol",
                                "interval": "D",
                                "timezone": "Etc/UTC",
                                "theme": "$theme",
                                "style": "1",
                                "locale": "en",
                                "toolbar_bg": "$backgroundColor",
                                "enable_publishing": false,
                                "hide_top_toolbar": false,
                                "hide_legend": false,
                                "save_image": false,
                                "container_id": "tradingview_chart",
                                "backgroundColor": "$backgroundColor",
                                "gridColor": "rgba(42, 46, 57, 0.06)",
                                "hide_side_toolbar": true,
                                "allow_symbol_change": false,
                                "details": true,
                                "hotlists": true,
                                "calendar": true,
                                "show_popup_button": true,
                                "popup_width": "1000",
                                "popup_height": "650"
                            });
                        }
                        if (window.TradingView) {
                            initWidget();
                        } else {
                            window.onload = initWidget;
                        }
                    </script>
                </div>
            </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://s3.tradingview.com", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // Re-load if symbol changes might be handled here or by keying the composable
        }
    )
}
