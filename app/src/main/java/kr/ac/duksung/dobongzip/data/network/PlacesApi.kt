package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import retrofit2.http.GET
import retrofit2.http.Query

interface PlacesApi {
    @GET("/api/v1/places/dobong")
    suspend fun getPlaces(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<PlaceDto>>
}
