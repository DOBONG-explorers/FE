// ui/like/LikesAdapter.kt
package kr.ac.duksung.dobongzip.ui.like

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import kr.ac.duksung.dobongzip.R

class LikesAdapter(
    private val onClickUnlike: (LikeItemUi) -> Unit
) : ListAdapter<LikeItemUi, LikesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<LikeItemUi>() {
        override fun areItemsTheSame(oldItem: LikeItemUi, newItem: LikeItemUi) =
            oldItem.placeId == newItem.placeId
        override fun areContentsTheSame(oldItem: LikeItemUi, newItem: LikeItemUi) =
            oldItem == newItem
    }

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ShapeableImageView = view.findViewById(R.id.ivPhoto)
        val tvPlace: TextView = view.findViewById(R.id.tvPlace)
        val ivHeart: ImageView = view.findViewById(R.id.ivHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_like_card, parent, false)
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
        return VH(view)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val item = getItem(position)

        Glide.with(h.ivPhoto)
            .load(item.imageUrl)
            //.placeholder(R.drawable.bg_image_placeholder) // 없으면 제거해도 됨
            .centerCrop()
            .into(h.ivPhoto)

        h.tvPlace.text = item.placeName
        h.ivHeart.setImageResource(R.drawable.love_fill)

        h.ivHeart.setOnClickListener {
            // 낙관적 UI: 아이콘 먼저 변경 후 콜백으로 서버 요청
            h.ivHeart.setImageResource(R.drawable.love)
            onClickUnlike(item)
        }
    }
}
