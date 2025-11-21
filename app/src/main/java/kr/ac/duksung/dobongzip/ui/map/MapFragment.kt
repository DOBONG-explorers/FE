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
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import android.content.res.ColorStateList
import android.os.Handler
import android.os.Looper
import com.google.android.gms.common.api.ResolvableApiException
import de.hdodenhof.circleimageview.CircleImageView
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
import java.lang.Math.pow
import kotlin.math.pow
import com.google.gson.Gson

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
    private var isFirstLaunch = true

    private var focusPlaceId: String? = null
    private var focusLatLng: Pair<Double, Double>? = null
    private val threeDPlaceIds = mutableSetOf<String>()
    private val markerStyleCache = mutableMapOf<String, LabelStyle>()
    private var recommendedPlace: PlaceDto? = null

    private var placesLoaded = false

    private data class ThreeDPlaceInfo(
        val label: String,
        val filename: String,
        val latitude: Double,
        val longitude: Double
    )

    private val threeDPlacesList = listOf(
        ThreeDPlaceInfo("쌍둥이 전망대", "twintower.glb", 37.6738502, 127.0291849),
        ThreeDPlaceInfo("원당샘공원", "wondangsaem.glb", 37.6607694, 127.0219571),
        ThreeDPlaceInfo("원당한옥마을도서관", "wondanghanok.glb", 37.660552, 127.0215044),
        ThreeDPlaceInfo("둘리뮤지엄", "duliymuseum.glb", 37.6522772, 127.0275989),
        ThreeDPlaceInfo("원썸35카페", "wonsome35cafe.glb", 37.66114, 127.0213),
        ThreeDPlaceInfo("서울 창업허브", "changdonghubcube.glb", 37.6553721, 127.0480068),
        ThreeDPlaceInfo("도봉집", "dobongzip.glb", 0.0, 0.0)
    )

    private fun isLocationMatch(
        lat1: Double,
        lng1: Double,
        lat2: Double,
        lng2: Double,
        tolerance: Double = 0.0001
    ): Boolean {
        return kotlin.math.abs(lat1 - lat2) < tolerance && kotlin.math.abs(lng1 - lng2) < tolerance
    }

    private fun createDummy3DPlaces(): List<PlaceDto> {
        return listOf(
            PlaceDto(
                placeId = "dummy_wondanghanok",
                name = "원당한옥마을도서관",
                address = "서울특별시 도봉구 해등로32가길 17",
                latitude = 37.660552,
                longitude = 127.0215044,
                distanceMeters = 950,
                distanceText = "0.9 km",
                imageUrl = "data:image/jpeg;base64,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",
                description = "한옥마을 안에 자리한 아담한 동네 도서관.",
                openingHours = listOf(
                    "월요일: AM 9:00 ~ PM 8:00",
                    "화요일: 휴무일",
                    "수요일: AM 9:00 ~ PM 8:00",
                    "목요일: AM 9:00 ~ PM 8:00",
                    "금요일: AM 9:00 ~ PM 8:00",
                    "토요일: AM 9:00 ~ PM 5:00",
                    "일요일: AM 9:00 ~ PM 5:00"
                ),
                priceLevel = null,
                mapsUrl = "https://maps.app.goo.gl/sJR4HDpMMYgMmvm46",
                phone = "02-000-0000",
                rating = 4.5,
                reviewCount = 87,
                isLiked = false
            ),
            PlaceDto(
                placeId = "dummy_wonsome35cafe",
                name = "원썸35카페",
                address = "서울특별시 도봉구 방학제3동 해등로32가길 40",
                latitude = 37.66114,
                longitude = 127.0213,
                distanceMeters = 1200,
                distanceText = "1.2 km",
                imageUrl = "https://d12zq4w4guyljn.cloudfront.net/20250707051234_photo1_49cacd37483c.webp",
                description = "동네 주민들이 자주 찾는 감성 카페. 브런치와 디저트가 인기.",
                openingHours = listOf(
                    "월요일: AM 10:00 ~ PM 10:00",
                    "화요일: AM 10:00 ~ PM 10:00",
                    "수요일: AM 10:00 ~ PM 10:00",
                    "목요일: AM 10:00 ~ PM 10:00",
                    "금요일: AM 10:00 ~ PM 11:00",
                    "토요일: AM 10:00 ~ PM 11:00",
                    "일요일: AM 11:00 ~ PM 9:00"
                ),
                priceLevel = 2,
                mapsUrl = "https://maps.app.goo.gl/8HNgJjmR5FGNtJSLA",
                phone = "02-111-2233",
                rating = 4.3,
                reviewCount = 152,
                isLiked = false
            ),
            PlaceDto(
                placeId = "dummy_changdonghubcube",
                name = "서울 창업허브",
                address = "서울특별시 도봉구 마들로13길 84",
                latitude = 37.6553721,
                longitude = 127.0480068,
                distanceMeters = 2100,
                distanceText = "2.1 km",
                imageUrl = "https://lh3.googleusercontent.com/gps-cs-s/AG0ilSyeCfBa9pmzGkWlgEa5WNVAiZRBs9EdC7oVsTtybhXmT2tu0EqYiPiG2oTcWh1Jn9DNIFCIEEm-TEfxgEj9MrHczG0Ymp4QeWH0wjiUm_TrWKvQJj_pmpD6XXC9P8acVaywPqg1dQ=w408-h306-k-no",
                description = "지역 스타트업을 위한 공유 오피스와 교육 프로그램을 운영하는 창업 지원 공간.",
                openingHours = listOf(
                    "월요일: AM 9:00 ~ PM 6:00",
                    "화요일: AM 9:00 ~ PM 6:00",
                    "수요일: AM 9:00 ~ PM 6:00",
                    "목요일: AM 9:00 ~ PM 6:00",
                    "금요일: AM 9:00 ~ PM 6:00",
                    "토요일: 휴무일",
                    "일요일: 휴무일"
                ),
                priceLevel = null,
                mapsUrl = "https://maps.app.goo.gl/sEeaKheV5h57jU2x6",
                phone = "02-222-3333",
                rating = 4.6,
                reviewCount = 45,
                isLiked = false
            )
        )
    }


    private val dobongCenter = LatLng.from(37.668, 127.047)

    private val locationClient by lazy {
        LocationServices.getFusedLocationProviderClient(
            requireContext()
        )
    }
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
        isFirstLaunch = savedInstanceState == null

        arguments?.let { args ->
            focusPlaceId = args.getString(ARG_FOCUS_PLACE_ID)
            args.getDouble(ARG_FOCUS_LAT, Double.NaN).takeIf { !it.isNaN() }?.let { lat ->
                args.getDouble(ARG_FOCUS_LNG, Double.NaN).takeIf { !it.isNaN() }?.let { lng ->
                    focusLatLng = Pair(lat, lng)
                }
            }

            args.getString(ARG_RECOMMENDED_PLACE_JSON)?.let { json ->
                try {
                    val gson = com.google.gson.Gson()
                    recommendedPlace = gson.fromJson(json, PlaceDto::class.java)
                } catch (e: Exception) {
                    Log.e("MapFragment", "Failed to parse recommended place JSON: ${e.message}", e)
                }
            }

            args.getStringArrayList(ARG_THREE_D_PLACE_IDS)?.let { ids ->
                threeDPlaceIds.addAll(ids)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentMapBinding.inflate(inflater, container, false)
        setupSearchView()
        return b.root
    }

    private fun setupSearchView() {
        val searchView = b.searchView
        val searchCard = b.searchCard

        searchCard.setOnClickListener {
            searchView.isIconified = false
            searchView.requestFocus()
        }

        val searchEditText =
            searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText?.background = null
        searchEditText?.setPadding(0, 0, 0, 0)

        val searchCloseButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        val searchButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_mag_icon)

        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            val query = searchView.query?.toString()?.trim().orEmpty()
            val hasText = query.isNotBlank()

            if (hasFocus) {
                searchCloseButton?.visibility = if (hasText) View.VISIBLE else View.GONE
                searchButton?.visibility = View.GONE
            } else {
                searchCloseButton?.visibility = View.GONE
                searchButton?.visibility = View.VISIBLE
            }
        }

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val hasText = !newText.isNullOrBlank()
                val hasFocus = searchView.hasFocus()

                if (hasFocus) {
                    searchCloseButton?.visibility = if (hasText) View.VISIBLE else View.GONE
                    searchButton?.visibility = View.GONE
                } else {
                    searchCloseButton?.visibility = View.GONE
                    searchButton?.visibility = View.VISIBLE
                }
                return false
            }
        })

        searchView.setOnCloseListener {
            searchCloseButton?.visibility = View.GONE
            searchButton?.visibility = View.VISIBLE
            false
        }
    }

    private fun performSearch(query: String) {
        if (query.isBlank()) return

        val searchQuery = query.trim()
        val matchingPlace = currentPlaces.firstOrNull { place ->
            place.name.contains(searchQuery, ignoreCase = true)
        }

        if (matchingPlace != null) {
            val map = kakaoMap ?: return
            val targetLatLng =
                com.kakao.vectormap.LatLng.from(matchingPlace.latitude, matchingPlace.longitude)
            map.moveCamera(
                com.kakao.vectormap.camera.CameraUpdateFactory.newCenterPosition(
                    targetLatLng
                )
            )
            map.moveCamera(com.kakao.vectormap.camera.CameraUpdateFactory.zoomTo(17))

            (activity as? MainActivity)?.showPlaceSheet(matchingPlace)

            b.searchView.clearFocus()
            val imm =
                requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(b.searchView.windowToken, 0)
        } else {
            Toast.makeText(requireContext(), "검색 결과를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            // Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
        }
    }

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
                        try {
                            Log.d("MapFragment", "LodLabel 클릭 이벤트 발생, tag=${lodLabel.tag}")
                            val placeId = lodLabel.tag as? String
                            if (placeId != null) {
                                val place = currentPlaces.firstOrNull { it.placeId == placeId }
                                if (place != null) {
                                    Log.d("MapFragment", "마커 클릭: ${place.name}, placeId=$placeId")
                                    showPlaceSheet(place)
                                    true
                                } else {
                                    Log.e(
                                        "MapFragment",
                                        "LodLabel click: place not found for id=$placeId"
                                    )
                                    false
                                }
                            } else {
                                val position = lodLabel.position
                                Log.d(
                                    "MapFragment",
                                    "LodLabel tag 없음, 위치로 찾기: lat=${position.latitude}, lng=${position.longitude}"
                                )
                                val place = currentPlaces.firstOrNull { place ->
                                    isLocationMatch(
                                        place.latitude,
                                        place.longitude,
                                        position.latitude,
                                        position.longitude,
                                        0.001
                                    )
                                }
                                if (place != null) {
                                    Log.d("MapFragment", "마커 클릭 (위치 기반): ${place.name}")
                                    showPlaceSheet(place)
                                    true
                                } else {
                                    Log.e(
                                        "MapFragment",
                                        "LodLabel click: tag is not String and place not found by position, tag=${lodLabel.tag}"
                                    )
                                    false
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("MapFragment", "마커 클릭 처리 중 오류: ${e.message}", e)
                            false
                        }
                    }

                    map.setOnMapClickListener { _, position, _, _ ->
                        Log.d(
                            "MapFragment",
                            "맵 클릭: lat=${position.latitude}, lng=${position.longitude}"
                        )
                        val nearestPlace = currentPlaces.minByOrNull { place ->
                            val dx = place.latitude - position.latitude
                            val dy = place.longitude - position.longitude
                            dx * dx + dy * dy
                        }
                        nearestPlace?.let { place ->
                            val distance = sqrt(
                                (place.latitude - position.latitude).pow(2.0) +
                                        (place.longitude - position.longitude).pow(2.0)
                            )
                            if (distance < 0.002) {
                                Log.d(
                                    "MapFragment",
                                    "맵 클릭으로 마커 감지: ${place.name}, distance=$distance"
                                )
                                showPlaceSheet(place)
                            }
                        }
                    }

                    val context = context ?: return
                    val lm = map.labelManager ?: run {
                        return
                    }

                    placesLayer = lm.lodLayer?.apply { isClickable = true }
                    myLayer =
                        lm.getLayer(MY_LAYER_ID) ?: lm.addLayer(LabelLayerOptions.from(MY_LAYER_ID))
                    debugLayer = lm.getLayer(DEBUG_LAYER_ID) ?: lm.addLayer(
                        LabelLayerOptions.from(DEBUG_LAYER_ID)
                    )

                    val targetCenter = focusLatLng?.let {
                        LatLng.from(it.first, it.second)
                    }
                        ?: recommendedPlace?.let {
                            LatLng.from(it.latitude, it.longitude)
                        }
                        ?: null

                    if (targetCenter != null) {
                        val cameraUpdate = CameraUpdateFactory.newCenterPosition(targetCenter)
                        map.moveCamera(cameraUpdate)
                        map.moveCamera(CameraUpdateFactory.zoomTo(17))
                    } else {
                        map.moveCamera(CameraUpdateFactory.newCenterPosition(dobongCenter))
                        map.moveCamera(CameraUpdateFactory.zoomTo(15))
                    }

                    val initialCenter = targetCenter ?: dobongCenter
                    loadPlacesAndRender(initialCenter, limit = 30)

                    if (targetCenter == null && isFirstLaunch) {
                        ensureLocationAndMove()  // 처음 한 번만 내 위치로 맞추고
                        isFirstLaunch = false    // 이후에는 다시 호출되지 않도록 막기
                    }
                }

                override fun getPosition(): LatLng = dobongCenter
                override fun getZoomLevel(): Int = 15
            }
        )
    }


    private fun loadLikedPlaces() {
        lifecycleScope.launch {
            try {
                val response = RetrofitProvider.placeLikeApi.getMyLikes(size = 30, order = "latest")
                if (response.success) {
                    val likedPlaces = response.data as List<PlaceDto>
                    showToast("${likedPlaces.size}개의 장소를 좋아요 목록에 추가했습니다.")
                } else {
                    showToast("좋아요 목록을 가져오는 데 실패했습니다: ${response.message}")
                }
            } catch (e: Exception) {
                showToast("좋아요 목록을 가져오는 데 실패했습니다: ${e.message}")
            }
        }
    }

    private fun loadPlacesAndRender(center: LatLng, limit: Int = 30, forceReload: Boolean = false) {
        val map = kakaoMap ?: return

        if (placesLoaded && !forceReload && currentPlaces.isNotEmpty()) {
            lifecycleScope.launch {
                renderPlaceMarkers(map, currentPlaces)
                focusOnTarget(currentPlaces)
            }
            return
        }

        lifecycleScope.launch {
            try {
                val places =
                    PlacesRepository().fetchPlaces(center.latitude, center.longitude, limit)
                val combined = mutableListOf<PlaceDto>().apply {
                    val dummyPlaces = createDummy3DPlaces()
                    dummyPlaces.forEach { dummyPlace ->
                        if (none { it.placeId == dummyPlace.placeId }) {
                            add(0, dummyPlace)
                        }
                    }
                    
                    recommendedPlace?.let { rec ->
                        add(0, rec)
                    }
                    addAll(places.filter { it.placeId != recommendedPlace?.placeId })
                }
                currentPlaces = combined
                placesLoaded = true

                renderPlaceMarkers(map, combined)
                focusOnTarget(combined)
                val context = context ?: return@launch
                if (combined.isEmpty()) {
                    addDebugLabel(center, "0 places")
                }
            } catch (e: Exception) {
                Log.e("PLACES", "API 실패: ${e.message}", e)
                val context = context ?: return@launch
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

        val sortedPlaces = places.sortedByDescending { it.placeId == recommendedPlace?.placeId }

        sortedPlaces.forEachIndexed { idx, p ->
            val position = LatLng.from(p.latitude, p.longitude)
            val title = p.name.takeIf { it.isNotBlank() } ?: "이름 없음"

            val isRecommended = p.placeId == recommendedPlace?.placeId
            val isPrimary = isPrimaryPlace(p) && !isRecommended
            val hasThreeD = hasThreeDPlace(p) && !isRecommended

            val variant = when {
                isRecommended -> MarkerVariant.DEFAULT
                isPrimary -> MarkerVariant.THREE_D
                hasThreeD -> MarkerVariant.THREE_D
                else -> MarkerVariant.DEFAULT
            }
            val style = getMarkerStyle(p, variant)

            val options = LabelOptions.from("place_$idx", position)
                .setStyles(style)
                .setTexts(LabelTextBuilder().setTexts(title))
                .setClickable(true)
                .setTag(p.placeId)

            val lodLabel = layer.addLodLabel(options)
            lodLabel?.isClickable = true
            lodLabel?.let {
                placeLabels += it
                Log.d(
                    "MapFragment",
                    "마커 생성: ${p.name}, placeId=${p.placeId}, variant=$variant, clickable=${it.isClickable}"
                )
            }
        }
        Log.d("MapFragment", "총 ${placeLabels.size}개의 마커 생성 완료")
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

        val has3D = threeDPlacesList.any { threeDPlace ->
            if (threeDPlace.latitude == 0.0 && threeDPlace.longitude == 0.0) {
                place.name == threeDPlace.label
            } else {
                isLocationMatch(
                    place.latitude,
                    place.longitude,
                    threeDPlace.latitude,
                    threeDPlace.longitude,
                    0.001
                ) && (place.name == threeDPlace.label || place.name.contains(
                    threeDPlace.label,
                    ignoreCase = true
                ))
            }
        }

        val urlMatch = place.mapsUrl?.contains(THREE_D_WEB_HOST, ignoreCase = true) == true

        val matches = has3D || urlMatch
        if (matches) threeDPlaceIds.add(place.placeId)
        return matches
    }

    private fun isPrimaryPlace(place: PlaceDto): Boolean {
        return threeDPlacesList.any { threeDPlace ->
            if (threeDPlace.latitude == 0.0 && threeDPlace.longitude == 0.0) {
                place.name == threeDPlace.label
            } else {
                isLocationMatch(
                    place.latitude,
                    place.longitude,
                    threeDPlace.latitude,
                    threeDPlace.longitude,
                    0.001
                ) && (place.name == threeDPlace.label || place.name.contains(
                    threeDPlace.label,
                    ignoreCase = true
                ))
            }
        }
    }


    // BottomSheet: place detail
    private fun showPlaceSheet(place: PlaceDto) {
        val mainActivity = activity as? MainActivity
        mainActivity?.showPlaceSheet(place)
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
        val fine = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarse = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
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
                    try {
                        e.startResolutionForResult(activity, REQUEST_RESOLVE_GPS)
                    } catch (_: IntentSender.SendIntentException) {
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
            Toast.makeText(
                context,
                "현 위치로 이동: ${location.latitude}, ${location.longitude}",
                Toast.LENGTH_SHORT
            ).show()
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

    private val myPinStyle by lazy { makePinStyleDp(20) }
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

    private fun buildMarkerBitmap(
        context: android.content.Context,
        place: PlaceDto,
        variant: MarkerVariant
    ): Bitmap {
        val binding = ViewMarkerBinding.inflate(LayoutInflater.from(context))
        val outlineColor = ContextCompat.getColor(context, variant.strokeColorRes)
        val strokePx = (context.resources.displayMetrics.density * 2f).toInt().coerceAtLeast(1)

        (binding.markerCircle.background.mutate() as? GradientDrawable)?.apply {
            setColor(ContextCompat.getColor(context, android.R.color.white))
            setStroke(strokePx, outlineColor)
        }
        binding.markerCircle.backgroundTintList = ColorStateList.valueOf(outlineColor)
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

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.enableMapFragmentLayout()

        if (this::mapView.isInitialized) {
            // 이미 만든 MapView가 있으면 다시 resume만
            mapView.resume()
        } else {
            // 처음 들어올 때만 initMap()
            initMap()
        }
    }

    override fun onPause() {
        if (this::mapView.isInitialized) mapView.pause()
        (activity as? MainActivity)?.restoreNormalLayout()
        (activity as? MainActivity)?.hidePlaceSheet()
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
