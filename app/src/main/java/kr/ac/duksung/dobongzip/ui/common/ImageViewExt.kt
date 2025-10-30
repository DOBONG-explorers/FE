// kr/ac/duksung/dobongzip/ui/common/ImageViewExt.kt
package kr.ac.duksung.dobongzip.ui.common

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.R

/** Uri 또는 null을 받아 프로필 미리보기 */
fun ImageView.loadProfile(uri: Uri?) {
    if (uri == null) {
        setImageResource(R.drawable.prf3) // 기본 이미지
        return
    }
    Glide.with(this.context)
        .load(uri)
        .centerCrop()
        .placeholder(R.drawable.prf3)
        .into(this)
}

/** URL을 받아 서버 이미지를 로드 */
fun ImageView.loadProfileUrl(url: String?) {
    if (url.isNullOrBlank()) {
        setImageResource(R.drawable.prf3)
        return
    }
    Glide.with(this.context)
        .load(url)
        .centerCrop()
        .placeholder(R.drawable.prf3)
        .into(this)
}
