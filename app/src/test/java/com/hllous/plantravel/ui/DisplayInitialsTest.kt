package com.hllous.plantravel.ui

import com.hllous.plantravel.ui.utils.displayInitials
import org.junit.Assert.assertEquals
import org.junit.Test

class DisplayInitialsTest {

    @Test
    fun twoWordName_returnsFirstLettersUppercase() {
        assertEquals("AL", displayInitials("Ana López"))
    }

    @Test
    fun singleWordName_returnsSingleLetter() {
        assertEquals("A", displayInitials("Ana"))
    }

    @Test
    fun threeWordName_takesFirstTwoWords() {
        assertEquals("AM", displayInitials("Ana María López"))
    }

    @Test
    fun emptyName_returnsQuestionMark() {
        assertEquals("?", displayInitials(""))
    }

    @Test
    fun blankName_returnsQuestionMark() {
        assertEquals("?", displayInitials("   "))
    }

    @Test
    fun lowercaseName_returnsUppercaseInitials() {
        assertEquals("AL", displayInitials("ana lópez"))
    }
}
