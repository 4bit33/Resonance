package com.resonance.player.domain.importer

/**
 * Pure rules for importing music with yt-dlp (ADR-013). No Android here, so
 * every rule is unit-tested; the engine only executes what these return.
 */

private val urlPattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)

/**
 * The first http(s) link in [text]. Apps share things like
 * "Listen to X on YouTube Music https://music.youtube.com/watch?v=..." so the
 * link is picked out of whatever surrounds it. Trailing punctuation is dropped.
 */
fun extractImportUrl(text: String?): String? =
    text?.let { urlPattern.find(it)?.value?.trimEnd('.', ',', ')', ']', '!', '?', ';') }
        ?.takeIf { it.length > "https://".length }

/**
 * YouTube "radio"/mix lists (list=RD...) are generated and practically
 * endless: a link to a song played from a mix must import that song only.
 */
fun isGeneratedMix(url: String): Boolean {
    val list = Regex("""[?&]list=([^&#]+)""").find(url)?.groupValues?.get(1) ?: return false
    return list.startsWith("RD") && Regex("""[?&]v=""").containsMatchIn(url)
}

/**
 * yt-dlp arguments (without the URL), as tokens.
 *
 * - Prefer the original AAC stream (m4a): no re-encoding in the common case,
 *   and m4a takes an embedded cover through ffmpeg alone.
 * - Embed tags and the thumbnail cropped to a square (video thumbnails are 16:9).
 * - "Artist - Topic" auto-channels lose the " - Topic" suffix.
 * - Files land as Artist/Album/Title; songs without an album go to "Singles".
 * - The archive file remembers what was already imported, so re-importing a
 *   playlist only fetches what is new.
 */
fun ytDlpArguments(url: String, outputDir: String, archivePath: String): List<String> {
    val args = mutableListOf(
        "-f", "bestaudio[ext=m4a]/bestaudio",
        "-x",
        "--audio-format", "m4a",
        "--embed-metadata",
        "--embed-thumbnail",
        "--convert-thumbnails", "jpg",
        "--ppa", "ThumbnailsConvertor+FFmpeg_o:-c:v mjpeg -qmin 1 -qscale:v 1 " +
            "-vf crop=\"'if(gt(ih,iw),iw,ih)':'if(gt(iw,ih),ih,iw)'\"",
        "--replace-in-metadata", "uploader", " - Topic$", "",
        "--download-archive", archivePath,
        "--windows-filenames",
        "--no-mtime",
        "--newline",
        "-o", "$outputDir/%(artist,uploader|Unknown artist)s/%(album|Singles)s/%(track,title)s.%(ext)s"
    )
    if (isGeneratedMix(url)) args += "--no-playlist"
    return args
}

/** The value after [option] in [args], or null. */
fun List<String>.optionValue(option: String): String? = indexOf(option).takeIf { it >= 0 }?.let { getOrNull(it + 1) }

/** "[download] Downloading item 3 of 20" -> 3 to 20 (playlist position). */
fun parsePlaylistItem(line: String): Pair<Int, Int>? {
    val match = Regex("""Downloading (?:item|video) (\d+) of (\d+)""").find(line) ?: return null
    val (index, total) = match.destructured
    return index.toInt() to total.toInt()
}

/** "[download] Destination: /x/Artist/Album/Song.webm" -> "Song" (what is being fetched right now). */
fun parseCurrentTitle(line: String): String? {
    val path = Regex("""\[download] Destination: (.+)$""").find(line)?.groupValues?.get(1) ?: return null
    return path.substringAfterLast('/').substringBeforeLast('.').takeIf { it.isNotBlank() }
}

/** Audio file extensions the import keeps (everything else in the staging dir is a leftover). */
val importedAudioExtensions = setOf("m4a", "mp3", "opus", "ogg", "flac", "webm", "aac", "wav")
