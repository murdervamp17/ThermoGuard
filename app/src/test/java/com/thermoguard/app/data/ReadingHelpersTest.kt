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

    @Test
    fun appAndWidgetUseTheSameTemperatureColorThresholds() {
        assertEquals(0xFF4FC3F7.toInt(), temperatureColorArgb(29.9f))
        assertEquals(0xFF26A69A.toInt(), temperatureColorArgb(30f))
        assertEquals(0xFFFFB74D.toInt(), temperatureColorArgb(37f))
        assertEquals(0xFFFF7043.toInt(), temperatureColorArgb(42f))
        assertEquals(0xFFE53935.toInt(), temperatureColorArgb(47f))
        assertEquals(0xFF8B949E.toInt(), temperatureColorArgb(null))
        assertEquals(0xFF8B949E.toInt(), temperatureColorArgb(Float.NaN))
    }

    @Test
    fun widgetThermalAccentMatchesTheAppStatusColors() {
        assertEquals(0xFF8B949E.toInt(), thermalColorArgb(null))
        assertEquals(0xFF26A69A.toInt(), thermalColorArgb(0))
        assertEquals(0xFFFFB74D.toInt(), thermalColorArgb(1))
        assertEquals(0xFFFF7043.toInt(), thermalColorArgb(2))
        assertEquals(0xFFE53935.toInt(), thermalColorArgb(3))
    }

    @Test
    fun thermalZoneDetectionRecognizesVendorCpuZonesWithoutMatchingBatteryZones() {
        assertEquals(true, isCpuThermalZone("cpu-thermal"))
        assertEquals(true, isCpuThermalZone("mtktscpu"))
        assertEquals(true, isCpuThermalZone("AP thermal"))
        assertEquals(false, isCpuThermalZone("battery"))
        assertEquals(false, isCpuThermalZone("gpu"))
    }

    @Test
    fun temperatureReadersNormalizeUnitsAndRejectImplausibleValues() {
        assertEquals(42f, parseBatteryTemperature(420))
        assertEquals(null, parseBatteryTemperature(Int.MIN_VALUE))
        assertEquals(null, parseBatteryTemperature(2_000))
        assertEquals(42f, parseThermalZoneTemperature("42000"))
        assertEquals(42f, parseThermalZoneTemperature("42"))
        assertEquals(null, parseThermalZoneTemperature("not-a-number"))
        assertEquals(null, parseThermalZoneTemperature("420000"))
        assertEquals(true, isPlausibleAmbientTemperature(-10f))
        assertEquals(false, isPlausibleAmbientTemperature(Float.NaN))
        assertEquals(false, isPlausibleAmbientTemperature(126f))
    }
}
