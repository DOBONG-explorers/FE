package kr.ac.duksung.dobongzip.ui.top

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.TopPlaceDto
import kr.ac.duksung.dobongzip.databinding.ItemTopPlacesBinding

class TopPlacesAdapter(
    private val onItemClick: (TopPlaceDto) -> Unit = {}
) : ListAdapter<TopPlaceDto, TopPlacesAdapter.TopPlaceViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopPlaceViewHolder {
        val binding = ItemTopPlacesBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopPlaceViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: TopPlaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TopPlaceViewHolder(
        private val binding: ItemTopPlacesBinding,
        private val onItemClick: (TopPlaceDto) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TopPlaceDto) {
            binding.placeTitle.text = item.name.ifBlank { "정보 없음" }

            val rating = item.rating?.coerceIn(0.0, 5.0) ?: 0.0
            binding.placeRating.rating = rating.toFloat()
            binding.placeRatingValue.text = String.format("%.1f", rating)

            val reviewCount = item.reviewCount ?: 0
            binding.placeReviewCount.text = "($reviewCount)"

            val distanceText = item.distanceText
                ?: item.distanceMeters?.takeIf { it > 0 }?.let { meters ->
                    if (meters >= 1000) {
                        val km = meters / 1000.0
                        String.format("%.1fkm", km)
                    } else {
                        "${meters}m"
                    }
                }
            binding.placeDistance.text = distanceText
                ?.let { "내 위치로부터: $it" }
                ?: binding.root.context.getString(R.string.top_place_distance_unknown)

            binding.placePhone.text = item.phone?.takeIf { it.isNotBlank() }
                ?.let { "번호:  $it" }
                ?: binding.root.context.getString(R.string.top_place_phone_unknown)

            Glide.with(binding.placeImage)
                .load(item.imageUrl)
                .apply(
                    RequestOptions()
                        .placeholder(R.drawable.placeholder)
                        .error(R.drawable.placeholder)
                        .transform(RoundedCorners(binding.root.resources.getDimensionPixelSize(R.dimen.top_place_image_radius)))
                )
                .into(binding.placeImage)

            binding.root.setOnClickListener { onItemClick(item) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<TopPlaceDto>() {
            override fun areItemsTheSame(oldItem: TopPlaceDto, newItem: TopPlaceDto): Boolean =
                oldItem.placeId == newItem.placeId

            override fun areContentsTheSame(oldItem: TopPlaceDto, newItem: TopPlaceDto): Boolean =
                oldItem == newItem
        }
    }
}

