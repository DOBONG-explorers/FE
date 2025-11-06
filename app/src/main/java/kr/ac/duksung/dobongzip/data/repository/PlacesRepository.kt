// data/repository/PlacesRepository.kt
package kr.ac.duksung.dobongzip.data.repository

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.data.models.ApiResponse
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider

class PlacesRepository {

    private val placesApi = RetrofitProvider.placesApi
    private val dobongApi = RetrofitProvider.dobongApi
    private val likeApi   = RetrofitProvider.placeLikeApi
    
    private var lastErrorMessage: String? = null

    /** 도봉 명소 목록 */
    suspend fun fetchPlaces(lat: Double, lng: Double, limit: Int): List<PlaceDto> =
        withContext(Dispatchers.IO) {
            val res: ApiResponse<List<PlaceDto>> = placesApi.getPlaces(lat, lng, limit)
            res.data.orEmpty()
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

    /** 추천 장소 가져오기 - /api/v1/places/dobong 엔드포인트 사용 */
    suspend fun getRecommendedPlace(lat: Double, lng: Double): PlaceDto? =
        withContext(Dispatchers.IO) {
            try {
                lastErrorMessage = null
                Log.d("PlacesRepository", "Requesting recommended place from dobong API: lat=$lat, lng=$lng")
                
                // limit을 충분히 크게 설정하여 여러 장소 중에서 선택
                val limit = 20
                val response = placesApi.getPlaces(lat, lng, limit)
                
                Log.d("PlacesRepository", "Response received: success=${response.success}, data count=${response.data?.size}, message=${response.message}")
                
                if (response.success && !response.data.isNullOrEmpty()) {
                    // 리스트에서 랜덤하게 하나 선택
                    val places = response.data
                    val randomPlace = places.random()
                    Log.d("PlacesRepository", "Selected random place: ${randomPlace.name} (from ${places.size} places)")
                    randomPlace
                } else if (response.success && response.data.isNullOrEmpty()) {
                    lastErrorMessage = response.message ?: "추천할 장소가 없습니다."
                    Log.e("PlacesRepository", "API returned empty list: message=$lastErrorMessage")
                    null
                } else {
                    lastErrorMessage = response.message ?: "서버에서 추천 장소를 반환하지 않았습니다."
                    Log.e("PlacesRepository", "API returned failure: success=${response.success}, message=$lastErrorMessage")
                    null
                }
            } catch (e: HttpException) {
                val errorCode = e.code()
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("PlacesRepository", "HTTP Error $errorCode: $errorBody", e)
                
                lastErrorMessage = when (errorCode) {
                    500 -> "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                    404 -> "추천 장소를 찾을 수 없습니다."
                    400 -> "잘못된 요청입니다. 위치 정보를 확인해주세요."
                    401, 403 -> "인증이 필요합니다. 다시 로그인해주세요."
                    else -> "서버 오류 (HTTP $errorCode). 잠시 후 다시 시도해주세요."
                }
                null
            } catch (e: Exception) {
                lastErrorMessage = "네트워크 연결을 확인해주세요: ${e.message}"
                Log.e("PlacesRepository", "Failed to get recommended place: ${e.message}", e)
                e.printStackTrace()
                null
            }
        }
    
    fun getLastErrorMessage(): String? = lastErrorMessage

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

        val placeId = first.placeId  // ← 필드명이 다르면 first.id 등으로 교체

        likeApi.like(placeId)        // 서버에 좋아요 등록
        return@withContext placeId
    }

}
