package com.example.calendar.util

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TimeSkewMonitorTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2024-01-01T12:00:00Z"),
        ZoneOffset.UTC
    )

    @Test
    fun `returns unknown when reference is missing`() = runTest {
        val monitor = TimeSkewMonitor(
            referenceTimeProvider = { null },
            tolerance = Duration.ofMinutes(5),
            clock = clock
        )

        val result = monitor.evaluate()

        assertTrue(result is TimeSkewResult.Unknown)
    }

    @Test
    fun `detects skew within tolerance`() = runTest {
        val monitor = TimeSkewMonitor(
            referenceTimeProvider = { clock.instant().minus(Duration.ofMinutes(2)) },
            tolerance = Duration.ofMinutes(5),
            clock = clock
        )

        val result = monitor.evaluate()

        assertTrue(result is TimeSkewResult.WithinTolerance)
        result as TimeSkewResult.WithinTolerance
        assertEquals(Duration.ofMinutes(2), result.difference)
        assertEquals(TimeSkewDirection.DeviceAhead, result.direction)
    }

    @Test
    fun `detects skew exceeding tolerance`() = runTest {
        val monitor = TimeSkewMonitor(
            referenceTimeProvider = { clock.instant().plus(Duration.ofMinutes(10)) },
            tolerance = Duration.ofMinutes(5),
            clock = clock
        )

        val result = monitor.evaluate()

        assertTrue(result is TimeSkewResult.SkewExceeded)
        result as TimeSkewResult.SkewExceeded
        assertEquals(Duration.ofMinutes(10), result.difference)
        assertEquals(TimeSkewDirection.DeviceBehind, result.direction)
    }
}
