package kr.ac.duksung.dobongzip.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository
import kr.ac.duksung.dobongzip.databinding.FragmentMapBinding
import kr.ac.duksung.dobongzip.ui.threed.ThreeDActivity
import kotlin.math.pow
import kotlin.math.sin
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import de.hdodenhof.circleimageview.CircleImageView // CircleImageView import 추가
import kr.ac.duksung.dobongzip.LoginActivity
import kr.ac.duksung.dobongzip.MainActivity
import kr.ac.duksung.dobongzip.data.auth.AuthSession
import retrofit2.Retrofit
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.converter.gson.GsonConverterFactory
import kr.ac.duksung.dobongzip.data.network.PlaceLikeApi
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.databinding.ViewMarkerBinding

class MapFragment : Fragment(R.layout.fragment_map) {

    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!

    private var toast: Toast? = null

    // Kakao Map
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    private var placesLayer: LodLabelLayer? = null
    private var myLayer: LabelLayer? = null
    private var debugLayer: LabelLayer? = null
    private val placeLabels = mutableListOf<LodLabel>()
    private var myLabel: Label? = null

    private var focusPlaceId: String? = null
    private var focusLatLng: Pair<Double, Double>? = null
    private val threeDPlaceIds = mutableSetOf<String>()
    private val markerStyleCache = mutableMapOf<String, LabelStyle>()
    private var recommendedPlace: PlaceDto? = null

    private lateinit var sheetBehavior: BottomSheetBehavior<MaterialCardView>

    private val dobongCenter = LatLng.from(37.668, 127.047)

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }
    private val settingsClient by lazy { LocationServices.getSettingsClient(requireContext()) }
    private val REQUEST_RESOLVE_GPS = 1001
    private val MY_LAYER_ID = "my_layer"
    private val DEBUG_LAYER_ID = "debug_layer"
    private val repository = PlacesRepository()
    private var currentPlaces: List<PlaceDto> = emptyList()

    private val locationPerms = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val requestLocationPerms = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) ensureLocationSettings { moveToMyLocation() }
        else {
            val context = context ?: return@registerForActivityResult
            Toast.makeText(context, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 저장된 상태 복원 로직
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentMapBinding.inflate(inflater, container, false)

        // 바텀시트 세팅
        sheetBehavior = BottomSheetBehavior.from(b.placeSheet).apply {
            isDraggable = true
            isHideable = true
            skipCollapsed = false
            state = BottomSheetBehavior.STATE_HIDDEN
        }
        b.placeSheet.apply {
            bringToFront()
            alpha = 1f
            isVisible = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(b.placeSheet) { view, insets ->
            adjustPlaceSheetBottomMargin()
            insets
        }

        return b.root
    }

    // 성공 또는 실패 메시지를 UI 스레드에서 실행하도록 변경
    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
           // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

    // Map init
    private fun initMap() {
        mapView = MapView(requireContext())
        b.mapContainer.addView(mapView)

        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(e: Exception) {
                    Log.e("MapFragment", "Map init error: ${e.message}", e)
                    val context = context ?: return
                    Toast.makeText(context, "지도 초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map

                    map.setOnLodLabelClickListener { _, _, lodLabel ->
                        val placeId = lodLabel.tag as? String
                        if (placeId != null) {
                            val place = currentPlaces.firstOrNull { it.placeId == placeId }
                            if (place != null) {
                                showPlaceSheet(place)
                                true
                            } else {
                                Log.e("MapFragment", "LodLabel click: place not found for id=$placeId")
                                false
                            }
                        } else {
                            Log.e("MapFragment", "LodLabel click: tag is not String, tag=${lodLabel.tag}")
                            false
                        }
                    }


                    // 레이어 생성/획득
                    val context = context ?: return
                    val lm = map.labelManager ?: run {
                        //Toast.makeText(context, "레이어 매니저 초기화 실패", Toast.LENGTH_LONG).show()
                        return
                    }

                    // LodLabel 전용 레이어
                    placesLayer = lm.lodLayer?.apply { isClickable = true }
                    myLayer = lm.getLayer(MY_LAYER_ID) ?: lm.addLayer(LabelLayerOptions.from(MY_LAYER_ID))
                    debugLayer = lm.getLayer(DEBUG_LAYER_ID) ?: lm.addLayer(LabelLayerOptions.from(DEBUG_LAYER_ID))

                    // 카메라 설정
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(dobongCenter))
                    map.moveCamera(CameraUpdateFactory.zoomTo(15))

                    // 위치 정보 및 장소 로드
                    ensureLocationAndMove()
                    val initialCenter = focusLatLng?.let { LatLng.from(it.first, it.second) } ?: dobongCenter
                    loadPlacesAndRender(initialCenter, limit = 30)
                }

                override fun getPosition(): LatLng = dobongCenter
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    private fun toggleLike(place: PlaceDto) {
        if (place.isLiked) {
            // 현재 좋아요 상태라면 → 해제 요청
            removeFromLikedPlaces(place)
        } else {
            // 현재 좋아요가 아니라면 → 추가 요청
            addToLikedPlaces(place)
        }
    }


    private fun addToLikedPlaces(place: PlaceDto) {
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.placeLikeApi.like(place.placeId)
                if (response.success) {
                    // ✅ 서버가 성공이라 한 경우에만 상태 변경
                    place.isLiked = true
                    b.imgHeart.setImageResource(R.drawable.love_fill)
                    showToast("${place.name} 장소가 좋아요 목록에 추가되었습니다!")
                } else {
                    val msg = response.errorMessage
                        ?: (response.data?.message)
                        ?: "알 수 없는 오류"
                    showToast("좋아요 추가 실패: $msg")
                }
            } catch (e: Exception) {
                showToast("좋아요 추가 실패: ${e.message}")
            }
        }
    }


    private fun removeFromLikedPlaces(place: PlaceDto) {
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.placeLikeApi.unlike(place.placeId)
                if (response.success) {
                    // ✅ 서버에서 진짜 좋아요 삭제 성공했을 때만 해제
                    place.isLiked = false
                    b.imgHeart.setImageResource(R.drawable.love)
                    showToast("${place.name} 장소가 좋아요 목록에서 제거되었습니다!")
                } else {
                    val msg = response.errorMessage
                        ?: (response.data?.message)
                        ?: "알 수 없는 오류"
                    showToast("좋아요 취소 실패: $msg")
                }
            } catch (e: Exception) {
                showToast("좋아요 취소 실패: ${e.message}")
            }
        }
    }

    private fun loadLikedPlaces() {
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.placeLikeApi.getMyLikes(size = 30, order = "latest")
                if (response.success) {
                    val likedPlaces = response.data as List<PlaceDto>  // 응답 데이터 처리
                    // 좋아요 목록을 RecyclerView 등에 표시
                    showToast("${likedPlaces.size}개의 장소를 좋아요 목록에 추가했습니다.")
                } else {
                    showToast("좋아요 목록을 가져오는 데 실패했습니다: ${response.message}")
                }
            } catch (e: Exception) {
                showToast("좋아요 목록을 가져오는 데 실패했습니다: ${e.message}")
            }
        }
    }

    // Places load & render
    private fun loadPlacesAndRender(center: LatLng, limit: Int = 30) {
        val map = kakaoMap ?: return
        lifecycleScope.launch {
            try {
                val places = PlacesRepository().fetchPlaces(center.latitude, center.longitude, limit)
                val combined = mutableListOf<PlaceDto>().apply {
                    addAll(places)
                    recommendedPlace?.let { rec ->
                        if (none { it.placeId == rec.placeId }) add(rec)
                    }
                }
                //현재리스트 저장
                currentPlaces = combined

                renderPlaceMarkers(map, combined)
                focusOnTarget(combined)
                val context = context ?: return@launch
                if (combined.isEmpty()) {
                   // Toast.makeText(context, "서버 응답 성공, 하지만 0건", Toast.LENGTH_SHORT).show()
                    addDebugLabel(center, "0 places")
                } else {
                   // Toast.makeText(context, "명소 ${combined.size}개 표시", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PLACES", "API 실패: ${e.message}", e)
                val context = context ?: return@launch
                //Toast.makeText(context, "장소 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                addDebugLabel(center, "API FAIL")
            }
        }
    }

    // LodLabel로 마커 렌더링
    private suspend fun renderPlaceMarkers(map: KakaoMap, places: List<PlaceDto>) {
        val layer = placesLayer ?: return
        layer.isClickable = true
        layer.isVisible = true

        placeLabels.forEach { it.remove() }
        placeLabels.clear()

        places.forEachIndexed { idx, p ->
            val position = LatLng.from(p.latitude, p.longitude)
            val title = p.name.takeIf { it.isNotBlank() } ?: "이름 없음"
            val variant = if (hasThreeDPlace(p)) MarkerVariant.THREE_D else MarkerVariant.DEFAULT
            val style = getMarkerStyle(p, variant)

            val options = LabelOptions.from("place_$idx", position)
                .setStyles(style)
                .setTexts(LabelTextBuilder().setTexts(title))
                .setClickable(true)
                .setTag(p.placeId) //스트링만 넣기

            val lodLabel = layer.addLodLabel(options)
            lodLabel?.let { placeLabels += it }
        }
    }

    private fun focusOnTarget(places: List<PlaceDto>) {
        val map = kakaoMap ?: return
        val target = focusPlaceId?.let { id -> places.firstOrNull { it.placeId == id } }
        val targetLatLng = target?.let { LatLng.from(it.latitude, it.longitude) }
            ?: focusLatLng?.let { LatLng.from(it.first, it.second) }
            ?: recommendedPlace?.let { LatLng.from(it.latitude, it.longitude) }

        if (targetLatLng != null) {
            map.moveCamera(CameraUpdateFactory.newCenterPosition(targetLatLng))
            map.moveCamera(CameraUpdateFactory.zoomTo(17))
            (target ?: recommendedPlace)?.let { showPlaceSheet(it) }
            focusPlaceId = null
            focusLatLng = null
        }
    }

    private fun hasThreeDPlace(place: PlaceDto): Boolean {
        if (threeDPlaceIds.contains(place.placeId)) return true
        val url = place.mapsUrl
        val matches = url?.contains(THREE_D_WEB_HOST, ignoreCase = true) == true
        if (matches) threeDPlaceIds.add(place.placeId)
        return matches
    }


    private fun adjustPlaceSheetBottomMargin() {
        val navHeightRaw = (activity as? MainActivity)?.getBottomNavHeight() ?: 0
        val fallback = resources.getDimensionPixelSize(R.dimen.bottom_nav_height)
        val navHeight = if (navHeightRaw > 0) navHeightRaw else fallback
        val params = b.placeSheet.layoutParams as? ViewGroup.MarginLayoutParams ?: return
        val desired = navHeight + resources.getDimensionPixelSize(R.dimen.map_sheet_bottom_margin_extra)
        if (params.bottomMargin != desired) {
            params.bottomMargin = desired
            b.placeSheet.layoutParams = params
        }
    }

    // BottomSheet: place detail
    private fun showPlaceSheet(place: PlaceDto) {
        val img = b.imgPlace
        val name = b.txtPlaceName
        val dist = b.txtDistance
        val phone = b.txtPhone
        val btn3d = b.btnView3D
        val ratingLayout = b.layoutRating
        val ratingText = b.txtRating
        val reviewText = b.txtReviewCount
        val heartIcon = b.imgHeart

        adjustPlaceSheetBottomMargin()

        name.text = place.name
        phone.text = place.phone ?: "전화번호 없음"

        val cam = kakaoMap?.cameraPosition?.position
        val distanceText = place.distanceText ?: cam?.let {
            val km = haversineKm(it.latitude, it.longitude, place.latitude, place.longitude)
            String.format("내 위치로부터 %.1fkm", km)
        } ?: ""
        dist.text = distanceText

        val rating = place.rating
        if (rating != null && rating > 0) {
            ratingLayout.isVisible = true
            ratingText.text = String.format("%.1f", rating)
            val reviews = place.reviewCount
            if (reviews != null && reviews > 0) {
                reviewText.isVisible = true
                reviewText.text = "($reviews)"
            } else {
                reviewText.isVisible = false
            }
        } else {
            ratingLayout.isVisible = false
        }

        if (!place.imageUrl.isNullOrBlank()) Glide.with(this).load(place.imageUrl).into(img)
        else img.setImageResource(R.drawable.placeholder)

        val hasThreeD = hasThreeDPlace(place)
        btn3d.isVisible = hasThreeD
        if (hasThreeD) {
            btn3d.setOnClickListener {
                val ctx = requireContext()
                val intent = ThreeDActivity.createIntent(
                    context = ctx,
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
        } else {
            btn3d.setOnClickListener(null)
        }

        // ✅ 현재 좋아요 상태에 따라 하트 모양 세팅
        heartIcon.setImageResource(
            if (place.isLiked) R.drawable.love_fill else R.drawable.love
        )

        // ✅ 하트 클릭 시 좋아요 토글
        heartIcon.setOnClickListener {
            toggleLike(place)
        }


        b.placeSheet.isVisible = true
        sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    // My location marker (일반 Label)
    private fun addMyLocationMarker(here: LatLng) {
        val layer = myLayer ?: return
        layer.isClickable = false
        layer.isVisible = true
        myLabel?.remove()

        val opt = LabelOptions.from(here)
            .setStyles(myPinStyle)
            .setTexts(LabelTextBuilder().setTexts("내 위치"))
            .setClickable(false)

        myLabel = layer.addLabel(opt)
    }

    // Debug label (일반 Label)
    private fun addDebugLabel(position: LatLng, text: String) {
        val layer = debugLayer ?: return
        layer.isClickable = false
        layer.isVisible = true
        layer.removeAll()

        val opt = LabelOptions.from(position)
            .setStyles(debugPinStyle)
            .setTexts(LabelTextBuilder().setTexts(text))
            .setClickable(false)

        layer.addLabel(opt)
    }

    // Distance
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                Math.cos(Math.toRadians(lat1)) *
                Math.cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    // Location helpers
    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureLocationAndMove() {
        if (hasLocationPermission()) {
            ensureLocationSettings { moveToMyLocation() }
        } else {
            requestLocationPerms.launch(locationPerms)
        }
    }

    private fun ensureLocationSettings(onReady: () -> Unit) {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setWaitForAccurateLocation(true)
            .build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(req)
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { e ->
                val context = context ?: return@addOnFailureListener
                val activity = activity ?: return@addOnFailureListener
                if (e is ResolvableApiException) {
                    try { e.startResolutionForResult(activity, REQUEST_RESOLVE_GPS) }
                    catch (_: IntentSender.SendIntentException) {
                        Toast.makeText(context, "위치 설정 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "위치 설정을 켜주세요(고정밀/GPS).", Toast.LENGTH_LONG).show()
                }
            }
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (!hasLocationPermission()) return
        locationClient.lastLocation.addOnSuccessListener { loc ->
            if (loc != null) centerMapTo(loc)
        }
    }

    private fun centerMapTo(location: Location) {
        val here = LatLng.from(location.latitude, location.longitude)
        val context = context ?: return
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.newCenterPosition(here))
            map.moveCamera(CameraUpdateFactory.zoomTo(16))
            Toast.makeText(context, "현 위치로 이동: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
            addMyLocationMarker(here)
        }
    }

    // Pin styles (dp sizing)
    private fun makePinStyleDp(targetHeightDp: Int, @ColorInt tint: Int? = null): LabelStyle {
        val dm = resources.displayMetrics
        val hPx = (targetHeightDp * dm.density + 0.5f).toInt()
        val src = BitmapFactory.decodeResource(resources, R.drawable.pin)
        val ratio = src.width.toFloat() / src.height
        val wPx = (hPx * ratio).toInt()
        val scaled: Bitmap = Bitmap.createScaledBitmap(src, wPx, hPx, true)
        if (scaled != src) src.recycle()
        val finalBitmap = tint?.let {
            val tinted = scaled.tint(it)
            if (tinted !== scaled) scaled.recycle()
            tinted
        } ?: scaled
        return LabelStyle.from(finalBitmap).setAnchorPoint(0.5f, 1.0f)
    }
    private val myPinStyle    by lazy { makePinStyleDp(20) }
    private val debugPinStyle by lazy { makePinStyleDp(16) }

    private fun Bitmap.tint(@ColorInt color: Int): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(this, 0f, 0f, paint)
        return result
    }

    private suspend fun getMarkerStyle(place: PlaceDto, variant: MarkerVariant): LabelStyle {
        val context = context ?: throw IllegalStateException("Fragment not attached")
        val key = "${variant.name}:${place.placeId}:${place.imageUrl ?: ""}"
        markerStyleCache[key]?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            buildMarkerBitmap(requireContext(), place, variant)
        }
        val style = LabelStyle.from(bitmap).setAnchorPoint(0.5f, 1f)
        markerStyleCache[key] = style
        return style
    }

    private fun buildMarkerBitmap(context: android.content.Context, place: PlaceDto, variant: MarkerVariant): Bitmap {
        val binding = ViewMarkerBinding.inflate(LayoutInflater.from(context))
        val outlineColor = ContextCompat.getColor(context, variant.strokeColorRes)
        val strokePx = (context.resources.displayMetrics.density * 2f).toInt().coerceAtLeast(1)

        (binding.markerCircle.background.mutate() as? GradientDrawable)?.apply {
            setColor(ContextCompat.getColor(context, android.R.color.white))
            setStroke(strokePx, outlineColor)
        }
        binding.markerTail.imageTintList = ColorStateList.valueOf(outlineColor)
        binding.txtBadge3d.isVisible = variant == MarkerVariant.THREE_D

        val thumbnail = loadMarkerThumbnail(context, place.imageUrl)
        if (thumbnail != null) {
            binding.markerImage.setImageBitmap(thumbnail)
        } else {
            binding.markerImage.setImageResource(R.drawable.placeholder)
        }

        val widthSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.getDimensionPixelSize(R.dimen.marker_total_width),
            View.MeasureSpec.EXACTLY
        )
        val heightSpec = View.MeasureSpec.makeMeasureSpec(
            context.resources.getDimensionPixelSize(R.dimen.marker_total_height),
            View.MeasureSpec.EXACTLY
        )
        binding.root.measure(widthSpec, heightSpec)
        val measuredWidth = binding.root.measuredWidth.takeIf { it > 0 }
            ?: context.resources.getDimensionPixelSize(R.dimen.marker_total_width)
        val measuredHeight = binding.root.measuredHeight.takeIf { it > 0 }
            ?: context.resources.getDimensionPixelSize(R.dimen.marker_total_height)
        binding.root.layout(0, 0, measuredWidth, measuredHeight)

        val bitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        binding.root.draw(canvas)
        return bitmap
    }

    private fun loadMarkerThumbnail(context: android.content.Context, imageUrl: String?): Bitmap? {
        if (imageUrl.isNullOrBlank()) return null
        return try {
            val future = Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .centerCrop()
                .submit(200, 200)
            try {
                future.get()
            } finally {
                Glide.with(context).clear(future)
            }
        } catch (e: Exception) {
            Log.w("MapFragment", "마커 이미지 로드 실패: ${e.message}")
            null
        }
    }

    private enum class MarkerVariant(@ColorRes val strokeColorRes: Int) {
        DEFAULT(R.color.marker_original),
        THREE_D(R.color.marker_outline_three_d)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        focusPlaceId?.let { outState.putString(ARG_FOCUS_PLACE_ID, it) }
        focusLatLng?.let {
            outState.putDouble(ARG_FOCUS_LAT, it.first)
            outState.putDouble(ARG_FOCUS_LNG, it.second)
        }
        if (threeDPlaceIds.isNotEmpty()) {
            outState.putStringArrayList(ARG_THREE_D_PLACE_IDS, ArrayList(threeDPlaceIds))
        }
    }

    // Lifecycle
    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.enableMapFragmentLayout()
        initMap()
    }

    override fun onPause() {
        if (this::mapView.isInitialized) mapView.pause()
        (activity as? MainActivity)?.restoreNormalLayout()
        super.onPause()
    }

    override fun onDestroyView() {
        (activity as? MainActivity)?.restoreNormalLayout()
        if (this::mapView.isInitialized) b.mapContainer.removeView(mapView)

        myLabel?.remove(); myLabel = null
        placeLabels.forEach { it.remove() }
        placeLabels.clear()
        markerStyleCache.clear()

        placesLayer?.removeAll()
        myLayer?.removeAll()
        debugLayer?.removeAll()

        kakaoMap = null
        _b = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_FOCUS_PLACE_ID = "focus_place_id"
        const val ARG_FOCUS_LAT = "focus_lat"
        const val ARG_FOCUS_LNG = "focus_lng"
        const val ARG_THREE_D_PLACE_IDS = "three_d_place_ids"
        const val ARG_RECOMMENDED_PLACE_JSON = "recommended_place_json"
        private const val THREE_D_WEB_HOST = "dobongvillage-5f531.web.app"
    }
}
