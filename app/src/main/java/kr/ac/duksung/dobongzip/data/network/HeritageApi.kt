package kr.ac.duksung.dobongzip.data.network

import kr.ac.duksung.dobongzip.data.models.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface HeritageApi {
    @GET("/api/v1/mainpage/heritage/list")
    suspend fun getHeritageList(): ApiResponse<List<HeritageItem>>

    @GET("/api/v1/mainpage/heritage/{id}")
    suspend fun getHeritageDetail(@Path("id") id: String): ApiResponse<HeritageDetail>
}

data class HeritageItem(
    val id: String,
    val name: String,
    val imageUrl: String
)

data class HeritageDetail(
    val ID: String? = null,
    val NUMBER: Int? = null,
    val SHD_NM: String? = null,
    val IMAGE_URL: String? = null,
    val LATITUDE: String? = null,
    val LONGITUDE: String? = null,
    val CO_F1: String? = null,
    val CO_F2: String? = null,
    val SCD_JIBUN_ADDR: String? = null,
    val CO_F3: String? = null,
    val VA_F2: String? = null,
    val VA_F3: String? = null,
    val VA_F4: String? = null,
    val VA_F5: String? = null,
    val VA_F6: String? = null,
    val VA_F7: String? = null,
    val VA_F8: String? = null,
    val VA_F9: String? = null,
    val VA_F10: String? = null,
    val VA_F11: String? = null,
    val VA_F12: String? = null
)
