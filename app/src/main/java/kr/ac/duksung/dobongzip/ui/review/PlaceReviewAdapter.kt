package kr.ac.duksung.dobongzip.ui.review

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.PlaceReviewDto
import kr.ac.duksung.dobongzip.databinding.ItemReviewBinding

class PlaceReviewAdapter(
    private val onEdit: (PlaceReviewDto) -> Unit,
    private val onDelete: (PlaceReviewDto) -> Unit,
    private val onUpdate: (Long, Double, String) -> Unit
) : ListAdapter<PlaceReviewDto, PlaceReviewAdapter.ReviewViewHolder>(DiffCallback) {
    
    private var editingPosition: Int = -1
    private var editingReviewId: Long? = null

    object DiffCallback : DiffUtil.ItemCallback<PlaceReviewDto>() {
        override fun areItemsTheSame(oldItem: PlaceReviewDto, newItem: PlaceReviewDto): Boolean =
            oldItem.reviewId == newItem.reviewId

        override fun areContentsTheSame(oldItem: PlaceReviewDto, newItem: PlaceReviewDto): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReviewViewHolder(binding, onEdit, onDelete, onUpdate, this)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val item = getItem(position)
        val isEditing = editingPosition == position
        holder.bind(item, isEditing, editingReviewId)
    }
    
    fun setEditingPosition(position: Int, reviewId: Long?) {
        val oldPosition = editingPosition
        editingPosition = position
        editingReviewId = reviewId
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }
    
    fun clearEditing() {
        val oldPosition = editingPosition
        editingPosition = -1
        editingReviewId = null
        if (oldPosition != -1) {
            notifyItemChanged(oldPosition)
        }
    }

    class ReviewViewHolder(
        private val binding: ItemReviewBinding,
        private val onEdit: (PlaceReviewDto) -> Unit,
        private val onDelete: (PlaceReviewDto) -> Unit,
        private val onUpdate: (Long, Double, String) -> Unit,
        private val adapter: PlaceReviewAdapter
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PlaceReviewDto, isEditing: Boolean, editingReviewId: Long?) {
            binding.userName.text = item.authorName ?: "익명"
            binding.reviewTime.text = item.relativeTime ?: ""
            binding.reviewRating.rating = (item.rating ?: 0.0).toFloat()
            binding.reviewRating.alpha = 1.0f
            binding.reviewRating.progressTintList = android.content.res.ColorStateList.valueOf(0xFF0B79D0.toInt())
            binding.reviewRating.secondaryProgressTintList = android.content.res.ColorStateList.valueOf(0xFF0B79D0.toInt())
            
            val photo = item.authorProfilePhoto
            if (!photo.isNullOrBlank()) {
                Glide.with(binding.profileImage)
                    .load(photo)
                    .placeholder(R.drawable.prf3)
                    .circleCrop()
                    .into(binding.profileImage)
            } else {
                binding.profileImage.setImageResource(R.drawable.prf3)
            }

            val menuButton = binding.menuButton
            if (item.mine) {
                menuButton.visibility = View.VISIBLE
                
                if (isEditing && editingReviewId == item.reviewId) {
                    binding.reviewContentText.visibility = View.GONE
                    binding.reviewContentEdit.visibility = View.VISIBLE
                    binding.reviewContentEdit.setText(item.text ?: "")
                    binding.reviewContentEdit.requestFocus()
                    
                    binding.reviewRating.rating = (item.rating ?: 0.0).toFloat()
                    binding.reviewRating.isEnabled = true
                    binding.reviewRating.alpha = 1.0f
                    binding.reviewRating.setOnRatingBarChangeListener(null)
                    
                    menuButton.setImageResource(R.drawable.ic_check)
                    menuButton.contentDescription = "수정 완료"
                    menuButton.setOnClickListener {
                        val newText = binding.reviewContentEdit.text?.toString()?.trim().orEmpty()
                        val newRating = binding.reviewRating.rating
                        val ratingValue = newRating.toDouble()
                        val currentReviewId = item.reviewId
                        android.util.Log.d("PlaceReviewAdapter", "체크 클릭: item.reviewId=$currentReviewId, editingReviewId=$editingReviewId, rating=$ratingValue, text=$newText, ratingFloat=$newRating")
                        if (currentReviewId > 0 && newText.isNotBlank() && newRating > 0) {
                            android.util.Log.d("PlaceReviewAdapter", "리뷰 수정 호출: reviewId=$currentReviewId, rating=$ratingValue, text=$newText")
                            onUpdate(currentReviewId, ratingValue, newText)
                            adapter.clearEditing()
                        } else {
                            android.util.Log.e("PlaceReviewAdapter", "리뷰 수정 실패: reviewId=$currentReviewId, rating=$ratingValue, text=$newText, ratingFloat=$newRating")
                            val message = when {
                                currentReviewId <= 0 -> "리뷰 ID가 유효하지 않습니다."
                                newText.isBlank() && newRating <= 0 -> "별점과 내용을 모두 입력해주세요."
                                newText.isBlank() -> "리뷰 내용을 입력해주세요."
                                newRating <= 0 -> "별점을 선택해주세요."
                                else -> "별점과 내용을 모두 입력해주세요."
                            }
                            android.widget.Toast.makeText(binding.root.context, message, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    binding.reviewContentText.visibility = View.VISIBLE
                    binding.reviewContentEdit.visibility = View.GONE
                    binding.reviewContentText.text = item.text ?: ""
                    
                    binding.reviewRating.isEnabled = false
                    binding.reviewRating.setOnRatingBarChangeListener(null)
                    binding.reviewRating.alpha = 1.0f
                    
                    menuButton.setImageResource(R.drawable.ic_more_dots)
                    menuButton.contentDescription = "메뉴"
                    menuButton.setOnClickListener {
                        showMenu(it, item)
                    }
                }
            } else {
                menuButton.visibility = View.GONE
                binding.reviewContentText.visibility = View.VISIBLE
                binding.reviewContentEdit.visibility = View.GONE
                binding.reviewContentText.text = item.text ?: ""
                menuButton.setOnClickListener(null)
            }
        }

        private fun showMenu(anchor: View, item: PlaceReviewDto) {
            android.widget.PopupMenu(anchor.context, anchor).apply {
                inflate(R.menu.menu_review_item)
                setOnMenuItemClickListener { menuItem ->
                    when (menuItem.itemId) {
                        R.id.action_edit -> {
                            adapter.setEditingPosition(adapterPosition, item.reviewId)
                            true
                        }
                        R.id.action_delete -> {
                            onDelete(item)
                            true
                        }
                        else -> false
                    }
                }
            }.show()
        }
    }
}

