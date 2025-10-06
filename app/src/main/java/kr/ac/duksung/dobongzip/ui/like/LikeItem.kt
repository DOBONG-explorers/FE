package kr.ac.duksung.dobongzip.ui.like

data class LikeItem(
    val id: Long = System.nanoTime(),
    val placeName: String,
    val imageResId: Int
)
