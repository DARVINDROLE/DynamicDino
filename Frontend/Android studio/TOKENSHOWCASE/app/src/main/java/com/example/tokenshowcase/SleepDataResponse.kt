package com.example.tokenshowcase

data class SleepApiResponse(
    val sleep_sessions: SleepSessions,
    val sleep_summary: SleepSummary
)

data class SleepSessions(
    val session: List<SleepSession>,
    val deletedSession: List<SleepSession>,
    val nextPageToken: String
)

data class SleepSession(
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val startTimeMillis: String? = null,
    val endTimeMillis: String? = null,
    val modifiedTimeMillis: String? = null,
    val application: ApplicationInfo? = null,
    val activityType: Int? = null
)

data class ApplicationInfo(
    val packageName: String? = null
)

data class SleepSummary(
    val total_sleep_minutes: Int,
    val deep_sleep_minutes: Int,
    val light_sleep_minutes: Int
)
