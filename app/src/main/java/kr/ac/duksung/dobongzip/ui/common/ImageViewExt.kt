package kr.ac.duksung.dobongzip.ui.common

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView.loadProfile(uri: Uri?) {
    if (uri == null) {
        setImageResource(kr.ac.duksung.dobongzip.R.drawable.prf3) // 기본 이미지
        return
    }
    Glide.with(this).load(uri).centerCrop().into(this)
}
