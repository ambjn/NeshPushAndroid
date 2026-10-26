package com.ambjn.neshpushandroid

import com.ambjn.neshpushandroid.push.PushMessage
import com.ambjn.neshpushandroid.push.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors nerd-push-web/lib/push/fixtures/transactional-payload.json data map. Keep both in sync. */
class PushMessageTest {
    private val fixture = mapOf(
        "title" to "Your plan is ready", "message" to "Open your study plan.", "event_source" to "NerdPush",
        "event_id" to "11111111-1111-4111-8111-111111111111", "notification_type" to "nerdpush_transactional",
        "route" to "study_plan", "target_user_id" to "firebase-uid-123", "collapse_key" to "update_notification",
    )

    @Test fun parsesTheSharedContract() {
        val p = PushMessage(fixture)
        assertTrue(p.isNerdPush); assertFalse(p.isMarketing)
        assertEquals(Route.STUDY_PLAN, p.route); assertEquals("firebase-uid-123", p.targetUserId); assertEquals("Open your study plan.", p.message)
    }
    @Test fun rejectsForeignSourceAndUnknownRoutes() {
        assertFalse(PushMessage(fixture + ("event_source" to "Other")).isNerdPush)
        assertFalse(PushMessage(fixture + ("route" to "com.evil.Activity")).isNerdPush)
        assertNull(PushMessage(fixture + ("web_url" to "http://nope")).webUrl)
        assertNull(Route.fromKey("https://example.com"))
    }
}
