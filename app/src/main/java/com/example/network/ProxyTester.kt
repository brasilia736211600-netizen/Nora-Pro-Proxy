package com.example.network

import com.example.model.ProxyConfig
import com.example.model.ProxyProtocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

sealed class ProxyTestResult {
    data class Success(val latencyMs: Long, val responseCode: Int, val serverDate: String?) : ProxyTestResult()
    data class Failure(val errorMessage: String, val latencyMs: Long = 0) : ProxyTestResult()
}

object ProxyTester {

    suspend fun checkProxy(config: ProxyConfig, targetUrl: String = "https://www.google.com/generate_204"): ProxyTestResult = withContext(Dispatchers.IO) {
        if (!config.enabled) {
            return@withContext ProxyTestResult.Failure("Proxy is not enabled")
        }
        if (!config.isValid()) {
            return@withContext ProxyTestResult.Failure("Invalid proxy configuration (check host and port)")
        }

        val startTime = System.currentTimeMillis()
        try {
            val clientBuilder = OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)

            val javaProxy = when (config.protocol) {
                ProxyProtocol.HTTP, ProxyProtocol.HTTPS -> {
                    Proxy(Proxy.Type.HTTP, InetSocketAddress(config.host, config.port))
                }
                ProxyProtocol.SOCKS5 -> {
                    Proxy(Proxy.Type.SOCKS, InetSocketAddress(config.host, config.port))
                }
                ProxyProtocol.DIRECT -> {
                    Proxy.NO_PROXY
                }
            }
            clientBuilder.proxy(javaProxy)

            // Handle Proxy Authentication if provided
            if (config.username.isNotBlank()) {
                val credential = Credentials.basic(config.username, config.password)
                clientBuilder.proxyAuthenticator(object : Authenticator {
                    override fun authenticate(route: Route?, response: Response): Request? {
                        if (response.request.header("Proxy-Authorization") != null) {
                            return null // Give up if already attempted
                        }
                        return response.request.newBuilder()
                            .header("Proxy-Authorization", credential)
                            .build()
                    }
                })
            }

            val client = clientBuilder.build()
            val request = Request.Builder()
                .url(targetUrl)
                .head()
                .build()

            val response = client.newCall(request).execute()
            val latency = System.currentTimeMillis() - startTime
            val serverDate = response.header("Date")
            val code = response.code

            if (response.isSuccessful || code in 200..399 || code == 204) {
                ProxyTestResult.Success(latencyMs = latency, responseCode = code, serverDate = serverDate)
            } else {
                ProxyTestResult.Failure("Proxy returned HTTP $code", latencyMs = latency)
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            ProxyTestResult.Failure(e.localizedMessage ?: "Connection timed out or failed", latencyMs = latency)
        }
    }
}
