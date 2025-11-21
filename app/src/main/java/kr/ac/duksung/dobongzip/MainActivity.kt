package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.NavigationUI
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import kr.ac.duksung.dobongzip.databinding.ActivityMainBinding
import com.kakao.sdk.common.util.Utility
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import android.view.View
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.ui.threed.ThreeDActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.data.models.LikeCardDto
import kotlin.math.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var originalNavElevation: Float = 0f
    private var placeSheetBehavior: BottomSheetBehavior<MaterialCardView>? = null
    private var likedPlaceIds: Set<String> = emptySet()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // 기본 네비게이션 연결
        navView.setupWithNavController(navController)
        
        loadLikedPlaces()
        
        // 네비게이션 변경 시 바텀시트 숨기기 및 배경색 변경
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id != R.id.mapFragment) {
                hidePlaceSheet()
            }
            
            if (destination.id == R.id.commend_place_fragment) {
                binding.root.setBackgroundColor(0xFF2d85cd.toInt())
            } else {
                binding.root.setBackgroundColor(0xFFFFFFFF.toInt())
            }
        }

        // 카카오 키해시 로그
        val keyHash = Utility.getKeyHash(this)
        Log.e("KAKAO_RELEASE_KEY_HASH", keyHash)

        //  바텀 네비 클릭 시 로그인 여부 체크
        navView.setOnItemSelectedListener { item ->
            val isLoggedIn = TokenHolder.isLoggedIn

            // 마이페이지 / 좋아요(알림) → 회원 전용으로 가정
            val isMemberOnlyDestination = when (item.itemId) {
                R.id.navigation_mypage,
                R.id.navigation_notifications -> true
                else -> false
            }

            if (!isLoggedIn && isMemberOnlyDestination) {
                //  비회원이 회원 전용 탭 클릭 → 로그인 안내 팝업
                showLoginRequiredDialog()
                return@setOnItemSelectedListener false
            }

            //  나머지는 원래대로 네비게이션 처리
            NavigationUI.onNavDestinationSelected(item, navController)
            true
        }

        // 외부에서 특정 탭으로 진입했을 때 처리
        intent.getIntExtra(EXTRA_TARGET_DESTINATION, -1).takeIf { it != -1 }?.let { targetItemId ->
            if (navView.selectedItemId != targetItemId) {
                navView.selectedItemId = targetItemId
            }
        }

        // 재선택 시 동작 막기
        navView.setOnItemReselectedListener {}

        originalNavElevation = navView.elevation
        
        setupPlaceSheet()
    }
    
    private fun setupPlaceSheet() {
        binding.placeSheet.visibility = View.GONE
        placeSheetBehavior = BottomSheetBehavior.from(binding.placeSheet).apply {
            isDraggable = true
            isHideable = true
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_HIDDEN
            
            addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                        binding.placeSheet.post {
                            binding.placeSheet.visibility = View.GONE
                        }
                    }
                }
                
                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                    if (slideOffset < -0.5f && state != BottomSheetBehavior.STATE_HIDDEN) {
                        state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }
            })
        }
    }
    
    fun showPlaceSheet(place: PlaceDto) {
        currentPlace = place
        binding.placeSheet.visibility = View.VISIBLE
        placeSheetBehavior?.state = BottomSheetBehavior.STATE_EXPANDED
        
        binding.txtPlaceName.text = place.name
        binding.txtPhone.text = place.phone ?: "전화번호 없음"
        
        val distanceText = place.distanceText ?: ""
        binding.txtDistance.text = distanceText
        
        val rating = place.rating
        if (rating != null && rating > 0) {
            binding.layoutRating.visibility = View.VISIBLE
            binding.txtRating.text = String.format("%.1f", rating)
            val reviews = place.reviewCount
            if (reviews != null && reviews > 0) {
                binding.txtReviewCount.visibility = View.VISIBLE
                binding.txtReviewCount.text = "($reviews)"
            } else {
                binding.txtReviewCount.visibility = View.GONE
            }
        } else {
            binding.layoutRating.visibility = View.GONE
        }
        
        binding.layoutRating.setOnClickListener {
            val intent = kr.ac.duksung.dobongzip.ui.review.ReviewActivity.createIntent(
                this,
                place.placeId,
                place.name
            )
            startActivity(intent)
        }
        
        if (!place.imageUrl.isNullOrBlank()) {
            Glide.with(this).load(place.imageUrl).into(binding.imgPlace)
        } else {
            binding.imgPlace.setImageResource(kr.ac.duksung.dobongzip.R.drawable.placeholder)
        }
        
        val isPrimary = isPrimaryPlace(place)
        val show3DButton = isPrimary
        
        binding.btnView3D.visibility = if (show3DButton) View.VISIBLE else View.GONE
        if (show3DButton) {
            binding.btnView3D.setOnClickListener {
                val intent = ThreeDActivity.createIntent(
                    context = this,
                    placeId = place.placeId,
                    placeName = place.name,
                    latitude = place.latitude,
                    longitude = place.longitude,
                    address = place.address,
                    description = place.description,
                    openingHours = place.openingHours?.let { ArrayList(it) },
                    priceLevel = place.priceLevel,
                    rating = place.rating,
                    reviewCount = place.reviewCount,
                    phone = place.phone
                )
                startActivity(intent)
            }
        }
        
        checkAndUpdateLikeStatus(place)
        
        binding.imgHeart.setOnClickListener {
            toggleLike(place)
        }
    }
    
    fun hidePlaceSheet() {
        placeSheetBehavior?.state = BottomSheetBehavior.STATE_HIDDEN
        binding.placeSheet.visibility = View.GONE
        currentPlace = null
    }
    
    private var currentPlace: PlaceDto? = null
    
    private fun isPrimaryPlace(place: PlaceDto): Boolean {
        val threeDPlacesList = listOf(
            Triple("쌍둥이 전망대", 37.6738502, 127.0291849),
            Triple("원당샘공원", 37.6607694, 127.0219571),
            Triple("원당한옥마을도서관", 37.660552, 127.0215044),
            Triple("둘리뮤지엄", 37.6522772, 127.0275989),
            Triple("원썸35카페", 37.66114, 127.0213),
            Triple("서울 창업허브", 37.6553721, 127.0480068),
            Triple("도봉집", 0.0, 0.0)
        )
        return threeDPlacesList.any { (name, lat, lng) ->
            if (lat == 0.0 && lng == 0.0) {
                place.name == name
            } else {
                kotlin.math.abs(place.latitude - lat) < 0.001 && 
                kotlin.math.abs(place.longitude - lng) < 0.001 &&
                (place.name == name || place.name.contains(name, ignoreCase = true))
            }
        }
    }
    
    private fun hasThreeDPlace(place: PlaceDto): Boolean {
        return !place.mapsUrl.isNullOrBlank() || isPrimaryPlace(place)
    }
    
    private fun loadLikedPlaces() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitProvider.placeLikeApi.getMyLikes(size = 100, order = "latest")
                if (response.success && response.data != null) {
                    likedPlaceIds = response.data.mapNotNull { it.placeId }.toSet()
                    android.util.Log.d("MainActivity", "좋아요 목록 로드 완료: ${likedPlaceIds.size}개")
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "좋아요 목록 로드 실패: ${e.message}", e)
            }
        }
    }
    
    private fun checkAndUpdateLikeStatus(place: PlaceDto) {
        if (place.placeId.startsWith("dummy_")) {
            place.isLiked = likedPlaceIds.contains(place.placeId)
            updateHeartIcon(place.isLiked)
            return
        }
        
        val isLiked = likedPlaceIds.contains(place.placeId)
        place.isLiked = isLiked
        updateHeartIcon(isLiked)
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitProvider.placeLikeApi.getMyLikes(size = 100, order = "latest")
                if (response.success && response.data != null) {
                    val newLikedPlaceIds = response.data.mapNotNull { it.placeId }.toSet()
                    likedPlaceIds = newLikedPlaceIds
                    val updatedIsLiked = newLikedPlaceIds.contains(place.placeId)
                    
                    CoroutineScope(Dispatchers.Main).launch {
                        place.isLiked = updatedIsLiked
                        if (currentPlace?.placeId == place.placeId) {
                            updateHeartIcon(updatedIsLiked)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "좋아요 상태 확인 실패: ${e.message}", e)
            }
        }
    }
    
    private fun updateHeartIcon(isLiked: Boolean) {
        if (binding.placeSheet.visibility == View.VISIBLE && currentPlace != null) {
            binding.imgHeart.setImageResource(
                if (isLiked) kr.ac.duksung.dobongzip.R.drawable.love_fill else kr.ac.duksung.dobongzip.R.drawable.love
            )
        }
    }
    
    private fun toggleLike(place: PlaceDto) {
        if (place.placeId.startsWith("dummy_")) {
            place.isLiked = !place.isLiked
            if (place.isLiked) {
                likedPlaceIds = likedPlaceIds + place.placeId
            } else {
                likedPlaceIds = likedPlaceIds - place.placeId
            }
            updateHeartIcon(place.isLiked)
            return
        }
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val response = if (place.isLiked) {
                    RetrofitProvider.placeLikeApi.unlike(place.placeId)
                } else {
                    RetrofitProvider.placeLikeApi.like(place.placeId)
                }
                if (response.success) {
                    place.isLiked = !place.isLiked
                    if (place.isLiked) {
                        likedPlaceIds = likedPlaceIds + place.placeId
                    } else {
                        likedPlaceIds = likedPlaceIds - place.placeId
                    }
                    updateHeartIcon(place.isLiked)
                } else {
                    Toast.makeText(this@MainActivity, response.message ?: "좋아요 처리에 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "좋아요 처리 실패: ${e.message}", e)
                Toast.makeText(this@MainActivity, "좋아요 처리 중 오류가 발생했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLoginRequiredDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_login_required, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 버튼 연결
        val btnCancel = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnLogin = dialogView.findViewById<TextView>(R.id.btnLogin)

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnLogin.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, LoginActivity::class.java))
        }

        dialog.show()
    }


    fun enableMapFragmentLayout() {
        val containerView = binding.navHostFragment
        val navView: BottomNavigationView = binding.navView



        val params =
            containerView.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params?.let {
            it.bottomToBottom =
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            it.bottomToTop = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            containerView.layoutParams = it
            containerView.requestLayout()
        }

        navView.elevation = 16f
        navView.bringToFront()
    }

    fun restoreNormalLayout() {
        val containerView = binding.navHostFragment
        val navView = binding.navView

        val params =
            containerView.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params?.let {
            it.bottomToBottom =
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            it.bottomToTop = R.id.nav_view
            containerView.layoutParams = it
            containerView.requestLayout()
        }

        navView.elevation = originalNavElevation
    }

    fun getBottomNavHeight(): Int = binding.navView.height

    companion object {
        const val EXTRA_TARGET_DESTINATION = "extra_target_destination"
    }
}
