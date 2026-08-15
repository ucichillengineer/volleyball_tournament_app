package com.familydays.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegacyCsvParserTest {
    @Test
    fun `extracts birthday with a month name`() {
        val event = LegacyCsvParser.parse("Ravi : DOB: April 22, 1986").single()

        assertEquals("Ravi", event.name)
        assertEquals(4, event.month)
        assertEquals(22, event.day)
        assertEquals(1986, event.year)
        assertEquals(EventType.BIRTHDAY, event.type)
    }

    @Test
    fun `marks missing years for review`() {
        val event = LegacyCsvParser.parse("Padmaja: B: March 1").single()

        assertTrue(event.needsReview)
        assertFalse(event.initial.isNotBlank())
    }
}
