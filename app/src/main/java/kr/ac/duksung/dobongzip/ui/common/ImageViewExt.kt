package kr.ac.duksung.dobongzip.ui.common

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.R

fun ImageView.loadProfile(uri: Uri?) {
    if (uri == null) {
        setImageResource(R.drawable.prf3)
    } else {
        Glide.with(this).load(uri).centerCrop().into(this)
    }
}

fun ImageView.loadProfileUrl(url: String?) {
    if (url.isNullOrBlank()) {
        setImageResource(R.drawable.prf3)
    } else {
        Glide.with(this).load(url).centerCrop().into(this)
    }
}
