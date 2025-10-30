// data/repository/PlacesRepository.kt
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
    private val likeApi   = RetrofitProvider.placeLikeApi

    /** 도봉 명소 목록 */
    suspend fun fetchPlaces(lat: Double, lng: Double, limit: Int): List<PlaceDto> =
        withContext(Dispatchers.IO) {
            val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat, lng, limit)
            res.data.orEmpty()
        }

    /** 명소 상세 (GET /api/v1/map/getPlaceDetail?placeId=) */
    suspend fun fetchPlaceDetail(placeId: String): PlaceDetailDto =
        withContext(Dispatchers.IO) {
            // dobongApi.getPlaceDetail(...) 이 Call<ApiResponse<PlaceDetailDto>> 라고 가정
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

    /** 좋아요 토글 (현재 상태를 전달받아 반대로 처리) */
    suspend fun toggleLike(placeId: String, isCurrentlyLiked: Boolean) =
        withContext(Dispatchers.IO) {
            if (isCurrentlyLiked) likeApi.unlike(placeId) else likeApi.like(placeId)
        }

    // data/repository/PlacesRepository.kt
    suspend fun likeFirstPlaceFromServer(lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
        // 서버 정렬 기본값 기준으로 첫 1개만 가져옴
        val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat = lat, lng = lng, limit = 1)
        val first = res.data?.firstOrNull() ?: return@withContext null

        // PlaceDto에 식별자가 무엇인지에 따라 맞춰주세요.
        // 보통 placeId 또는 id를 씁니다.
        val placeId = first.placeId  // ← 필드명이 다르면 first.id 등으로 교체

        likeApi.like(placeId)        // 서버에 좋아요 등록
        return@withContext placeId
    }

}
