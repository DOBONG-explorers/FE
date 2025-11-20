package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.models.RandomPlaceDto
import kr.ac.duksung.dobongzip.data.models.TopPlaceDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PlacesApi {
    @GET("/api/v1/places/dobong")
    suspend fun getPlaces(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("limit") limit: Int = 10
    ): ApiResponse<List<PlaceDto>>

    @GET("/api/v1/mainpage/random-place")
    suspend fun getRandomPlaces(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): ApiResponse<RandomPlaceDto>

    @GET("/api/v1/mainpage/top")
    suspend fun getTopPlaces(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("limit") limit: Int = 3
    ): ApiResponse<List<TopPlaceDto>>

    @GET("/api/v1/places/{placeId}")
    suspend fun getPlaceDetail(
        @Path("placeId") placeId: String
    ): ApiResponse<PlaceDetailDto>
}
