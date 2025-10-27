// data/repository/PlacesRepository.kt  (package 경로는 실제 폴더와 일치시켜 주세요)
package kr.ac.duksung.dobongzip.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider

class PlacesRepository {

    private val placesApi = RetrofitProvider.placesApi
    private val dobongApi = RetrofitProvider.dobongApi
    private val likeApi   = RetrofitProvider.placeLikeApi   // ← 이름 확인

    /** 도봉 명소 목록 */
    suspend fun fetchPlaces(lat: Double, lng: Double, limit: Int): List<PlaceDto> =
        withContext(Dispatchers.IO) {
            val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat, lng, limit)
            res.data.orEmpty()                                // ← or emptyList<PlaceDto>()
        }

    /** 명소 상세 (GET /api/v1/map/getPlaceDetail?placeId=) */
    suspend fun fetchPlaceDetail(placeId: String): PlaceDetailDto =
        withContext(Dispatchers.IO) {
            val resp = dobongApi.getPlaceDetail(placeId).execute()
            val body = resp.body()
            if (!resp.isSuccessful || body?.data == null) {
                throw IllegalStateException("상세 조회 실패 (${resp.code()}): ${body?.message}")
            }
            body.data!!
        }

    /** 좋아요 / 좋아요 취소 */
    suspend fun like(placeId: String)   = withContext(Dispatchers.IO) { likeApi.like(placeId) }
    suspend fun unlike(placeId: String) = withContext(Dispatchers.IO) { likeApi.unlike(placeId) }
}
