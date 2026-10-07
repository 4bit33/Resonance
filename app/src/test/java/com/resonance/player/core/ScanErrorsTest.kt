package com.resonance.player.core

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanReport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanErrorsTest {

    @Test
    fun newErrors_haveMessages() {
        assertTrue(AppError.ScanFailed("boom").userMessage().contains("boom"))
        assertTrue(AppError.ScanFailed(null).userMessage().isNotBlank())
    }

    @Test
    fun report_defaultsKeepOldCallSitesWorking() {
        val report = ScanReport(added = 1, updated = 2, removed = 3)
        assertEquals(0, report.failed)
        assertEquals(0, report.total)
    }
}
