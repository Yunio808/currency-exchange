package com.example.exchange

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query


interface ExchangeApiService {
    @GET("latest/{baseCurrency}")
    suspend fun getLatestRates(
        @Path("baseCurrency") baseCurrency: String,
        @Query("apikey") apiKey: String = "982bb107887200c0f503d970"
    ): Response<ExchangeRateResponse>
}
