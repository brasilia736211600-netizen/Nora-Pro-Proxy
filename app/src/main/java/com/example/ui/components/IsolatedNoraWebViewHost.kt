package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.model.NoraProfile
import com.example.webview.IsolationManager
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

@Composable
fun IsolatedNoraWebViewHost(
    profile: NoraProfile,
    url: String,
    onUrlChanged: (String) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var proxyError by remember { mutableStateOf<String?>(null) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Reconstruct WebView cleanly whenever profile ID changes (Maximum Zero-Leak Isolation)
    DisposableEffect(profile.id) {
        onDispose {
            activeWebView?.let { wv ->
                IsolationManager.hardTeardownWebView(wv)
            }
            activeWebView = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("nora_webview_container"),
            factory = { ctx ->
                // Apply process-wide proxy synchronously for the active profile before creating WebView
                IsolationManager.applyProfileProxy(profile.proxy, executor)

                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    activeWebView = this

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        useWideViewPort = true
                        loadWithOverviewMode = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                        // User Agent configuration
                        val defaultUA = userAgentString
                        val effectiveUA = IsolationManager.resolveUserAgent(profile, defaultUA)
                        userAgentString = effectiveUA
                    }

                    // Multi-profile partition if supported by device WebView
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                        try {
                            val profileStore = androidx.webkit.ProfileStore.getInstance()
                            val webProfile = profileStore.getOrCreateProfile(profile.id)
                            WebViewCompat.setProfile(this, webProfile.name)
                        } catch (_: Exception) {
                            // Fallback to default profile with hard-teardown isolation
                        }
                    }

                    // Inject document-start scripts for UA client hints & Timezone simulation
                    val defaultUA = settings.userAgentString
                    val effectiveUA = IsolationManager.resolveUserAgent(profile, defaultUA)
                    val uaShim = IsolationManager.generateUAShimScript(effectiveUA, profile.userAgent.osFamily, profile.userAgent.browserFamily)
                    val timeShim = IsolationManager.generateTimeShimScript(profile)

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        if (uaShim.isNotBlank()) {
                            WebViewCompat.addDocumentStartJavaScript(this, uaShim, setOf("*"))
                        }
                        if (timeShim.isNotBlank()) {
                            WebViewCompat.addDocumentStartJavaScript(this, timeShim, setOf("*"))
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            proxyError = null
                            url?.let { onUrlChanged(it) }

                            // Inject shims as early evaluation fallback
                            if (uaShim.isNotBlank()) view?.evaluateJavascript(uaShim, null)
                            if (timeShim.isNotBlank()) view?.evaluateJavascript(timeShim, null)
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                            url?.let { onUrlChanged(it) }
                            view?.title?.let { onTitleChanged(it) }
                        }

                        override fun onReceivedHttpAuthRequest(
                            view: WebView?,
                            handler: HttpAuthHandler?,
                            host: String?,
                            realm: String?
                        ) {
                            // Automatically inject credentials if profile proxy authentication is configured
                            if (profile.proxy.enabled && profile.proxy.username.isNotBlank()) {
                                handler?.proceed(profile.proxy.username, profile.proxy.password)
                            } else {
                                super.onReceivedHttpAuthRequest(view, handler, host, realm)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            if (request?.isForMainFrame == true) {
                                isLoading = false
                                if (profile.proxy.enabled) {
                                    // Fail-Closed enforcement: display fail-closed blocking proxy error
                                    proxyError = "Proxy Connection Failed: ${error?.description ?: "Unreachable node"}. Failing closed to prevent IP address leak."
                                }
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadProgress = newProgress / 100f
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let { onTitleChanged(it) }
                        }
                    }

                    loadUrl(url)
                }
            },
            update = { wv ->
                // Ensure correct URL loaded if changed
                if (wv.url != url && !url.startsWith("about:")) {
                    wv.loadUrl(url)
                }
            }
        )

        // Loading Progress Bar
        if (isLoading) {
            LinearProgressIndicator(
                progress = { loadProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Fail-Closed Dedicated Security Error Screen
        proxyError?.let { errText ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
                    .padding(24.dp)
                    .testTag("proxy_fail_closed_screen")
            ) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Proxy Connection Blocked",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errText,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row {
                            OutlinedButton(
                                onClick = {
                                    proxyError = null
                                    activeWebView?.reload()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}
