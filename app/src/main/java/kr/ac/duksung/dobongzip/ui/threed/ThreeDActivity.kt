package kr.ac.duksung.dobongzip.ui.threed

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.webkit.WebSettings
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.android.material.tabs.TabLayout
import kr.ac.duksung.dobongzip.MainActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.PlaceDetailDto
import kr.ac.duksung.dobongzip.databinding.ThreeDActivityBinding
import java.util.Locale

class ThreeDActivity : AppCompatActivity() {

    private val binding: ThreeDActivityBinding by lazy {
        ThreeDActivityBinding.inflate(layoutInflater)
    }

    private val viewModel: ThreeDViewModel by viewModels()

    private val tabContentViews = mutableListOf<View>()

    private var currentContent: PlaceContent = PlaceContent()
    private var currentWebLatLng: Pair<Double, Double>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        setupUi()

        val placeId = intent.getStringExtra(EXTRA_PLACE_ID)
        val placeName = intent.getStringExtra(EXTRA_PLACE_NAME)
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, Double.NaN).takeIf { !it.isNaN() }
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, Double.NaN).takeIf { !it.isNaN() }

        if (placeId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.message_missing_place), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentContent = PlaceContent(
            address = intent.getStringExtra(EXTRA_ADDRESS),
            description = intent.getStringExtra(EXTRA_DESCRIPTION),
            openingHours = intent.getStringArrayListExtra(EXTRA_OPENING_HOURS)?.filter { it.isNotBlank() },
            priceLevel = takeNullableIntExtra(EXTRA_PRICE_LEVEL),
            rating = intent.getDoubleExtra(EXTRA_RATING, Double.NaN).takeIf { !it.isNaN() },
            reviewCount = intent.getIntExtra(EXTRA_REVIEW_COUNT, -1).takeIf { it >= 0 },
            phone = intent.getStringExtra(EXTRA_PHONE),
            latitude = latitude,
            longitude = longitude
        )

        binding.titleOverlay.text = placeName.orEmpty()
        loadWebContent(latitude, longitude)
        renderTabs(currentContent)

        observeViewModel(placeId)
        viewModel.loadPlaceDetail(placeId)
    }

    override fun onDestroy() {
        binding.webview3d.apply {
            loadUrl("about:blank")
            stopLoading()
            destroy()
        }
        super.onDestroy()
    }

    private fun setupUi() {
        setupWebView()
        setupBottomNavigation()
        binding.backButton.setOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }

    private fun observeViewModel(placeId: String) {
        viewModel.detail.observe(this) { detail ->
            currentContent = currentContent.merge(detail)
            val lat = currentContent.latitude
            val lng = currentContent.longitude
            if (lat != null && lng != null) {
                val current = currentWebLatLng
                if (current == null || current.first != lat || current.second != lng) {
                    loadWebContent(lat, lng)
                }
            }
            renderTabs(currentContent)
        }

        viewModel.liked.observe(this) { liked ->
            updateHeartState(liked)
        }

        viewModel.error.observe(this) { message ->
            if (!message.isNullOrBlank()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                viewModel.consumeError()
            }
        }

        binding.btnHeart.setOnClickListener {
            viewModel.toggleLike(placeId)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(binding.webview3d.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_NO_CACHE
        }
        binding.webview3d.webChromeClient = android.webkit.WebChromeClient()
        binding.webview3d.webViewClient = android.webkit.WebViewClient()
    }

    private fun loadWebContent(latitude: Double?, longitude: Double?) {
        binding.webview3d.clearHistory()
        binding.webview3d.clearCache(true)
        val builder = Uri.parse(WEB_BASE_URL).buildUpon()
        if (latitude != null && longitude != null) {
            builder.appendQueryParameter("lat", latitude.toString())
                .appendQueryParameter("lng", longitude.toString())
            currentWebLatLng = latitude to longitude
        } else {
            currentWebLatLng = null
        }
        builder.appendQueryParameter("ts", System.currentTimeMillis().toString())
        binding.webview3d.loadUrl(builder.build().toString())
    }

    private fun renderTabs(content: PlaceContent) {
        val address = content.address
        val description = content.description
        val openingHours = content.openingHours?.takeIf { it.isNotEmpty() }
        val priceLevel = content.priceLevel
        val ratingInfo = content.rating?.let { rating ->
            val countText = content.reviewCount?.let { " (${it}건)" } ?: ""
            "평점 ${String.format(Locale.KOREA, "%.1f", rating)}$countText"
        }
        val phone = content.phone

        val tabs = listOf(
            TabSpec("장소 소개") {
                buildSection(
                    title = "장소 소개",
                    contents = buildList {
                        if (!address.isNullOrBlank()) add("주소: $address")
                        if (!phone.isNullOrBlank()) add("전화: $phone")
                        if (!description.isNullOrBlank()) add(description)
                        if (isEmpty()) add("장소 소개 정보가 없습니다.")
                    }
                )
            },
            TabSpec("이용 시간") {
                buildSection(
                    title = "이용 시간",
                    contents = openingHours?.takeIf { it.isNotEmpty() }
                        ?: listOf("이용 시간 정보가 없습니다.")
                )
            },
            TabSpec("이용 금액") {
                val priceText = priceLevel?.let { mapPriceLevel(it) }
                    ?: "이용 금액 정보가 없습니다."
                buildSection(
                    title = "이용 금액",
                    contents = listOf(priceText)
                )
            },
            TabSpec("리뷰") {
                buildSection(
                    title = "리뷰",
                    contents = buildList {
                        ratingInfo?.let { add(it) }
                        add("리뷰 데이터는 준비 중입니다.")
                    }
                )
            }
        )

        binding.tabLayout.clearOnTabSelectedListeners()
        binding.tabLayout.removeAllTabs()
        binding.tabContentContainer.removeAllViews()
        tabContentViews.clear()

        tabs.forEachIndexed { index, tabSpec ->
            val tab = binding.tabLayout.newTab().setText(tabSpec.title)
            binding.tabLayout.addTab(tab, index == 0)

            val contentView = tabSpec.contentBuilder()
            contentView.isVisible = index == 0
            binding.tabContentContainer.addView(contentView)
            tabContentViews.add(contentView)
        }

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                updateTabVisibility(tab.position)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        updateTabVisibility(binding.tabLayout.selectedTabPosition.coerceAtLeast(0))
    }

    private fun updateTabVisibility(selectedIndex: Int) {
        tabContentViews.forEachIndexed { index, view ->
            view.isVisible = index == selectedIndex
        }
    }

    private fun updateHeartState(isLiked: Boolean) {
        binding.btnHeart.setImageResource(
            if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_empty
        )
        binding.btnHeart.contentDescription = if (isLiked) {
            getString(R.string.content_desc_unlike)
        } else {
            getString(R.string.content_desc_like)
        }
    }

    private fun buildSection(title: String, contents: List<String>): View {
        val context = this
        val sectionLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 0, 0, 24.dpToPx(context))
        }

        val titleView = TextView(context).apply {
            text = title
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
        }

        sectionLayout.addView(titleView)

        contents.forEach { text ->
            val bodyView = TextView(context).apply {
                this.text = text
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, android.R.color.black))
                setLineSpacing(4.dpToPx(context).toFloat(), 1f)
                setPadding(0, 8.dpToPx(context), 0, 0)
            }
            sectionLayout.addView(bodyView)
        }

        return sectionLayout
    }

    private fun mapPriceLevel(priceLevel: Int): String {
        return when (priceLevel) {
            0 -> "무료"
            1 -> "저렴"
            2 -> "보통"
            3 -> "조금 비쌈"
            4 -> "매우 비쌈"
            else -> "이용 금액 정보가 없습니다."
        }
    }

    private fun Int.dpToPx(context: Context): Int {
        val density = context.resources.displayMetrics.density
        return (this * density).toInt()
    }

    private fun takeNullableIntExtra(key: String): Int? {
        return if (intent.hasExtra(key)) intent.getIntExtra(key, Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE } else null
    }

    private fun setupBottomNavigation() {
        binding.navView.selectedItemId = R.id.navigation_home
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home,
                R.id.navigation_chat,
                R.id.navigation_notifications,
                R.id.navigation_mypage -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(MainActivity.EXTRA_TARGET_DESTINATION, item.itemId)
                    })
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private data class TabSpec(
        val title: String,
        val contentBuilder: () -> View
    )

    private data class PlaceContent(
        val address: String? = null,
        val description: String? = null,
        val openingHours: List<String>? = null,
        val priceLevel: Int? = null,
        val rating: Double? = null,
        val reviewCount: Int? = null,
        val phone: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    )

    private fun PlaceContent.merge(detail: PlaceDetailDto?): PlaceContent {
        if (detail == null) return this
        return PlaceContent(
            address = detail.address?.takeIf { it.isNotBlank() } ?: this.address,
            description = detail.description?.takeIf { it.isNotBlank() } ?: this.description,
            openingHours = detail.openingHours?.filter { it.isNotBlank() }?.takeIf { it.isNotEmpty() } ?: this.openingHours,
            priceLevel = detail.priceLevel ?: this.priceLevel,
            rating = detail.rating?.toDouble() ?: this.rating,
            reviewCount = detail.reviewCount ?: this.reviewCount,
            phone = detail.phone?.takeIf { it.isNotBlank() } ?: this.phone,
            latitude = detail.location?.latitude ?: this.latitude,
            longitude = detail.location?.longitude ?: this.longitude
        )
    }

    companion object {
        private const val WEB_BASE_URL = "https://dobongvillage-5f531.web.app"

        const val EXTRA_PLACE_ID = "extra_place_id"
        const val EXTRA_PLACE_NAME = "extra_place_name"
        const val EXTRA_LATITUDE = "extra_latitude"
        const val EXTRA_LONGITUDE = "extra_longitude"
        const val EXTRA_ADDRESS = "extra_address"
        const val EXTRA_DESCRIPTION = "extra_description"
        const val EXTRA_OPENING_HOURS = "extra_opening_hours"
        const val EXTRA_PRICE_LEVEL = "extra_price_level"
        const val EXTRA_RATING = "extra_rating"
        const val EXTRA_REVIEW_COUNT = "extra_review_count"
        const val EXTRA_PHONE = "extra_phone"

        fun createIntent(
            context: Context,
            placeId: String,
            placeName: String,
            latitude: Double,
            longitude: Double,
            address: String? = null,
            description: String? = null,
            openingHours: ArrayList<String>? = null,
            priceLevel: Int? = null,
            rating: Double? = null,
            reviewCount: Int? = null,
            phone: String? = null
        ): Intent {
            return Intent(context, ThreeDActivity::class.java).apply {
                putExtra(EXTRA_PLACE_ID, placeId)
                putExtra(EXTRA_PLACE_NAME, placeName)
                putExtra(EXTRA_LATITUDE, latitude)
                putExtra(EXTRA_LONGITUDE, longitude)
                address?.let { putExtra(EXTRA_ADDRESS, it) }
                description?.let { putExtra(EXTRA_DESCRIPTION, it) }
                openingHours?.let { putStringArrayListExtra(EXTRA_OPENING_HOURS, it) }
                priceLevel?.let { putExtra(EXTRA_PRICE_LEVEL, it) }
                rating?.let { putExtra(EXTRA_RATING, it) }
                reviewCount?.let { putExtra(EXTRA_REVIEW_COUNT, it) }
                phone?.let { putExtra(EXTRA_PHONE, it) }
            }
        }
    }
}

