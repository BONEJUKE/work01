package com.example.calendar.util

import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Detects significant time differences between the device clock and a trusted reference
 * such as the server time or the timestamp of the last successful synchronization.
 */
class TimeSkewMonitor(
    private val referenceTimeProvider: suspend () -> Instant?,
    private val tolerance: Duration = Duration.ofMinutes(5),
    private val clock: Clock = Clock.systemUTC(),
    private val referenceRecorder: suspend (Instant) -> Unit = {}
) {
    /**
     * Evaluates the current device time against the reference. When the absolute
     * difference exceeds the configured [tolerance], a [TimeSkewResult.SkewExceeded]
     * is returned with the direction of the skew.
     */
    suspend fun evaluate(): TimeSkewResult {
        val reference = referenceTimeProvider() ?: return TimeSkewResult.Unknown
        val now = clock.instant()
        val rawDifference = Duration.between(reference, now)
        val direction = if (rawDifference.isNegative) {
            TimeSkewDirection.DeviceBehind
        } else {
            TimeSkewDirection.DeviceAhead
        }
        val difference = rawDifference.absoluteValue()

        return if (difference > tolerance) {
            TimeSkewResult.SkewExceeded(difference, direction)
        } else {
            TimeSkewResult.WithinTolerance(difference, direction)
        }
    }

    private fun Duration.absoluteValue(): Duration = if (isNegative) negated() else this

    /**
     * Records the provided [reference] instant as the latest trusted timestamp. This should
     * be invoked after a successful synchronization with the server or any operation that
     * confirms the device clock is aligned with authoritative time.
     */
    suspend fun recordReference(reference: Instant = clock.instant()) {
        referenceRecorder(reference)
    }
}

sealed class TimeSkewResult {
    data class WithinTolerance(
        val difference: Duration,
        val direction: TimeSkewDirection
    ) : TimeSkewResult()

    data class SkewExceeded(
        val difference: Duration,
        val direction: TimeSkewDirection
    ) : TimeSkewResult()

    data object Unknown : TimeSkewResult()
}

enum class TimeSkewDirection {
    DeviceAhead,
    DeviceBehind
}
