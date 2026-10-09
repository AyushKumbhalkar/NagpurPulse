package com.nagpurpulse.ui.components

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class CountFormatTest {
    @Test fun smallNumbersAreUnchanged() {
        assertEquals("0", formatCount(0))
        assertEquals("999", formatCount(999))
    }

    @Test fun thousands() {
        assertEquals("1K", formatCount(1_000))
        assertEquals("1.5K", formatCount(1_500))
        assertEquals("12.3K", formatCount(12_345))
        assertEquals("999.9K", formatCount(999_949))
    }

    @Test fun roundingUpNeverShows1000K() {
        assertEquals("1M", formatCount(999_999))
    }

    @Test fun millions() {
        assertEquals("1M", formatCount(1_000_000))
        assertEquals("2.5M", formatCount(2_500_000))
    }

    @Test fun ignoresDeviceLocale() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("de", "DE"))
            assertEquals("1.5K", formatCount(1_500))
        } finally {
            Locale.setDefault(previous)
        }
    }
}
