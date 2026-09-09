package com.example.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "https://duckduckgo.com",
    val profileId: String = "",
    val isIncognito: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
