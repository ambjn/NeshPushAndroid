package com.ambjn.neshpushandroid.push

/** Route allowlist shared with the web creative lint. Anything else falls back to home. */
object Routes {
    const val HOME = "home"
    const val DETAILS = "details"
    private val ALLOWED = setOf(HOME, DETAILS)

    fun sanitize(routeKey: String?): String = if (routeKey in ALLOWED) routeKey!! else HOME
}
