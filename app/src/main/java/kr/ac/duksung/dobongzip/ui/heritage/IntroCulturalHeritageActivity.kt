package kr.ac.duksung.dobongzip.ui.heritage

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
                        
                        android.util.Log.d("Heritage", "VA_F11 raw: \"${detail.VA_F11}\", length: ${detail.VA_F11?.length}")
                        android.util.Log.d("Heritage", "IMAGE_URL: ${detail.IMAGE_URL?.take(50)}...")
                        
                        val name = detail.SHD_NM?.takeIf { it.isNotBlank() } ?: ""
                        val subtitle = detail.VA_F5?.takeIf { it.isNotBlank() } ?: ""
                        val description = detail.VA_F11?.takeIf { it.isNotBlank() } ?: ""
                        val address = detail.CO_F2?.takeIf { it.isNotBlank() } ?: ""
                        val designationNumber = detail.VA_F2?.takeIf { it.isNotBlank() } ?: ""
                        val designationDate = detail.VA_F3?.takeIf { it.isNotBlank() } ?: ""
                        val phoneNumber = detail.CO_F3?.takeIf { it.isNotBlank() } ?: ""
                        
                        android.util.Log.d("Heritage", "Processed - name: $name, address: $address, description: ${description.take(50)}...")
                        
                        tvHeritageName.text = name
                        tvHeritageSubtitle.text = subtitle
                        tvDescription.text = if (description.isNotBlank()) description else "정보 없음"
                        tvName.text = "이름: $name"
                        tvAddress.text = "주소: $address"
                        tvDesignationNumber.text = "지정번호: $designationNumber"
                        tvDesignationDate.text = "지정일: $designationDate"
                        tvPhoneNumber.text = "전화번호: $phoneNumber"

                        if (!detail.IMAGE_URL.isNullOrBlank()) {
                            android.util.Log.d("Heritage", "Loading image: ${detail.IMAGE_URL}")
                            Glide.with(this@IntroCulturalHeritageActivity)
                                .load(detail.IMAGE_URL)
                                .centerCrop()
                                .apply(RequestOptions.bitmapTransform(RoundedCorners(20)))
                                .into(ivHeritage)
                        } else {
                            android.util.Log.w("Heritage", "IMAGE_URL is null or blank")
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

