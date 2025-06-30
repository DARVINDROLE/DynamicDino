package com.example.tokenshowcase

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface PredictApi {
    @GET("predict_test")
    suspend fun getPrediction(
        @Query("sleepHours") sleepHours: Double,
        @Query("stepsCount") stepsCount: Int,
        @Query("socialTime") socialTime: Int
    ): Response<PredictionResponse>

}
