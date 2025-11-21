package kr.ac.duksung.dobongzip.ui.review

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.commit
import kr.ac.duksung.dobongzip.databinding.ReviewActivityBinding

class ReviewActivity : AppCompatActivity() {

    private lateinit var binding: ReviewActivityBinding
    private var placeId: String? = null
    private var placeName: String? = null
    
    fun updateWriteButtonVisibility(visible: Boolean) {
        binding.writeReviewButton.visibility = if (visible) android.view.View.VISIBLE else android.view.View.GONE
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ReviewActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        if (placeId.isNullOrBlank()) {
            finish()
            return
        }
        placeName = intent.getStringExtra(EXTRA_PLACE_NAME)

        binding.titleText.text = placeName ?: binding.titleText.text
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
        binding.writeReviewButton.setOnClickListener {
            supportFragmentManager.setFragmentResult(
                PlaceReviewFragment.REQUEST_OPEN_EDITOR,
                bundleOf()
            )
        }

        if (supportFragmentManager.findFragmentById(binding.fragmentContainer.id) == null) {
            supportFragmentManager.commit {
                replace(
                    binding.fragmentContainer.id,
                    PlaceReviewFragment.newInstance(placeId!!, placeName),
                    FRAGMENT_TAG
                )
            }
        }
    }

    companion object {
        private const val EXTRA_PLACE_ID = "extra_place_id"
        private const val EXTRA_PLACE_NAME = "extra_place_name"
        private const val FRAGMENT_TAG = "review_fragment"

        fun createIntent(context: Context, placeId: String, placeName: String?): Intent {
            return Intent(context, ReviewActivity::class.java).apply {
                putExtra(EXTRA_PLACE_ID, placeId)
                putExtra(EXTRA_PLACE_NAME, placeName)
            }
        }
    }
}

