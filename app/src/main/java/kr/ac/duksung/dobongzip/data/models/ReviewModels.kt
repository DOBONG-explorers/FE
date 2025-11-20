package kr.ac.duksung.dobongzip.data.models

data class PlaceReviewSummaryDto(
    val placeId: String,
    val rating: Double?,
    val reviewCount: Int?,
    val reviews: List<PlaceReviewDto> = emptyList()
)

data class PlaceReviewDto(
    val reviewId: Long,
    val authorName: String?,
    val authorProfilePhoto: String?,
    val rating: Double?,
    val text: String?,
    val relativeTime: String?,
    val mine: Boolean
)

data class ReviewRequest(
    val rating: Double,
    val text: String
)

