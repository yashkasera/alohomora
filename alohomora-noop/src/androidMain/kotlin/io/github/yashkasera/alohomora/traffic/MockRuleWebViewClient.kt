package io.github.yashkasera.alohomora.traffic

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * No-op mirror of `:alohomora`'s `MockRuleWebViewClient`.
 *
 * Delegates everything to the wrapped client (or default behavior). Present so consumers
 * can reference the class unconditionally across debug and release variants.
 */
class MockRuleWebViewClient(
    private val delegate: WebViewClient? = null,
) : WebViewClient() {

    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest,
    ): WebResourceResponse? = delegate?.shouldInterceptRequest(view, request)

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        delegate?.shouldOverrideUrlLoading(view, request) ?: super.shouldOverrideUrlLoading(
            view,
            request,
        )

    override fun onPageFinished(view: WebView, url: String?) {
        delegate?.onPageFinished(view, url) ?: super.onPageFinished(view, url)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: android.webkit.WebResourceError,
    ) {
        delegate?.onReceivedError(view, request, error) ?: super.onReceivedError(
            view,
            request,
            error,
        )
    }
}
