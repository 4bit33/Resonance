package com.resonance.player.core

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.userMessage
import com.resonance.player.core.media.ScanReport
import com.resonance.player.core.permissions.AudioPermissionStatus
import com.resonance.player.core.permissions.permissionStatusFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PermissionStatusTest {

    @Test
    fun matrix() {
        assertEquals(
            AudioPermissionStatus.Granted,
            permissionStatusFor(granted = true, askedBefore = false, shouldShowRationale = false)
        )
        assertEquals(
            AudioPermissionStatus.NotAsked,
            permissionStatusFor(granted = false, askedBefore = false, shouldShowRationale = false)
        )
        assertEquals(
            AudioPermissionStatus.Denied,
            permissionStatusFor(granted = false, askedBefore = true, shouldShowRationale = true)
        )
        assertEquals(
            AudioPermissionStatus.PermanentlyDenied,
            permissionStatusFor(granted = false, askedBefore = true, shouldShowRationale = false)
        )
    }
}

class ScanErrorsTest {

    @Test
    fun newErrors_haveMessages() {
        assertTrue(AppError.MediaStoreUnavailable("x").userMessage().isNotBlank())
        assertTrue(AppError.MediaStoreUnavailable(null).userMessage().isNotBlank())
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
