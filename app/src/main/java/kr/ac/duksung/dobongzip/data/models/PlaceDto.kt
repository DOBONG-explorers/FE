package kr.ac.duksung.dobongzip.data.models

data class PlaceDto(
    val placeId: String,
    val name: String,
    val address: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Int?,
    val distanceText: String?,
    val imageUrl: String?,
    val description: String?,
    val openingHours: List<String>?,
    val priceLevel: Int?,
    val mapsUrl: String?,
    val phone: String?,
    val rating: Double?,
    val reviewCount: Int?
)

data class RandomPlaceDto (
    val placeId: String,
    val name: String,
    val address: String?,
    val latitude: Double?,
    val longitude: Double?,
    val distanceMeters: Int?,
    val distanceText: String?,
    val imageUrl: String?,
    val phone: String?,
    val rating: Double?,
    val reviewCount: Int?
)