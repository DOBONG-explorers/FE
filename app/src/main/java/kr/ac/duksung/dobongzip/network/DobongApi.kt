package kr.ac.duksung.dobongzip.network

import kr.ac.duksung.dobongzip.data.ApiResponse
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface DobongApi {
    @GET("api/v1/map/getPlaceDetail") // Swagger 실제 path
    fun getPlaceDetail(
        @Query("placeId") placeId: String
    ): Call<ApiResponse<PlaceDetailDto>>
}
