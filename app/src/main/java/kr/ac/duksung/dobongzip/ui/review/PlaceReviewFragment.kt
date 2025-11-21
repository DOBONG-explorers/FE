package kr.ac.duksung.dobongzip.ui.review

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kr.ac.duksung.dobongzip.data.models.PlaceReviewDto
import kr.ac.duksung.dobongzip.data.models.PlaceReviewSummaryDto
import kr.ac.duksung.dobongzip.databinding.FragmentExhibitReviewBinding

class PlaceReviewFragment : Fragment() {

    private var _binding: FragmentExhibitReviewBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PlaceReviewViewModel by viewModels()
    private lateinit var adapter: PlaceReviewAdapter

    private val placeId: String by lazy {
        requireArguments().getString(ARG_PLACE_ID)
            ?: throw IllegalArgumentException("placeId is required")
    }
    private val placeName: String? by lazy { requireArguments().getString(ARG_PLACE_NAME) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExhibitReviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecycler()
        observeViewModel()
        registerResultListeners()
        viewModel.initialize(placeId)
    }

    private fun setupRecycler() {
        adapter = PlaceReviewAdapter(
            onEdit = { },
            onDelete = { confirmDelete(it) },
            onUpdate = { reviewId, rating, text ->
                if (reviewId > 0) {
                    android.util.Log.d("PlaceReviewFragment", "리뷰 수정: reviewId=$reviewId, rating=$rating, text=$text")
                    viewModel.updateReview(reviewId, rating, text)
                } else {
                    Toast.makeText(requireContext(), "리뷰 ID가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        )
        binding.rvReview.layoutManager = LinearLayoutManager(requireContext())
        binding.rvReview.adapter = adapter
        binding.viewRating.setOnClickListener {
            showReviewEditor()
        }
    }

    private fun observeViewModel() {
        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            updateSummary(summary)
        }
        viewModel.reviews.observe(viewLifecycleOwner) { reviews ->
            adapter.submitList(reviews)
            binding.rvReview.isVisible = reviews.isNotEmpty()
            binding.txtReviewEmpty.isVisible = reviews.isEmpty()
            
            val hasMyReview = reviews.any { it.mine }
            (activity as? ReviewActivity)?.updateWriteButtonVisibility(!hasMyReview)
        }
        viewModel.error.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.consumeError()
            }
        }
        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }
    }

    private fun updateSummary(summary: PlaceReviewSummaryDto) {
        val reviews = summary.reviews
        val totalCount = reviews.size
        val count = summary.reviewCount ?: totalCount
        
        val ratingCounts = IntArray(6)
        var totalRating = 0.0
        var ratingCount = 0
        
        reviews.forEach { review ->
            review.rating?.let { rating ->
                val ratingInt = rating.toInt().coerceIn(1, 5)
                ratingCounts[ratingInt]++
                totalRating += rating
                ratingCount++
            }
        }
        
        val avg = if (ratingCount > 0) totalRating / ratingCount else (summary.rating ?: 0.0)
        
        binding.txtAverageRating.text = String.format("%.1f", avg)
        binding.ratingSummaryBar.rating = avg.toFloat()
        binding.txtReviewCount.text = "리뷰 ${count}개"
        
        val maxCount = ratingCounts.maxOrNull() ?: 1
        binding.progressBar5.progress = if (maxCount > 0) (ratingCounts[5] * 100 / maxCount) else 0
        binding.progressBar4.progress = if (maxCount > 0) (ratingCounts[4] * 100 / maxCount) else 0
        binding.progressBar3.progress = if (maxCount > 0) (ratingCounts[3] * 100 / maxCount) else 0
        binding.progressBar2.progress = if (maxCount > 0) (ratingCounts[2] * 100 / maxCount) else 0
        binding.progressBar1.progress = if (maxCount > 0) (ratingCounts[1] * 100 / maxCount) else 0
    }

    private fun showReviewEditor(existing: PlaceReviewDto? = null) {
        if (existing != null) {
            ReviewEditFragment.newInstance(
                placeName = placeName,
                reviewId = existing.reviewId,
                initialRating = existing.rating ?: 0.0,
                initialText = existing.text
            ).show(childFragmentManager, ReviewEditFragment.RESULT_KEY)
        } else {
            showCreateReviewDialog()
        }
    }
    
    private fun showCreateReviewDialog() {
        val dialogView = layoutInflater.inflate(kr.ac.duksung.dobongzip.R.layout.dialog_review_input, null)
        val ratingBar = dialogView.findViewById<android.widget.RatingBar>(kr.ac.duksung.dobongzip.R.id.dialog_rating)
        val editText = dialogView.findViewById<android.widget.EditText>(kr.ac.duksung.dobongzip.R.id.dialog_review_text)
        val submitButton = dialogView.findViewById<android.widget.Button>(kr.ac.duksung.dobongzip.R.id.dialog_submit_button)
        
        ratingBar.stepSize = 1.0f
        ratingBar.rating = 0f
        
        var rating = 0.0
        var text = ""
        
        ratingBar.setOnRatingBarChangeListener { _, r, _ ->
            rating = r.toDouble()
            updateDialogButtonState(submitButton, rating, editText.text?.toString()?.trim().orEmpty())
        }
        
        editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                text = s?.toString()?.trim().orEmpty()
                updateDialogButtonState(submitButton, rating, text)
            }
        })
        
        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setNegativeButton("취소", null)
            .create()
        
        submitButton.setOnClickListener {
            if (rating > 0 && text.isNotBlank()) {
                viewModel.createReview(rating, text)
                dialog.dismiss()
            } else {
                android.widget.Toast.makeText(requireContext(), "별점과 내용을 입력해주세요.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }
    
    private fun updateDialogButtonState(button: android.widget.Button, rating: Double, text: String) {
        val isValid = rating > 0 && text.isNotBlank()
        button.isEnabled = isValid
        button.alpha = if (isValid) 1.0f else 0.5f
    }

    private fun confirmDelete(review: PlaceReviewDto) {
        if (review.reviewId <= 0) {
            Toast.makeText(requireContext(), "리뷰 ID가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setMessage("이 리뷰를 삭제하시겠어요?")
            .setPositiveButton("삭제") { _, _ ->
                viewModel.deleteReview(review.reviewId)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun registerResultListeners() {
        parentFragmentManager.setFragmentResultListener(REQUEST_OPEN_EDITOR, viewLifecycleOwner) { _, _ ->
            showReviewEditor()
        }

        childFragmentManager.setFragmentResultListener(ReviewEditFragment.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            val rating = bundle.getDouble(ReviewEditFragment.RESULT_RATING, 0.0)
            val text = bundle.getString(ReviewEditFragment.RESULT_TEXT).orEmpty()
            val reviewId = bundle.getLong(ReviewEditFragment.RESULT_REVIEW_ID, -1L)
            if (reviewId > 0) {
                viewModel.updateReview(reviewId, rating, text)
            } else if (reviewId == -1L) {
                viewModel.createReview(rating, text)
            } else {
                viewModel.createReview(rating, text)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PLACE_ID = "arg_place_id"
        private const val ARG_PLACE_NAME = "arg_place_name"
        const val REQUEST_OPEN_EDITOR = "request_open_review_editor"

        fun newInstance(placeId: String, placeName: String?): PlaceReviewFragment {
            return PlaceReviewFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PLACE_ID, placeId)
                    putString(ARG_PLACE_NAME, placeName)
                }
            }
        }
    }
}

