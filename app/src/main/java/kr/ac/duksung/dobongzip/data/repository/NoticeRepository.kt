package kr.ac.duksung.dobongzip.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.data.models.DobongEventDetailDto
import kr.ac.duksung.dobongzip.data.models.DobongEventDto
import kr.ac.duksung.dobongzip.data.models.DobongEventImageDto
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.model.Notice
import kr.ac.duksung.dobongzip.model.NoticeCategory

class NoticeRepository {

    private val noticeApi = RetrofitProvider.noticeApi

    suspend fun fetchDobongEventDetail(id: String, date: String? = null): DobongEventDetailDto =
        withContext(Dispatchers.IO) {
            val response = noticeApi.getDobongEventDetail(id, date)
            if (response.success && response.data != null) {
                response.data
            } else {
                throw IllegalStateException(response.message ?: "행사 상세 정보를 불러오지 못했습니다.")
            }
        }

    suspend fun fetchDobongEvents(date: String? = null): List<Notice> = withContext(Dispatchers.IO) {
        val response = noticeApi.getDobongEvents(date)
        if (response.success) {
            response.data.orEmpty().mapNotNull { it.toNotice() }
        } else {
            throw IllegalStateException(response.message ?: "행사 정보를 불러오지 못했습니다.")
        }
    }

    suspend fun fetchDobongEventImages(date: String? = null): List<DobongEventImageDto> =
        withContext(Dispatchers.IO) {
            val response = noticeApi.getDobongEventImages(date)
            if (response.success) {
                response.data.orEmpty().mapNotNull { dto ->
                    val id = dto.id ?: return@mapNotNull null
                    val imageUrl = sanitizeUrl(dto.imageUrl) ?: return@mapNotNull null
                    DobongEventImageDto(id = id, imageUrl = imageUrl)
                }
            } else {
                throw IllegalStateException(response.message ?: "행사 이미지 정보를 불러오지 못했습니다.")
            }
        }

    private fun DobongEventDto.toNotice(): Notice? {
        val idValue = id ?: return null
        val titleValue = title ?: return null
        val dateValue = dateText ?: ""
        val imageUrl = sanitizeUrl(mainImg)
        val content = buildString {
            append(titleValue)
            if (dateValue.isNotBlank()) {
                append("\n일정: ").append(dateValue)
            }
        }.ifBlank { "행사 상세 정보는 추후에 제공될 예정입니다." }

        val numericId = idValue.toIntOrNull() ?: idValue.hashCode()

        return Notice(
            id = numericId,
            title = titleValue,
            date = dateValue,
            category = NoticeCategory.EVENT,
            content = content,
            remoteId = idValue,
            imageUrl = imageUrl
        )
    }

    private fun sanitizeUrl(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return raw.removePrefix("[").removeSuffix("]").trim().takeIf { it.isNotBlank() }
    }
}

