package kr.ac.duksung.dobongzip.data.repository

import kr.ac.duksung.dobongzip.data.models.PlaceReviewDto
import kr.ac.duksung.dobongzip.data.models.PlaceReviewSummaryDto
import kr.ac.duksung.dobongzip.data.models.ReviewRequest
import kr.ac.duksung.dobongzip.data.network.PlaceReviewApi
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReviewRepository(
    private val api: PlaceReviewApi = RetrofitProvider.placeReviewApi
) {

    suspend fun getReviews(placeId: String, limit: Int? = null): PlaceReviewSummaryDto =
        withContext(Dispatchers.IO) {
            if (placeId.startsWith("dummy_")) {
                return@withContext getDummyReviews(placeId)
            }
            
            val response = api.getReviews(placeId, limit)
            if (response.success && response.data != null) {
                response.data
            } else {
                throw kotlin.IllegalStateException(response.message ?: "리뷰를 불러오지 못했습니다.")
            }
        }

    private fun getDummyReviews(placeId: String): PlaceReviewSummaryDto {
        val dummyReviews = when (placeId) {
            "dummy_wondanghanok" -> listOf(
                PlaceReviewDto(
                    reviewId = 1L,
                    authorName = "도봉구민",
                    authorProfilePhoto = null,
                    rating = 4.5,
                    text = "한옥마을의 정취를 느낄 수 있는 아름다운 도서관입니다. 조용하고 편안한 분위기에서 책을 읽을 수 있어요.",
                    relativeTime = "2주 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 2L,
                    authorName = "독서광",
                    authorProfilePhoto = null,
                    rating = 5.0,
                    text = "전통 한옥 건물이 인상적이에요. 내부도 깔끔하고 책도 많아서 자주 방문하게 됩니다.",
                    relativeTime = "1개월 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 3L,
                    authorName = "문화탐방러",
                    authorProfilePhoto = null,
                    rating = 4.0,
                    text = "한옥 건축의 아름다움을 느낄 수 있는 곳입니다. 주변 경관도 좋아서 산책하기 좋아요.",
                    relativeTime = "2개월 전",
                    mine = false
                )
            )
            "dummy_wonsome35cafe" -> listOf(
                PlaceReviewDto(
                    reviewId = 4L,
                    authorName = "카페러버",
                    authorProfilePhoto = null,
                    rating = 4.5,
                    text = "분위기 좋은 카페예요. 커피 맛도 괜찮고 인테리어가 깔끔해서 좋아요.",
                    relativeTime = "1주 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 5L,
                    authorName = "원썸팬",
                    authorProfilePhoto = null,
                    rating = 5.0,
                    text = "도봉구에서 가장 좋아하는 카페입니다. 디저트도 맛있고 직원분들도 친절해요!",
                    relativeTime = "3주 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 6L,
                    authorName = "카페순례자",
                    authorProfilePhoto = null,
                    rating = 4.0,
                    text = "조용한 분위기에서 공부하기 좋은 카페입니다. 와이파이도 잘 되고요.",
                    relativeTime = "1개월 전",
                    mine = false
                )
            )
            "dummy_changdonghubcube" -> listOf(
                PlaceReviewDto(
                    reviewId = 7L,
                    authorName = "창업준비생",
                    authorProfilePhoto = null,
                    rating = 4.5,
                    text = "창업에 필요한 다양한 정보와 네트워킹 기회를 제공해주는 좋은 공간입니다.",
                    relativeTime = "1주 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 8L,
                    authorName = "스타트업CEO",
                    authorProfilePhoto = null,
                    rating = 5.0,
                    text = "창업 허브의 시설이 정말 좋아요. 세미나와 멘토링 프로그램도 유용합니다.",
                    relativeTime = "2주 전",
                    mine = false
                ),
                PlaceReviewDto(
                    reviewId = 9L,
                    authorName = "비즈니스맨",
                    authorProfilePhoto = null,
                    rating = 4.0,
                    text = "창업 관련 정보를 얻을 수 있는 좋은 공간입니다. 주변 교통도 편리해요.",
                    relativeTime = "1개월 전",
                    mine = false
                )
            )
            else -> emptyList()
        }
        
        val averageRating = if (dummyReviews.isNotEmpty()) {
            dummyReviews.mapNotNull { it.rating }.average()
        } else {
            null
        }
        
        return PlaceReviewSummaryDto(
            placeId = placeId,
            rating = averageRating,
            reviewCount = dummyReviews.size,
            reviews = dummyReviews
        )
    }

    suspend fun createReview(placeId: String, rating: Double, text: String) =
        withContext(Dispatchers.IO) {
            val response = api.createReview(placeId, ReviewRequest(rating, text))
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 작성에 실패했습니다.")
            }
        }

    suspend fun updateReview(placeId: String, reviewId: Long, rating: Double, text: String) =
        withContext(Dispatchers.IO) {
            val response = api.updateReview(placeId, reviewId, ReviewRequest(rating, text))
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 수정에 실패했습니다.")
            }
        }

    suspend fun deleteReview(placeId: String, reviewId: Long) =
        withContext(Dispatchers.IO) {
            val response = api.deleteReview(placeId, reviewId)
            if (!response.success) {
                throw kotlin.IllegalStateException(response.message ?: "리뷰 삭제에 실패했습니다.")
            }
        }
}

