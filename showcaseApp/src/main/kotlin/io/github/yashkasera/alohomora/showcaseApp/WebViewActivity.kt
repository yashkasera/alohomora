package io.github.yashkasera.alohomora.showcaseApp

import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import io.github.yashkasera.alohomora.traffic.MockRuleWebViewClient

class WebViewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = intent.getStringExtra(EXTRA_URL) ?: DEFAULT_URL

        val webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = MockRuleWebViewClient()
            webChromeClient = WebChromeClient()
            loadUrl(url)
        }

        setContentView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        actionBar?.title = "WebView — VPN Throttle Demo"
    }

    companion object {
        const val EXTRA_URL = "url"
        private const val DEFAULT_URL = "https://www.wikipedia.org"
    }
}
