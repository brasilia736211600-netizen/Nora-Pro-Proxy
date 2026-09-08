package com.example.model

data class FloatingWidgetSettings(
    val isVisible: Boolean = true,
    val sizeDp: Int = 56, // 44, 56, 68
    val opacityPercent: Int = 90, // 30% to 100%
    val offsetXPercent: Float = 0.85f, // relative X position 0..1
    val offsetYPercent: Float = 0.75f, // relative Y position 0..1
    val showProxyBadge: Boolean = true,
    val showProfileName: Boolean = true
)
