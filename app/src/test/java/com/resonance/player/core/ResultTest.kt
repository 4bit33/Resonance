package com.resonance.player.core

import com.resonance.player.core.common.AppError
import com.resonance.player.core.common.Result
import com.resonance.player.core.common.errorOrNull
import com.resonance.player.core.common.fold
import com.resonance.player.core.common.getOrNull
import com.resonance.player.core.common.map
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultTest {

    @Test
    fun success_map_transformsValue() {
        val result: Result<Int> = Result.Success(2)
        assertEquals(Result.Success(4), result.map { it * 2 })
    }

    @Test
    fun failure_map_preservesError() {
        val error = AppError.EmptyLibrary
        val result: Result<Int> = Result.Failure(error)
        assertEquals(Result.Failure(error), result.map { it * 2 })
    }

    @Test
    fun fold_routesBothBranches() {
        assertEquals(4, (Result.Success(2) as Result<Int>).fold({ -1 }, { it * 2 }))
        assertEquals(-1, (Result.Failure(AppError.EmptyLibrary) as Result<Int>).fold({ -1 }, { it * 2 }))
    }

    @Test
    fun accessors_behave() {
        assertEquals(1, (Result.Success(1)).getOrNull())
        assertNull((Result.Failure(AppError.EmptyLibrary) as Result<Int>).getOrNull())
        assertTrue((Result.Failure(AppError.EmptyLibrary) as Result<Int>).errorOrNull() is AppError.EmptyLibrary)
    }
}
