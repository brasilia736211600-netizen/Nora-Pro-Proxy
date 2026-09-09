package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.model.MediaType
import com.example.model.NoraProfile
import com.example.model.SniffedMedia
import com.example.util.AdBlockerEngine
import com.example.util.MediaSnifferHelper
import com.example.util.PageScriptsHelper
import com.example.util.UrlHelper
import com.example.webview.IsolationManager
import org.json.JSONArray
import java.io.ByteArrayInputStream
import java.util.concurrent.Executors

class NoraWebViewController {
    var webView: WebView? = null
    var reload: () -> Unit = {}
    var stopLoading: () -> Unit = {}
    var loadUrl: (String) -> Unit = {}
    var goBack: () -> Unit = {}
    var goForward: () -> Unit = {}
    var canGoBack: Boolean by mutableStateOf(false)
    var canGoForward: Boolean by mutableStateOf(false)
    var isDesktopMode: Boolean by mutableStateOf(false)
    var isSecureConnection: Boolean by mutableStateOf(false)
    var currentTitle: String by mutableStateOf("")
    var currentUrl: String by mutableStateOf("")
    var isLoading: Boolean by mutableStateOf(false)
    var progress: Float by mutableFloatStateOf(0f)

    // XBrowser inspired capabilities
    var sniffedMedia: List<SniffedMedia> by mutableStateOf(emptyList())
    var isDarkModeInjected: Boolean by mutableStateOf(false)
    var isReaderModeInjected: Boolean by mutableStateOf(false)
    var textZoom: Int by mutableIntStateOf(100)

    fun addSniffedMedia(media: SniffedMedia) {
        if (sniffedMedia.none { it.url == media.url }) {
            sniffedMedia = sniffedMedia + media
        }
    }

    fun clearSniffedMedia() {
        sniffedMedia = emptyList()
    }

    fun toggleDarkMode() {
        webView?.evaluateJavascript(PageScriptsHelper.DARK_MODE_SCRIPT) { res ->
            isDarkModeInjected = res?.contains("enabled") == true
        }
    }

    fun toggleReaderMode() {
        webView?.evaluateJavascript(PageScriptsHelper.READER_MODE_SCRIPT) { res ->
            isReaderModeInjected = res?.contains("enabled") == true
        }
    }

    fun unlockCopyRestrictions() {
        webView?.evaluateJavascript(PageScriptsHelper.UNLOCK_COPY_SCRIPT, null)
    }

    fun setTextZoomLevel(percent: Int) {
        textZoom = percent
        webView?.settings?.textZoom = percent
    }

    fun getPageSource(onResult: (String) -> Unit) {
        webView?.evaluateJavascript(PageScriptsHelper.GET_HTML_SOURCE_SCRIPT) { raw ->
            val unescaped = raw?.removeSurrounding("\"")
                ?.replace("\\u003C", "<")
                ?.replace("\\u003E", ">")
                ?.replace("\\\"", "\"")
                ?.replace("\\n", "\n")
                ?.replace("\\t", "\t")
                ?: ""
            onResult(unescaped)
        }
    }
}

@Composable
fun IsolatedNoraWebViewHost(
    profile: NoraProfile,
    url: String,
    controller: NoraWebViewController = remember { NoraWebViewController() },
    adBlockEnabled: Boolean = true,
    onUrlChanged: (String) -> Unit = {},
    onTitleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var proxyError by remember { mutableStateOf<String?>(null) }
    var activeWebView by remember { mutableStateOf<WebView?>(null) }
    val executor = remember { Executors.newSingleThreadExecutor() }

    // Desktop UA template
    val desktopUaFallback = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"

    // When the active profile changes, automatically apply its proxy, user agent, and RELOAD the page
    var previousProfileId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(profile.id, profile.proxy, profile.userAgent) {
        if (previousProfileId != null && previousProfileId != profile.id) {
            // Profile switched! Reapply proxy and trigger reload automatically
            IsolationManager.applyProfileProxy(profile.proxy, executor) {
                activeWebView?.let { wv ->
                    val defaultUA = wv.settings.userAgentString
                    val effectiveUA = if (controller.isDesktopMode || profile.privacy.forceDesktopMode) {
                        desktopUaFallback
                    } else {
                        IsolationManager.resolveUserAgent(profile, defaultUA)
                    }
                    wv.settings.userAgentString = effectiveUA

                    // Check if profile has custom startup URL or reload current page
                    val targetUrl = if (profile.privacy.startupUrl.isNotBlank()) profile.privacy.startupUrl else wv.url ?: url
                    wv.post {
                        wv.loadUrl(targetUrl)
                    }
                }
            }
        }
        previousProfileId = profile.id
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("isolated_webview"),
            factory = { ctx ->
                // Apply proxy configuration on creation
                IsolationManager.applyProfileProxy(profile.proxy, executor)

                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    activeWebView = this
                    controller.webView = this
                    controller.reload = { reload() }
                    controller.stopLoading = { stopLoading() }
                    controller.loadUrl = { newUrl -> loadUrl(newUrl) }
                    controller.goBack = { if (canGoBack()) goBack() }
                    controller.goForward = { if (canGoForward()) goForward() }

                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        allowFileAccess = false
                        allowContentAccess = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        cacheMode = WebSettings.LOAD_DEFAULT
                        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        textZoom = controller.textZoom
                    }

                    // Native Download Manager Integration
                    setDownloadListener { downloadUrl, userAgent, contentDisposition, mimetype, contentLength ->
                        val media = SniffedMedia(
                            url = downloadUrl,
                            title = controller.currentTitle.ifBlank { "download" },
                            mimeType = mimetype ?: "application/octet-stream"
                        )
                        MediaSnifferHelper.downloadMedia(ctx, media)
                    }

                    // Cookie partition setup
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, false)

                    // DNT (Do Not Track) header support
                    if (profile.privacy.doNotTrack) {
                        settings.userAgentString = settings.userAgentString + " DNT/1"
                    }

                    // Multi-profile partition if supported by device WebView
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
                        try {
                            val profileStore = androidx.webkit.ProfileStore.getInstance()
                            val webProfile = profileStore.getOrCreateProfile(profile.id)
                            WebViewCompat.setProfile(this, webProfile.name)
                        } catch (_: Exception) {}
                    }

                    // Inject document-start scripts for UA client hints, Timezone simulation, and WebRTC leak shield
                    val defaultUA = settings.userAgentString
                    val effectiveUA = IsolationManager.resolveUserAgent(profile, defaultUA)
                    val uaShim = IsolationManager.generateUAShimScript(effectiveUA, profile.userAgent.osFamily, profile.userAgent.browserFamily)
                    val timeShim = IsolationManager.generateTimeShimScript(profile)

                    // WebRTC IP leak blocker script
                    val webrtcShim = if (profile.privacy.webrtcProtection) {
                        """
                        (function() {
                            try {
                                if (window.RTCPeerConnection) {
                                    window.RTCPeerConnection = undefined;
                                }
                                if (window.webkitRTCPeerConnection) {
                                    window.webkitRTCPeerConnection = undefined;
                                }
                                if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
                                    navigator.mediaDevices.getUserMedia = function() {
                                        return Promise.reject(new DOMException("WebRTC blocked for privacy", "NotAllowedError"));
                                    };
                                }
                            } catch(e) {}
                        })();
                        """.trimIndent()
                    } else ""

                    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                        if (uaShim.isNotBlank()) {
                            WebViewCompat.addDocumentStartJavaScript(this, uaShim, setOf("*"))
                        }
                        if (timeShim.isNotBlank()) {
                            WebViewCompat.addDocumentStartJavaScript(this, timeShim, setOf("*"))
                        }
                        if (webrtcShim.isNotBlank()) {
                            WebViewCompat.addDocumentStartJavaScript(this, webrtcShim, setOf("*"))
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, pageUrl, favicon)
                            controller.isLoading = true
                            proxyError = null
                            controller.clearSniffedMedia()
                            pageUrl?.let {
                                controller.currentUrl = it
                                controller.isSecureConnection = UrlHelper.isHttps(it)
                                onUrlChanged(it)
                            }
                            controller.canGoBack = canGoBack()
                            controller.canGoForward = canGoForward()

                            // Fallback JavaScript evaluation
                            if (uaShim.isNotBlank()) view?.evaluateJavascript(uaShim, null)
                            if (timeShim.isNotBlank()) view?.evaluateJavascript(timeShim, null)
                            if (webrtcShim.isNotBlank()) view?.evaluateJavascript(webrtcShim, null)
                        }

                        override fun onPageFinished(view: WebView?, pageUrl: String?) {
                            super.onPageFinished(view, pageUrl)
                            controller.isLoading = false
                            pageUrl?.let {
                                controller.currentUrl = it
                                controller.isSecureConnection = UrlHelper.isHttps(it)
                                onUrlChanged(it)
                            }
                            view?.title?.let {
                                controller.currentTitle = it
                                onTitleChanged(it)
                            }
                            controller.canGoBack = canGoBack()
                            controller.canGoForward = canGoForward()

                            // Re-apply Injected Dark Mode if toggled
                            if (controller.isDarkModeInjected) {
                                view?.evaluateJavascript(PageScriptsHelper.DARK_MODE_SCRIPT, null)
                            }

                            // Media Sniffer DOM Evaluation
                            view?.evaluateJavascript(MediaSnifferHelper.DOM_SNIFF_SCRIPT) { jsonArrayStr ->
                                try {
                                    if (!jsonArrayStr.isNullOrBlank() && jsonArrayStr != "null" && jsonArrayStr != "[]") {
                                        val cleaned = jsonArrayStr.removeSurrounding("\"").replace("\\\"", "\"")
                                        val arr = JSONArray(cleaned)
                                        for (i in 0 until arr.length()) {
                                            val mUrl = arr.getString(i)
                                            if (mUrl.startsWith("http")) {
                                                val (mType, mMime) = MediaSnifferHelper.parseMediaType(mUrl)
                                                controller.addSniffedMedia(
                                                    SniffedMedia(
                                                        url = mUrl,
                                                        title = controller.currentTitle,
                                                        mediaType = mType,
                                                        mimeType = mMime
                                                    )
                                                )
                                            }
                                        }
                                    }
                                } catch (_: Exception) {}
                            }
                        }

                        override fun shouldInterceptRequest(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): WebResourceResponse? {
                            val reqUrl = request?.url?.toString() ?: ""

                            // 1. Ad & Tracker Blocking
                            if (adBlockEnabled && reqUrl.isNotBlank() && AdBlockerEngine.isAdOrTracker(reqUrl)) {
                                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
                            }

                            // 2. Video & Media Sniffing
                            if (MediaSnifferHelper.isMediaUrl(reqUrl)) {
                                val (mType, mMime) = MediaSnifferHelper.parseMediaType(reqUrl)
                                val sniffed = SniffedMedia(
                                    url = reqUrl,
                                    title = controller.currentTitle,
                                    mediaType = mType,
                                    mimeType = mMime
                                )
                                view?.post { controller.addSniffedMedia(sniffed) }
                            }

                            return super.shouldInterceptRequest(view, request)
                        }

                        override fun onReceivedHttpAuthRequest(
                            view: WebView?,
                            handler: HttpAuthHandler?,
                            host: String?,
                            realm: String?
                        ) {
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
                                controller.isLoading = false
                                if (profile.proxy.enabled) {
                                    proxyError = "فشل الاتصال بالبروكسي: ${error?.description ?: "العقدة غير متاحة"}. تم قطع الاتصال لحماية خصوصيتك ومنع تسريب الـ IP."
                                }
                            }
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            controller.progress = newProgress / 100f
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let {
                                controller.currentTitle = it
                                onTitleChanged(it)
                            }
                        }
                    }

                    val initialUrl = if (profile.privacy.startupUrl.isNotBlank() && url == "https://duckduckgo.com") {
                        profile.privacy.startupUrl
                    } else {
                        url
                    }
                    loadUrl(initialUrl)
                }
            },
            update = { wv ->
                // Update controller reference
                controller.webView = wv
                controller.canGoBack = wv.canGoBack()
                controller.canGoForward = wv.canGoForward()

                // Desktop mode dynamic switch
                val defaultUA = wv.settings.userAgentString
                val targetUA = if (controller.isDesktopMode || profile.privacy.forceDesktopMode) {
                    desktopUaFallback
                } else {
                    IsolationManager.resolveUserAgent(profile, defaultUA)
                }
                if (wv.settings.userAgentString != targetUA) {
                    wv.settings.userAgentString = targetUA
                    wv.reload()
                }

                // If explicit URL navigation requested
                if (wv.url != url && !url.startsWith("about:") && !controller.isLoading) {
                    wv.loadUrl(url)
                }
            }
        )

        // Loading Progress Bar with smooth gradient
        AnimatedVisibility(
            visible = controller.isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { controller.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f)),
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
                            text = "انقطاع آمن (Fail-Closed Active)",
                            style = MaterialTheme.typography.titleMedium,
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
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    proxyError = null
                                    activeWebView?.reload()
                                }
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إعادة المحاولة")
                            }
                        }
                    }
                }
            }
        }
    }
}
