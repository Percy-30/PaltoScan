package com.atpdev.paltoscan.data.remote

import com.atpdev.paltoscan.domain.model.RecognitionResult
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("recognize/")
    suspend fun recognizeDisease(
        @Part image: MultipartBody.Part,
    ): RecognitionResult
}
