package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.DobongEventDetailDto
import kr.ac.duksung.dobongzip.data.models.DobongEventDto
import kr.ac.duksung.dobongzip.data.models.DobongEventImageDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NoticeApi {

    @GET("/api/v1/mainpage/dobong/list")
    suspend fun getDobongEvents(
        @Query("date") date: String? = null
    ): ApiResponse<List<DobongEventDto>>

    @GET("/api/v1/mainpage/dobong/{id}")
    suspend fun getDobongEventDetail(
        @Path("id") id: String,
        @Query("date") date: String? = null
    ): ApiResponse<DobongEventDetailDto>

    @GET("/api/v1/mainpage/dobong/images")
    suspend fun getDobongEventImages(
        @Query("date") date: String? = null
    ): ApiResponse<List<DobongEventImageDto>>
}

