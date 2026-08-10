package io.github.yashkasera.alohomora.traffic

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import io.github.yashkasera.alohomora.devtools.NetworkRuleEngine
import java.io.ByteArrayInputStream

/**
 * WebViewClient that applies mock rules to WebView requests.
 *
 * Wraps an optional delegate so existing WebViewClient logic is preserved:
 *
 * ```kotlin
 * webView.webViewClient = MockRuleWebViewClient()
 * // or, wrapping an existing client:
 * webView.webViewClient = MockRuleWebViewClient(myExistingClient)
 * ```
 *
 * Only intercepts requests matching an enabled mock rule. All other requests
 * (and all non-interceptable callbacks) pass through to the delegate.
 */
class MockRuleWebViewClient(
    private val delegate: WebViewClient? = null,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? {
        val url = request.url?.toString() ?: return delegate?.shouldInterceptRequest(view, request)
        val method = request.method

        val mockRule = NetworkRuleEngine.findMatch(url, method)
            ?: return delegate?.shouldInterceptRequest(view, request)

        val mimeType = mockRule.contentType.substringBefore(";").trim()
        val encoding = "UTF-8"

        return WebResourceResponse(
            mimeType,
            encoding,
            mockRule.statusCode,
            "Mocked by Alohomora",
            mapOf("X-Alohomora-Mock-Id" to mockRule.id),
            ByteArrayInputStream(mockRule.responseBody.toByteArray(Charsets.UTF_8)),
        )
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        delegate?.shouldOverrideUrlLoading(view, request) ?: super.shouldOverrideUrlLoading(view, request)

    override fun onPageFinished(view: WebView, url: String?) {
        delegate?.onPageFinished(view, url) ?: super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError,
    ) {
        delegate?.onReceivedError(view, request, error) ?: super.onReceivedError(view, request, error)
    }
}
