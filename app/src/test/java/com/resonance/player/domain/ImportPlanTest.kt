package com.resonance.player.domain

import com.resonance.player.domain.importer.extractImportUrl
import com.resonance.player.domain.importer.isGeneratedMix
import com.resonance.player.domain.importer.parseCurrentTitle
import com.resonance.player.domain.importer.parsePlaylistItem
import com.resonance.player.domain.importer.optionValue
import com.resonance.player.domain.importer.ytDlpArguments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportPlanTest {

    @Test
    fun extractsLinkFromSharedText() {
        assertEquals(
            "https://music.youtube.com/watch?v=abc123&si=x",
            extractImportUrl("Слухай «Song» у YouTube Music https://music.youtube.com/watch?v=abc123&si=x")
        )
        assertEquals("https://youtu.be/xyz", extractImportUrl("look: https://youtu.be/xyz."))
        assertNull(extractImportUrl("no link here"))
        assertNull(extractImportUrl(null))
    }

    @Test
    fun generatedMix_onlyWhenSongFromRadioList() {
        assertTrue(isGeneratedMix("https://music.youtube.com/watch?v=abc&list=RDAMVMabc"))
        assertFalse(isGeneratedMix("https://music.youtube.com/playlist?list=RDCLAK5uy_album"))
        assertFalse(isGeneratedMix("https://www.youtube.com/watch?v=abc&list=PLmy"))
        assertFalse(isGeneratedMix("https://youtu.be/abc"))
    }

    @Test
    fun options_addNoPlaylistOnlyForMixes() {
        assertFalse("--no-playlist" in ytDlpArguments("https://youtu.be/abc", "/out", "/a.txt"))
        assertTrue("--no-playlist" in ytDlpArguments("https://www.youtube.com/watch?v=abc&list=RDabc", "/out", "/a.txt"))
    }

    @Test
    fun options_writeIntoOutputDirAndUseArchive() {
        val args = ytDlpArguments("https://youtu.be/abc", "/staging", "/files/archive.txt")
        assertTrue(args.optionValue("-o")!!.startsWith("/staging/"))
        assertEquals("/files/archive.txt", args.optionValue("--download-archive"))
        assertTrue("--embed-thumbnail" in args)
        // three-token option stays intact
        val at = args.indexOf("--replace-in-metadata")
        assertEquals(listOf("uploader", " - Topic$", ""), args.subList(at + 1, at + 4))
    }

    @Test
    fun parsesProgressLines() {
        assertEquals(3 to 20, parsePlaylistItem("[download] Downloading item 3 of 20"))
        assertNull(parsePlaylistItem("[download]  45.3% of 3.21MiB"))
        assertEquals("Afterglow", parseCurrentTitle("[download] Destination: /s/Artist/Singles/Afterglow.m4a"))
        assertNull(parseCurrentTitle("[ExtractAudio] Not converting audio"))
    }
}
