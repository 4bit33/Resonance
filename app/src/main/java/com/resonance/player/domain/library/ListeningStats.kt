package com.resonance.player.domain.library

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** One play from the history, with what the stats need from its song. */
data class HistoryPlay(
    val durationMs: Long,
    val genreName: String?,
    val artistName: String
)

/** Listening over a period. [listenedMs] counts each play as the whole song (an estimate). */
data class ListeningStats(
    val plays: Int,
    val listenedMs: Long,
    val topGenre: String?,
    val topArtist: String?
)

/** Pure aggregation, unit-tested. Blank or "unknown" names never win the top slots. */
fun listeningStats(plays: List<HistoryPlay>): ListeningStats {
    fun top(names: List<String?>): String? = names
        .mapNotNull { it?.trim()?.takeIf { name -> name.isNotEmpty() && !name.equals("unknown", ignoreCase = true) } }
        .groupingBy { it }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key
    return ListeningStats(
        plays = plays.size,
        listenedMs = plays.sumOf { it.durationMs.coerceAtLeast(0L) },
        topGenre = top(plays.map { it.genreName }),
        topArtist = top(plays.map { it.artistName })
    )
}

/** Stats for the last [days] days. */
class ObserveListeningStatsUseCase(
    private val repository: MusicRepository,
    private val nowSec: () -> Long = { System.currentTimeMillis() / 1000L }
) {
    operator fun invoke(days: Int = 7): Flow<ListeningStats> =
        repository.observeHistorySince(nowSec() - days * 86_400L).map(::listeningStats)
}
