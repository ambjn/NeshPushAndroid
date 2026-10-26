package com.ambjn.neshpushandroid.push

/** Route allowlist shared with nerd-push-web lib/push/routes.ts. Unknown keys are rejected, never mapped. */
enum class Route(val key: String, val title: String) {
    HOME("home", "Home"),
    SCHEDULE("schedule", "Schedule"),
    MESSAGES("messages", "Messages"),
    ACCOUNT("account", "Account"),
    STUDY_PLAN("study_plan", "Study plan");

    companion object {
        fun fromKey(key: String?): Route? = entries.firstOrNull { it.key == key }
    }
}
