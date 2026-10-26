package com.thermoguard.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingHelpersTest {
    @Test
    fun quietHoursSupportsSchedulesThatCrossMidnight() {
        assertEquals(true, isWithinQuietHours(23 * 60, 22 * 60, 7 * 60))
        assertEquals(true, isWithinQuietHours(6 * 60 + 59, 22 * 60, 7 * 60))
        assertEquals(false, isWithinQuietHours(7 * 60, 22 * 60, 7 * 60))
        assertEquals(false, isWithinQuietHours(12 * 60, 22 * 60, 7 * 60))
    }

    @Test
    fun quietHoursSupportsSchedulesWithinOneDayAndEqualEndpointsDisableWindow() {
        assertEquals(true, isWithinQuietHours(9 * 60, 8 * 60, 10 * 60))
        assertEquals(false, isWithinQuietHours(12 * 60, 8 * 60, 10 * 60))
        assertEquals(false, isWithinQuietHours(9 * 60, 9 * 60, 9 * 60))
        assertEquals(true, isWithinQuietHours(-1, 23 * 60 + 59, 1))
    }

    @Test
    fun memoryFormattingUsesReadableUnits() {
        assertEquals("512 MB", formatMemory(512L * 1024 * 1024))
        assertEquals("1.5 GB", formatMemory(1536L * 1024 * 1024))
    }

    @Test
    fun temperatureDeltaConvertsWithoutAddingAbsoluteTemperatureOffset() {
        assertEquals("9.0°F", formatTempDelta(5f, true))
        assertEquals("5.0°C", formatTempDelta(5f, false))
    }

    @Test
    fun sensorStatusPersistsUsingStableNames() {
        assertEquals(SensorAvailability.UNAVAILABLE, SensorAvailability.fromStorage("UNAVAILABLE"))
        assertEquals(SensorAvailability.READ_FAILED, SensorAvailability.fromStorage("READ_FAILED"))
        assertEquals(SensorAvailability.READ_FAILED, SensorAvailability.fromStorage("invalid"))
    }
}
