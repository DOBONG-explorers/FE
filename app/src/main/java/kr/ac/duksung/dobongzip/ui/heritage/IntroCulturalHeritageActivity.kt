package kr.ac.duksung.dobongzip.ui.heritage

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider

class IntroCulturalHeritageActivity : AppCompatActivity() {

    private lateinit var ivHeritage: ImageView
    private lateinit var tvHeritageName: TextView
    private lateinit var tvHeritageSubtitle: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvDesignationNumber: TextView
    private lateinit var tvDesignationDate: TextView
    private lateinit var tvPhoneNumber: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cardContainer: com.google.android.material.card.MaterialCardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.intro_cultural_heritage_activity)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        ivHeritage = findViewById(R.id.ivHeritage)
        tvHeritageName = findViewById(R.id.tvHeritageName)
        tvHeritageSubtitle = findViewById(R.id.tvHeritageSubtitle)
        tvDescription = findViewById(R.id.tvDescription)
        tvName = findViewById(R.id.tvName)
        tvAddress = findViewById(R.id.tvAddress)
        tvDesignationNumber = findViewById(R.id.tvDesignationNumber)
        tvDesignationDate = findViewById(R.id.tvDesignationDate)
        tvPhoneNumber = findViewById(R.id.tvPhoneNumber)
        progressBar = findViewById(R.id.progressBar)
        cardContainer = findViewById(R.id.cardContainer)

        val heritageId = intent.getStringExtra("heritageId")
        if (heritageId != null) {
            loadHeritageDetail(heritageId)
        }
    }

    private fun cleanText(raw: String?): String {
        val trimmed = raw?.replace("\u00A0", " ")?.trim().orEmpty()
        if (trimmed.isBlank() || trimmed.equals("null", ignoreCase = true)) return ""
        return HtmlCompat.fromHtml(trimmed, HtmlCompat.FROM_HTML_MODE_LEGACY).toString().trim()
    }

    private fun loadHeritageDetail(id: String) {
        progressBar.visibility = View.VISIBLE
        cardContainer.visibility = View.GONE
        
        android.util.Log.d("Heritage", "Loading detail for id: $id")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitProvider.heritageApi.getHeritageDetail(id)
                android.util.Log.d("Heritage", "Response success: ${response.success}, data: ${response.data?.SHD_NM}")
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    cardContainer.visibility = View.VISIBLE
                    
                    if (response.success && response.data != null) {
                        val detail = response.data
                        
                        val name = cleanText(detail.SHD_NM)
                        val subtitle = cleanText(detail.VA_F5)
                        val description = cleanText(detail.description ?: detail.VA_F11)
                        val address = cleanText(detail.CO_F2)
                        val designationNumber = cleanText(detail.VA_F2)
                        val designationDate = cleanText(detail.VA_F3)
                        val phoneNumber = cleanText(detail.CO_F3)
                        
                        tvHeritageName.text = name.ifBlank { "정보 없음" }
                        tvHeritageSubtitle.text = subtitle.ifBlank { "" }
                        tvDescription.text = description.ifBlank { "정보 없음" }
                        tvName.text = name.ifBlank { "정보 없음" }
                        tvAddress.text = address.ifBlank { "정보 없음" }
                        tvDesignationNumber.text = designationNumber.ifBlank { "정보 없음" }
                        tvDesignationDate.text = designationDate.ifBlank { "정보 없음" }
                        tvPhoneNumber.text = phoneNumber.ifBlank { "정보 없음" }

                        if (!detail.IMAGE_URL.isNullOrBlank()) {
                            android.util.Log.d("Heritage", "Loading image: ${detail.IMAGE_URL}")
                            Glide.with(this@IntroCulturalHeritageActivity)
                                .load(detail.IMAGE_URL)
                                .centerCrop()
                                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                                .into(ivHeritage)
                        } else {
                            android.util.Log.w("Heritage", "IMAGE_URL is null or blank")
                            ivHeritage.setImageResource(R.drawable.placeholder)
                        }
                    } else {
                        android.util.Log.e("Heritage", "Response not successful or data is null")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    cardContainer.visibility = View.VISIBLE
                }
                e.printStackTrace()
            }
        }
    }
}

