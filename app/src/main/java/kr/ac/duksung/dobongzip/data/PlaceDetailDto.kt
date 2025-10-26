package kr.ac.duksung.dobongzip.data

data class ApiResponse<T>(
    val success: Boolean,
    val httpStatus: Int?,
    val message: String?,
    val data: T?
)

data class PlaceDetailDto(
    val placeId: String,
    val name: String,
    val address: String?,
    val description: String?,
    val openingHours: List<String>?,
    val priceLevel: Int?,
    val phone: String?,
    val rating: Float?,
    val reviewCount: Int?,
    val photos: List<String>?,
    val location: LatLngDto?,
    val liked: Boolean?
)

data class LatLngDto(
    val latitude: Double,
    val longitude: Double
)
