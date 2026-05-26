package com.hllous.plantravel.ui

import com.hllous.plantravel.ui.utils.formatExpiry
import org.junit.Assert.assertEquals
import org.junit.Test

class FormatExpiryTest {

    private val now = System.currentTimeMillis()

    @Test
    fun pastTimestampReturnsVencido() {
        assertEquals("Vencido", formatExpiry(now - 1_000))
    }

    @Test
    fun lessThanOneHourReturnsSub1HourMessage() {
        val thirtyMinutes = now + 30 * 60 * 1_000L
        assertEquals("Vence en menos de 1 hora", formatExpiry(thirtyMinutes))
    }

    @Test
    fun threeHoursReturnsFewHoursMessage() {
        val threeHours = now + 3 * 60 * 60 * 1_000L
        assertEquals("Vence en 3 horas", formatExpiry(threeHours))
    }

    @Test
    fun oneHourReturnsSingularHourMessage() {
        val oneHour = now + 60 * 60 * 1_000L + 60_000L // slightly over 1h
        assertEquals("Vence en 1 hora", formatExpiry(oneHour))
    }

    @Test
    fun lessThanTwoDaysReturnsTomorrowMessage() {
        val tomorrow = now + 30 * 60 * 60 * 1_000L
        assertEquals("Vence mañana", formatExpiry(tomorrow))
    }

    @Test
    fun threeDaysReturnsDaysMessage() {
        // +30s buffer so integer division doesn't truncate to 2 days due to test execution time
        val threeDays = now + 3 * 24 * 60 * 60 * 1_000L + 30_000L
        assertEquals("Vence en 3 días", formatExpiry(threeDays))
    }
}
