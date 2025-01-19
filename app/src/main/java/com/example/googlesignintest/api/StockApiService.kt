package com.example.googlesignintest.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

data class Stock(val id: Int, val ticker: String, val name: String, val sector: String)

interface StockApiService {
    @GET("recommendations")
    suspend fun getRecommendations(@Query("user_id") userId: Int): Response<List<Stock>>

    @GET("search")
    suspend fun searchStocks(@Query("query") query: String): Response<List<Stock>>
}
