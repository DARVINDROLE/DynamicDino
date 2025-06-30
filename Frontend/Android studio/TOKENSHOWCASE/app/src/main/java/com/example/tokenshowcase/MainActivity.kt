package com.example.tokenshowcase
import com.example.tokenshowcase.PredictionResponse

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.tokenshowcase.ui.theme.TOKENSHOWCASETheme
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlinx.coroutines.launch
import java.util.*
import androidx.compose.ui.viewinterop.AndroidView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val token = getSharedPreferences("auth", MODE_PRIVATE).getString("jwt_token", null)

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val fitnessState = mutableStateOf<FitnessDataResponse?>(null)
        val sleepState = mutableStateOf<SleepApiResponse?>(null)
        val usageStatsState = mutableStateOf<List<UsageStats>>(emptyList())
        val predictionState = mutableStateOf<PredictionResponse?>(null)
        val errorState = mutableStateOf<String?>(null)

        lifecycleScope.launch {
            try {
                val fitnessResponse = RetrofitClient.api.getFitnessData(token)
                val sleepResponse = RetrofitClient.api.getSleepData(token)

                if (fitnessResponse.isSuccessful && sleepResponse.isSuccessful) {
                    val fitness = fitnessResponse.body()
                    val sleep = sleepResponse.body()

                    fitnessState.value = fitness
                    sleepState.value = sleep

                    val usageStats = getDailyUsageStats(this@MainActivity)
                    usageStatsState.value = usageStats

                    val sleepMinutes = sleep?.sleep_summary?.total_sleep_minutes ?: 0
                    val sleepHours = sleepMinutes / 60.0

                    val steps = fitness?.steps_data?.point?.sumOf {
                        it.value?.firstOrNull()?.intVal ?: 0
                    } ?: 0

                    val totalSocialTime = usageStats.sumOf {
                        (it.totalTimeInForeground / 60000).toInt()
                    }

                    val predictionResponse = PredictApiClient.api.getPrediction(
                        sleepHours = sleepHours,
                        stepsCount = steps,
                        socialTime = totalSocialTime
                    )

                    if (predictionResponse.isSuccessful) {
                        predictionState.value = predictionResponse.body()
                    } else {
                        errorState.value = "Prediction API Error: ${predictionResponse.code()}"
                    }
                } else {
                    errorState.value = "Fitness: ${fitnessResponse.code()}, Sleep: ${sleepResponse.code()}"
                }
            } catch (e: Exception) {
                errorState.value = "Exception: ${e.localizedMessage}"
            }
        }

        setContent {
            TOKENSHOWCASETheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(16.dp)
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    ) {
                        if (errorState.value != null) {
                            Text("Error: ${errorState.value}", color = MaterialTheme.colorScheme.error)
                        } else {
                            FitnessDataSection(fitnessState.value)
                            Spacer(modifier = Modifier.height(24.dp))
                            SleepDataSection(sleepState.value)
                            Spacer(modifier = Modifier.height(24.dp))
                            UsageStatsSection(usageStatsState.value, context = this@MainActivity)
                            Spacer(modifier = Modifier.height(24.dp))

                            predictionState.value?.let {
                                Text("\uD83E\uDE96 Mood: ${it.predicted_emotion}")
                                Text("Confidence: ${"%.2f".format(it.confidence_score * 100)}%")
                                Text("Wellbeing Score: ${it.wellbeing_score}")
                                Text("Sleep Score: ${it.wellbeing_breakdown.sleep}")
                                Text("Activity Score: ${it.wellbeing_breakdown.activity}")
                                Text("Music Mood: ${it.wellbeing_breakdown.music_mood}")
                                Text("Digital Wellness: ${it.wellbeing_breakdown.digital_wellness}")
                                Spacer(modifier = Modifier.height(8.dp))
                                it.recommendations.forEach { rec ->
                                    Text("✅ ${rec.category}: ${rec.text}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getDailyUsageStats(context: Context): List<UsageStats> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)

        return if (stats.isNullOrEmpty()) {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            emptyList()
        } else {
            stats.sortedByDescending { it.totalTimeInForeground }.take(5)
        }
    }
}

@Composable
fun SleepDataSection(data: SleepApiResponse?) {
    if (data == null) {
        Text("Loading sleep data...")
        return
    }

    Text("\uD83D\uDECC Sleep Summary")
    Text("Total Sleep: ${data.sleep_summary.total_sleep_minutes} mins")
    Text("Deep Sleep: ${data.sleep_summary.deep_sleep_minutes} mins")
    Text("Light Sleep: ${data.sleep_summary.light_sleep_minutes} mins")
    Text("Sessions: ${data.sleep_sessions.session.size}")
}

@Composable
fun UsageStatsSection(stats: List<UsageStats>, context: Context) {
    if (stats.isEmpty()) {
        Text("\uD83D\uDCF5 Digital Wellbeing: No data (maybe permission not granted)")
        return
    }

    val appUsage = stats.associate {
        val appName = try {
            val appInfo = context.packageManager.getApplicationInfo(it.packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            it.packageName
        }
        appName to (it.totalTimeInForeground / 60000).toInt()
    }

    Text("\uD83D\uDCF1 Top Apps Today", style = MaterialTheme.typography.titleMedium)
    appUsage.forEach { (name, minutes) ->
        Text("$name: $minutes mins")
    }

    Spacer(modifier = Modifier.height(16.dp))
    AppUsageBarChart(appUsage)
}

@Composable
fun AppUsageBarChart(appUsage: Map<String, Int>) {
    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        factory = { context ->
            val chart = BarChart(context)

            val entries = appUsage.entries.mapIndexed { index, entry ->
                BarEntry(index.toFloat(), entry.value.toFloat())
            }

            val dataSet = BarDataSet(entries, "Usage (mins)")
            val barData = BarData(dataSet)
            chart.data = barData

            chart.xAxis.valueFormatter = IndexAxisValueFormatter(appUsage.keys.toList())
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.axisRight.isEnabled = false
            chart.description.isEnabled = false
            chart.animateY(800)
            chart.invalidate()

            chart
        }
    )
}
@Composable
fun FitnessDataSection(data: FitnessDataResponse?) {
    if (data == null) {
        Text("Loading fitness data...")
        return
    }

    Text("\uD83C\uDFC3 Steps Source: ${data.steps_data.dataSourceId}")
    Text("Steps Points: ${data.steps_data.point.size}")

    Spacer(modifier = Modifier.height(8.dp))

    Text("\u2764\uFE0F Heart Rate Source: ${data.heart_rate_data.dataSourceId}")
    Text("Heart Points: ${data.heart_rate_data.point.size}")

    Spacer(modifier = Modifier.height(8.dp))

    Text("\uD83D\uDD25 Calories Source: ${data.calories_data.dataSourceId}")
    Text("Calories Points: ${data.calories_data.point.size}")
}