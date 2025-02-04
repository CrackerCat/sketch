package com.github.panpf.sketch.sample.data.api

import com.github.panpf.sketch.sample.model.GiphySearchResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface GiphyService {
    @GET("/v1/gifs/search?type=gifs&sort=&api_key=Gc7131jiJuvI7IdN0HZ1D7nh0ow5BU6g&pingback_id=17c5f87f46b18d99")
    suspend fun search(
        @Query("q") queryWord: String?,
        @Query("offset") pageStart: Int,
        @Query("limit") pageSize: Int
    ): Response<GiphySearchResponse>

    @GET("/v1/gifs/trending?&api_key=Gc7131jiJuvI7IdN0HZ1D7nh0ow5BU6g&pingback_id=17c5f87f46b18d99")
    suspend fun trending(
        @Query("offset") pageStart: Int,
        @Query("limit") pageSize: Int
    ): Response<GiphySearchResponse>
}