package kr.ac.duksung.dobongzip.data.models

data class PlaceDetailDto(
    val placeId: String,
    val name: String,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val liked: Boolean? = null,
    val likeCount: Int? = null
)
