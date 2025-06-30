package com.example.tokenshowcase

data class FitnessDataResponse(
    val steps_data: FitnessMetricData,
    val heart_rate_data: FitnessMetricData,
    val calories_data: FitnessMetricData
)

data class FitnessMetricData(
    val minStartTimeNs: String,
    val maxEndTimeNs: String,
    val dataSourceId: String,
    val point: List<PointData>
)

data class PointData(
    val startTimeNanos: String? = null,
    val endTimeNanos: String? = null,
    val dataTypeName: String? = null,
    val originDataSourceId: String? = null,
    val value: List<ValueData>? = null
)

data class ValueData(
    val fpVal: Float? = null,
    val intVal: Int? = null
    // If you want to parse mapVal safely, use @SerializedName with JsonObject
    // @SerializedName("mapVal") val mapVal: JsonObject? = null
)
