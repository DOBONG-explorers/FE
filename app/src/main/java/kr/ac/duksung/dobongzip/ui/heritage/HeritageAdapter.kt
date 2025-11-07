package kr.ac.duksung.dobongzip.ui.heritage

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.network.HeritageItem

class HeritageAdapter(
    private var items: List<HeritageItem>,
    private val onItemClick: (HeritageItem) -> Unit
) : RecyclerView.Adapter<HeritageAdapter.HeritageViewHolder>() {

    class HeritageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivHeritage: ImageView = itemView.findViewById(R.id.ivHeritage)
        val tvHeritageTitle: TextView = itemView.findViewById(R.id.tvHeritageTitle)
        val btnVisit: Button = itemView.findViewById(R.id.btnVisit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeritageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_heritage, parent, false)
        return HeritageViewHolder(view)
    }

    override fun onBindViewHolder(holder: HeritageViewHolder, position: Int) {
        val item = items[position]

        holder.tvHeritageTitle.text = item.name

        Glide.with(holder.itemView.context)
            .load(item.imageUrl)
            .centerCrop()
            .apply(RequestOptions.bitmapTransform(RoundedCorners(16)))
            .into(holder.ivHeritage)

        holder.btnVisit.setOnClickListener(null)
        holder.btnVisit.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(newList: List<HeritageItem>) {
        items = newList
        notifyDataSetChanged()
    }
}

