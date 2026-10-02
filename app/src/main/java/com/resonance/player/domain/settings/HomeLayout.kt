package com.resonance.player.domain.settings

/** Blocks the Home screen can show. Order and visibility are the user's. */
enum class HomeSection {
    GREETING,
    SEARCH,
    CONTINUE,
    RECENTLY_PLAYED,
    RECENTLY_ADDED,
    GENRES,
    PLAYLISTS,
    STATS,
    IMPORT,
    QUICK_ACTIONS,
    MOST_PLAYED
}

data class HomeSectionSetting(val section: HomeSection, val enabled: Boolean)

/** Every [HomeSection] exactly once, in display order, each on or off. */
data class HomeLayout(
    val sections: List<HomeSectionSetting>,
    val compact: Boolean
) {
    val visible: List<HomeSection> get() = sections.filter { it.enabled }.map { it.section }

    fun toggled(section: HomeSection): HomeLayout =
        copy(sections = sections.map { if (it.section == section) it.copy(enabled = !it.enabled) else it })

    /** Moves [section] one step up (delta -1) or down (+1); out-of-range moves do nothing. */
    fun moved(section: HomeSection, delta: Int): HomeLayout {
        val from = sections.indexOfFirst { it.section == section }
        val to = from + delta
        if (from < 0 || to !in sections.indices) return this
        val list = sections.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        return copy(sections = list)
    }

    companion object {
        private val defaultOn = listOf(
            HomeSection.GREETING,
            HomeSection.SEARCH,
            HomeSection.CONTINUE,
            HomeSection.RECENTLY_PLAYED,
            HomeSection.RECENTLY_ADDED,
            HomeSection.GENRES,
            HomeSection.PLAYLISTS,
            HomeSection.STATS,
            HomeSection.IMPORT
        )

        val Default = HomeLayout(
            sections = defaultOn.map { HomeSectionSetting(it, true) } +
                HomeSection.entries.filter { it !in defaultOn }.map { HomeSectionSetting(it, false) },
            compact = true
        )
    }
}

/** Stored form: "GREETING:1,SEARCH:0,...;compact=1". */
fun encodeHomeLayout(layout: HomeLayout): String =
    layout.sections.joinToString(",") { "${it.section.name}:${if (it.enabled) 1 else 0}" } +
        ";compact=" + (if (layout.compact) 1 else 0)

/**
 * Tolerant reader: unknown or repeated names are dropped and sections added
 * in later versions are appended switched off, so an old stored value never
 * hides a block for good or crashes the app.
 */
fun decodeHomeLayout(raw: String?): HomeLayout {
    if (raw.isNullOrBlank()) return HomeLayout.Default
    val parts = raw.split(";")
    val seen = LinkedHashMap<HomeSection, Boolean>()
    parts.first().split(",").forEach { entry ->
        val name = entry.substringBefore(":").trim()
        val section = HomeSection.entries.firstOrNull { it.name == name } ?: return@forEach
        if (section !in seen) seen[section] = entry.substringAfter(":", "1").trim() != "0"
    }
    if (seen.isEmpty()) return HomeLayout.Default
    HomeSection.entries.filter { it !in seen }.forEach { seen[it] = false }
    val compact = parts.drop(1).firstOrNull { it.startsWith("compact=") }
        ?.substringAfter("=")?.trim()?.let { it != "0" } ?: HomeLayout.Default.compact
    return HomeLayout(seen.map { (section, on) -> HomeSectionSetting(section, on) }, compact)
}
