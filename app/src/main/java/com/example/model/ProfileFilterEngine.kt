package com.example.model

data class ProfileFilterCriteria(
    val searchQuery: String = "",
    val siteFilter: String? = null,
    val proxyFilter: Boolean? = null, // true = proxy on, false = proxy off, null = all
    val uaModeFilter: UAMode? = null,
    val timeModeFilter: TimeMode? = null,
    val customUAOnly: Boolean = false,
    val sortOption: ProfileSortOption = ProfileSortOption.LAST_USED,
    val sortAscending: Boolean = false,
    val groupBySite: Boolean = false
)

enum class ProfileSortOption(val label: String) {
    NAME("Name"),
    SITE("Website"),
    LAST_USED("Last Used"),
    RECENTLY_CREATED("Recently Created"),
    MOST_USED("Most Used")
}

object ProfileFilterEngine {

    fun filterAndSort(
        profiles: List<NoraProfile>,
        criteria: ProfileFilterCriteria
    ): List<NoraProfile> {
        var filtered = profiles.asSequence()

        // Search query
        if (criteria.searchQuery.isNotBlank()) {
            val q = criteria.searchQuery.trim().lowercase()
            filtered = filtered.filter { p ->
                p.name.lowercase().contains(q) || p.site.lowercase().contains(q)
            }
        }

        // Site filter
        if (!criteria.siteFilter.isNullOrBlank()) {
            filtered = filtered.filter { it.site.equals(criteria.siteFilter, ignoreCase = true) }
        }

        // Proxy filter
        if (criteria.proxyFilter != null) {
            filtered = filtered.filter { it.proxy.enabled == criteria.proxyFilter }
        }

        // UA mode filter
        if (criteria.uaModeFilter != null) {
            filtered = filtered.filter { it.userAgent.mode == criteria.uaModeFilter }
        }

        // Time mode filter
        if (criteria.timeModeFilter != null) {
            filtered = filtered.filter { it.time.mode == criteria.timeModeFilter }
        }

        // Custom UA only
        if (criteria.customUAOnly) {
            filtered = filtered.filter { it.userAgent.mode == UAMode.CUSTOM || it.userAgent.customUA.isNotBlank() }
        }

        val sorted = when (criteria.sortOption) {
            ProfileSortOption.NAME -> filtered.sortedBy { it.name.lowercase() }
            ProfileSortOption.SITE -> filtered.sortedBy { it.site.lowercase() }
            ProfileSortOption.LAST_USED -> filtered.sortedBy { it.lastUsedAt }
            ProfileSortOption.RECENTLY_CREATED -> filtered.sortedBy { it.createdAt }
            ProfileSortOption.MOST_USED -> filtered.sortedBy { it.usageCount }
        }

        val resultList = sorted.toList()
        return if (criteria.sortAscending) resultList else resultList.reversed()
    }

    fun groupBySite(profiles: List<NoraProfile>): Map<String, List<NoraProfile>> {
        return profiles.groupBy { if (it.site.isNotBlank()) it.site else "General / Other" }
    }
}
