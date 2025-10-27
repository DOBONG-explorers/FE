package kr.ac.duksung.dobongzip.data.repository

import kr.ac.duksung.dobongzip.data.models.LikeCardDto
import kr.ac.duksung.dobongzip.data.network.PlaceLikeApi

class LikeRepository(
    private val api: PlaceLikeApi
) {
    suspend fun getMyLikes(size: Int, order: String): List<LikeCardDto> {
        val res = api.getMyLikes(size = size, order = order)
        return res.data ?: emptyList()
    }

    suspend fun like(placeId: String) {
        api.like(placeId)
    }

    suspend fun unlike(placeId: String) {
        api.unlike(placeId)
    }
}
