package com.example.tokenshowcase

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import java.util.*

fun getDailyUsageStats(context: Context): List<UsageStats> {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val endTime = System.currentTimeMillis()
    val startTime = endTime - 1000 * 60 * 60 * 24 // last 24 hours

    val usageStatsList = usageStatsManager.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY, startTime, endTime
    )

    if (usageStatsList.isNullOrEmpty()) {
        // Permission is likely not granted
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        return emptyList()
    }

    return usageStatsList
}
