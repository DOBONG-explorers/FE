package kr.ac.duksung.dobongzip.model

import java.io.Serializable

data class Notice(
    val id: Int,
    val title: String,
    val date: String,
    val category: NoticeCategory,
    val content: String,
    val remoteId: String? = null,
    val imageUrl: String? = null,
    val pdfUrl: String? = null         // ✅ PDF 파일 URL
) : Serializable
