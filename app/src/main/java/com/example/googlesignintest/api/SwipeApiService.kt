package com.example.googlesignintest.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

data class SwipeRequest(val userId: Int, val stockId: Int, val swipeType: String)
data class ApiResponse(val message: String)

interface SwipeApiService {
    @POST("swipe")
    suspend fun recordSwipe(@Body swipeData: SwipeRequest): Response<ApiResponse>
}
