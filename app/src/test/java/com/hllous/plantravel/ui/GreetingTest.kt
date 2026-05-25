package com.hllous.plantravel.ui

import com.hllous.plantravel.ui.utils.greetingForHour
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingTest {

    @Test
    fun hour5_returnsGoodNight() {
        assertEquals("Buenas noches", greetingForHour(5))
    }

    @Test
    fun hour6_returnsGoodMorning() {
        assertEquals("Buenos días", greetingForHour(6))
    }

    @Test
    fun hour11_returnsGoodMorning() {
        assertEquals("Buenos días", greetingForHour(11))
    }

    @Test
    fun hour12_returnsGoodAfternoon() {
        assertEquals("Buenas tardes", greetingForHour(12))
    }

    @Test
    fun hour19_returnsGoodAfternoon() {
        assertEquals("Buenas tardes", greetingForHour(19))
    }

    @Test
    fun hour20_returnsGoodNight() {
        assertEquals("Buenas noches", greetingForHour(20))
    }

    @Test
    fun hour23_returnsGoodNight() {
        assertEquals("Buenas noches", greetingForHour(23))
    }
}
