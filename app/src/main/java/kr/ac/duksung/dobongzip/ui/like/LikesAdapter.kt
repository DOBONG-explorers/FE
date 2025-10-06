package kr.ac.duksung.dobongzip.ui.like

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.imageview.ShapeableImageView
import kr.ac.duksung.dobongzip.R

class LikesAdapter(
    private val items: List<LikeItem>
) : RecyclerView.Adapter<LikesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPhoto: ShapeableImageView = view.findViewById(R.id.ivPhoto)
        val tvPlace: TextView = view.findViewById(R.id.tvPlace)
        val ivHeart: ImageView = view.findViewById(R.id.ivHeart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_like_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.ivPhoto.setImageResource(item.imageResId)
        holder.tvPlace.text = item.placeName
        holder.ivHeart.setImageResource(R.drawable.love_fill)
    }

    override fun getItemCount(): Int = items.size
}
