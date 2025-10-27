package kr.ac.duksung.dobongzip.data.models

data class ApiResponse<T>(
    val success: Boolean,
    val httpStatus: Int?,
    val message: String?,
    val data: T?
)
