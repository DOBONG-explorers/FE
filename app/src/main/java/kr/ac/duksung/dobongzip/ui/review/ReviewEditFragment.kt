package kr.ac.duksung.dobongzip.ui.review

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kr.ac.duksung.dobongzip.databinding.FragmentEditReviewBinding

class ReviewEditFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEditReviewBinding? = null
    private val binding get() = _binding!!

    private val existingReviewId: Long?
        get() = arguments?.getLong(ARG_REVIEW_ID)?.takeIf { it != 0L }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditReviewBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val placeName = arguments?.getString(ARG_PLACE_NAME).orEmpty()
        val initialRating = arguments?.getDouble(ARG_INITIAL_RATING) ?: 0.0
        val initialText = arguments?.getString(ARG_INITIAL_TEXT).orEmpty()
        val isEditMode = existingReviewId != null

        binding.userName.text = placeName
        binding.reviewRating.rating = initialRating.toFloat()
        binding.reviewContent.setText(initialText)
        binding.menuButton.visibility = View.GONE
        binding.writeReviewButton.text = if (isEditMode) "리뷰 수정하기" else "리뷰 작성하기"
        
        binding.reviewContent.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.scrollView.postDelayed({
                    binding.scrollView.fullScroll(android.view.View.FOCUS_DOWN)
                }, 100)
            }
        }
        
        updateButtonState(rating = initialRating.toFloat(), text = initialText)

        binding.reviewRating.setOnRatingBarChangeListener { _, rating, _ ->
            updateButtonState(rating = rating, text = binding.reviewContent.text?.toString()?.trim().orEmpty())
        }
        
        binding.reviewContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val rating = binding.reviewRating.rating
                val text = s?.toString()?.trim().orEmpty()
                updateButtonState(rating = rating, text = text)
            }
        })

        binding.writeReviewButton.setOnClickListener {
            val rating = binding.reviewRating.rating.toDouble()
            val text = binding.reviewContent.text?.toString()?.trim().orEmpty()
            if (rating <= 0.0 || text.isBlank()) {
                Toast.makeText(requireContext(), "별점과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val currentReviewId = existingReviewId
            val resultReviewId = if (isEditMode && currentReviewId != null && currentReviewId > 0) {
                currentReviewId
            } else {
                -1L
            }
            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                bundleOf(
                    RESULT_RATING to rating,
                    RESULT_TEXT to text,
                    RESULT_REVIEW_ID to resultReviewId
                )
            )
            dismiss()
        }
    }
    
    private fun updateButtonState(rating: Float, text: String) {
        val isValid = rating > 0 && text.isNotBlank()
        binding.writeReviewButton.isEnabled = isValid
        binding.writeReviewButton.alpha = if (isValid) 1.0f else 0.5f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_PLACE_NAME = "arg_place_name"
        private const val ARG_REVIEW_ID = "arg_review_id"
        private const val ARG_INITIAL_RATING = "arg_initial_rating"
        private const val ARG_INITIAL_TEXT = "arg_initial_text"

        const val RESULT_KEY = "result_review_editor"
        const val RESULT_RATING = "result_rating"
        const val RESULT_TEXT = "result_text"
        const val RESULT_REVIEW_ID = "result_review_id"

        fun newInstance(
            placeName: String?,
            reviewId: Long? = null,
            initialRating: Double = 0.0,
            initialText: String? = null
        ): ReviewEditFragment {
            return ReviewEditFragment().apply {
                arguments = bundleOf(
                    ARG_PLACE_NAME to placeName,
                    ARG_REVIEW_ID to (reviewId ?: 0L),
                    ARG_INITIAL_RATING to initialRating,
                    ARG_INITIAL_TEXT to initialText
                )
            }
        }
    }
}

