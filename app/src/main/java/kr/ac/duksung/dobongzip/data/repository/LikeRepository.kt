// data/repository/LikeRepository.kt
package kr.ac.duksung.dobongzip.data.repository

import android.util.Log
import kr.ac.duksung.dobongzip.data.models.LikeCardDto
import kr.ac.duksung.dobongzip.data.network.PlaceLikeApi

class LikeRepository(
    private val api: PlaceLikeApi
) {
    suspend fun getMyLikes(size: Int, order: String): List<LikeCardDto> {
        val res = api.getMyLikes(size = size, order = order)
        return res.data ?: run {
            Log.w("LikeRepository", "getMyLikes() returned null data")
            emptyList()
        }
    }

    suspend fun like(placeId: String) {
        val res = api.like(placeId)

        if (!res.success) {
            val msg = res.errorMessage
                ?: res.message
                ?: "Failed to like: $placeId"
            throw IllegalStateException(msg)
        }
    }

    suspend fun unlike(placeId: String) {
        val res = api.unlike(placeId)
        if (!res.success) {
            val msg = res.errorMessage
                ?: res.message
                ?: "Failed to unlike: $placeId"
            throw IllegalStateException(msg)
        }
    }

}
