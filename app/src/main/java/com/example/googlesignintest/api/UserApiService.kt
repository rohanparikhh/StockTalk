package com.example.googlesignintest.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

data class UserRequest(val email: String, val password: String)
data class CreateUserResponse(val message: String, val userId: Int)
data class InterestsRequest(val interests: List<String>)

interface UserApiService {
    @POST("/")
    suspend fun createUser(@Body userData: UserRequest): Response<CreateUserResponse>

    @POST("/{userId}/interests")
    suspend fun createInterests(@Path("userId") userId: Int, @Body interests: InterestsRequest): Response<ApiResponse>

    @PUT("/{userId}/interests")
    suspend fun updateInterests(@Path("userId") userId: Int, @Body interests: InterestsRequest): Response<ApiResponse>
}
