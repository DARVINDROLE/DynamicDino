package com.example.tokenshowcase

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface FitnessApi {
    @GET("api/fitness/data")
    suspend fun getFitnessData(
        @Header("Authorization") token: String
    ): Response<FitnessDataResponse>

    @GET("api/fitness/sleep")
    suspend fun getSleepData(
        @Header("Authorization") authHeader: String
    ): Response<SleepApiResponse>

}


