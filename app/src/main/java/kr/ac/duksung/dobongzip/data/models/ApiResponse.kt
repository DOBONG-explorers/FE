package kr.ac.duksung.dobongzip.data.models

data class ApiResponse<T>(
    val success: Boolean,
    val httpStatus: Int?,
    val message: String?,
    val data: T?
)

data class DobongEventDto(
    val id: String?,
    val title: String?,
    val dateText: String?,
    val mainImg: String?
)

data class DobongEventDetailDto(
    val DATE: String?,
    val STRTDATE: String?,
    val END_DATE: String?,
    val GU_NAME: String?,
    val TITLE: String?,
    val PLACE: String?,
    val ORG_LINK: String?,
    val USE_TRGT: String?,
    val USE_FEE: String?,
    val MAIN_IMG: String?,
    val CODE_NAME: String?,
    val PROGRAM: String?,
    val ETC_DESC: String?
)

data class DobongEventImageDto(
    val id: String?,
    val imageUrl: String?
)
