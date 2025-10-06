package kr.ac.duksung.dobongzip.ui.like

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kr.ac.duksung.dobongzip.R

class LikesAdapter(
    private val items: MutableList<LikeItem>,
    private val onRemoved: (LikeItem) -> Unit = {}
) : RecyclerView.Adapter<LikesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ShapeableImageView = view.findViewById(R.id.ivPhoto)
        val tvPlace: TextView = view.findViewById(R.id.tvPlace)
        val ivHeart: ImageView = view.findViewById(R.id.ivHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_like_card, parent, false)
        (view.layoutParams as? ViewGroup.MarginLayoutParams)?.setMargins(0, 0, 0, 0)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivPhoto.setImageResource(item.imageResId)
        holder.tvPlace.text = item.placeName

        // 기본은 꽉 찬 하트
        holder.ivHeart.setImageResource(R.drawable.love_fill)

        holder.ivHeart.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener

            // 하트 외곽선으로 변경
            holder.ivHeart.setImageResource(R.drawable.love_outline)

            // 목록에서 제거
            val removed = items.removeAt(pos)
            notifyItemRemoved(pos)
            // 깜빡임 줄이고 싶으면 (선택)
            // notifyItemRangeChanged(pos, itemCount - pos)

            onRemoved(removed)
        }
    }

    override fun getItemCount(): Int = items.size
}
