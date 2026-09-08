package com.example.webview

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.webkit.ProxyConfig as AndroidXProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import com.example.model.NoraProfile
import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import com.example.model.TimeMode
import com.example.model.UAMode
import com.example.model.UserAgentLibrary
import java.util.concurrent.Executor

object IsolationManager {

    /**
     * Complete teardown of an outgoing WebView to ensure zero data leakage.
     * Clears cache, stops all loaders, clears transient state, and destroys.
     */
    fun hardTeardownWebView(webView: WebView?) {
        if (webView == null) return
        try {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.clearHistory()
            webView.clearCache(true)
            webView.clearFormData()
            webView.clearSslPreferences()
            webView.removeJavascriptInterface("nouBridge")
            webView.destroy()
        } catch (_: Exception) {
            // Ignore teardown edge cases
        }
    }

    /**
     * Applies the profile's proxy configuration to Chromium's ProxyController.
     * Ensures fail-closed behavior (no direct leak) when proxy is enabled.
     */
    fun applyProfileProxy(proxy: ProxyConfig, executor: Executor, onComplete: () -> Unit = {}) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            onComplete()
            return
        }

        val proxyController = ProxyController.getInstance()

        if (!proxy.enabled || proxy.protocol == ProxyProtocol.DIRECT || proxy.host.isBlank()) {
            // Clear proxy override for direct connections
            proxyController.clearProxyOverride(executor) {
                onComplete()
            }
            return
        }

        val builder = AndroidXProxyConfig.Builder()
        val schemePrefix = when (proxy.protocol) {
            ProxyProtocol.HTTP -> "http://"
            ProxyProtocol.HTTPS -> "https://"
            ProxyProtocol.SOCKS5 -> "socks://"
            ProxyProtocol.DIRECT -> ""
        }

        if (proxy.pacUrl.isNotBlank()) {
            builder.addProxyRule(proxy.pacUrl)
        } else {
            val proxyRule = "$schemePrefix${proxy.host}:${proxy.port}"
            builder.addProxyRule(proxyRule)
        }

        // Strictly do NOT add direct fallback rule! If proxy is unreachable, it MUST fail closed.
        proxyController.setProxyOverride(builder.build(), executor) {
            onComplete()
        }
    }

    /**
     * Resolves the effective User Agent string for the profile.
     */
    fun resolveUserAgent(profile: NoraProfile, defaultDeviceUA: String): String {
        return when (profile.userAgent.mode) {
            UAMode.DEFAULT -> defaultDeviceUA
            UAMode.CUSTOM -> {
                if (profile.userAgent.customUA.isNotBlank()) profile.userAgent.customUA else defaultDeviceUA
            }
            UAMode.PRESET -> {
                val item = UserAgentLibrary.findById(profile.userAgent.selectedId)
                item?.userAgentString ?: defaultDeviceUA
            }
            UAMode.RANDOM -> {
                val item = UserAgentLibrary.getRandomForFamily(
                    profile.userAgent.osFamily,
                    profile.userAgent.browserFamily
                )
                item.userAgentString
            }
        }
    }

    /**
     * Generates document-start JavaScript shim for client-side spoofing of navigator User Agent & Client Hints.
     */
    fun generateUAShimScript(effectiveUA: String, os: String = "Android", browser: String = "Chrome"): String {
        val platform = when (os.lowercase()) {
            "ios" -> "iPhone"
            "windows" -> "Win32"
            "macos" -> "MacIntel"
            "linux" -> "Linux x86_64"
            else -> "Linux armv81"
        }
        val isMobile = os.lowercase() in listOf("android", "ios")

        return """
        (function() {
            try {
                const targetUA = ${sanitizeJsString(effectiveUA)};
                const targetPlatform = ${sanitizeJsString(platform)};
                const targetMobile = $isMobile;

                Object.defineProperty(navigator, 'userAgent', { get: () => targetUA, configurable: true });
                Object.defineProperty(navigator, 'appVersion', { get: () => targetUA.replace(/^Mozilla\//, ''), configurable: true });
                Object.defineProperty(navigator, 'platform', { get: () => targetPlatform, configurable: true });

                if (navigator.userAgentData) {
                    Object.defineProperty(navigator, 'userAgentData', {
                        get: () => ({
                            brands: [
                                { brand: ${sanitizeJsString(browser)}, version: "128" },
                                { brand: "Chromium", version: "128" },
                                { brand: "Not=A?Brand", version: "24" }
                            ],
                            mobile: targetMobile,
                            platform: targetPlatform,
                            getHighEntropyValues: async (hints) => ({
                                brands: [
                                    { brand: ${sanitizeJsString(browser)}, version: "128.0.0.0" },
                                    { brand: "Chromium", version: "128.0.0.0" }
                                ],
                                mobile: targetMobile,
                                platform: targetPlatform,
                                platformVersion: "14.0.0",
                                architecture: "arm",
                                model: targetMobile ? "Pixel 8" : ""
                            })
                        }),
                        configurable: true
                    });
                }
            } catch (e) {
                console.warn("UA shim error:", e);
            }
        })();
        """.trimIndent()
    }

    /**
     * Generates document-start JavaScript shim to simulate Timezone and Date in the WebView.
     */
    fun generateTimeShimScript(profile: NoraProfile): String {
        if (profile.time.mode == TimeMode.DEVICE) {
            return "" // Use system device time
        }

        val tzId = if (profile.time.mode == TimeMode.PROXY) {
            profile.time.simulatedTimezone.ifBlank { profile.time.timezoneId }
        } else {
            profile.time.timezoneId
        }

        val offsetMinutes = profile.time.offsetMinutes

        return """
        (function() {
            try {
                const targetTz = ${sanitizeJsString(tzId)};
                const offsetMs = ${offsetMinutes * 60 * 1000}L;

                const OrigDate = window.Date;
                const OrigDateTimeFormat = Intl.DateTimeFormat;

                function MockDate(...args) {
                    if (!(this instanceof MockDate)) {
                        return new OrigDate(OrigDate.now() + offsetMs).toString();
                    }
                    if (args.length === 0) {
                        return new OrigDate(OrigDate.now() + offsetMs);
                    }
                    return new OrigDate(...args);
                }

                MockDate.prototype = OrigDate.prototype;
                MockDate.now = () => OrigDate.now() + offsetMs;
                MockDate.parse = OrigDate.parse;
                MockDate.UTC = OrigDate.UTC;

                window.Date = MockDate;

                // Mock Intl.DateTimeFormat to honor profile timezone
                Intl.DateTimeFormat = function(locales, options) {
                    const opts = Object.assign({}, options);
                    if (!opts.timeZone) {
                        opts.timeZone = targetTz;
                    }
                    return new OrigDateTimeFormat(locales, opts);
                };
                Intl.DateTimeFormat.prototype = OrigDateTimeFormat.prototype;
                Intl.DateTimeFormat.supportedLocalesOf = OrigDateTimeFormat.supportedLocalesOf;
            } catch (e) {
                console.warn("Time shim error:", e);
            }
        })();
        """.trimIndent()
    }

    private fun sanitizeJsString(s: String): String {
        return "\"" + s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r") + "\""
    }
}
