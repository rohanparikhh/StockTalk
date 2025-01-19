package com.example.googlesignintest.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "http://<YOUR_BACKEND_URL>/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val stockApiService: StockApiService = retrofit.create(StockApiService::class.java)
    val swipeApiService: SwipeApiService = retrofit.create(SwipeApiService::class.java)
    val userApiService: UserApiService = retrofit.create(UserApiService::class.java)
}
