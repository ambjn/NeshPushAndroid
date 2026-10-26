package com.ambjn.neshpushandroid

import com.ambjn.neshpushandroid.push.Routes
import org.junit.Assert.assertEquals
import org.junit.Test

class RoutesTest {
    @Test
    fun unknownRoutesFallBackToHome() {
        assertEquals("home", Routes.sanitize(null))
        assertEquals("home", Routes.sanitize("com.evil.Activity"))
        assertEquals("home", Routes.sanitize("https://example.com"))
        assertEquals("details", Routes.sanitize("details"))
    }
}
