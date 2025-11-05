package kr.ac.duksung.dobongzip.ui.map

import androidx.core.view.isVisible
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetBehavior

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository
import kr.ac.duksung.dobongzip.databinding.ActivityMapBinding
import kr.ac.duksung.dobongzip.ui.chat.ChatFragment
import kr.ac.duksung.dobongzip.ui.like.LikesFragment
import kr.ac.duksung.dobongzip.ui.mypage.MyPageFragment
import kotlin.math.max
import kotlin.math.pow

class MapActivity : AppCompatActivity() {

    // ---------- Location settings ----------
    private val REQUEST_RESOLVE_GPS = 1001
    private val settingsClient: SettingsClient by lazy { LocationServices.getSettingsClient(this) }

    private fun ensureLocationSettings(onReady: () -> Unit) {
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setWaitForAccurateLocation(true)
            .build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(req)
        settingsClient.checkLocationSettings(builder.build())
            .addOnSuccessListener { onReady() }
            .addOnFailureListener { e ->
                if (e is ResolvableApiException) {
                    try {
                        e.startResolutionForResult(this, REQUEST_RESOLVE_GPS)
                    } catch (_: IntentSender.SendIntentException) {
                        Toast.makeText(this, "위치 설정 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "위치 설정을 켜주세요(고정밀/GPS).", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---------- View / Map ----------
    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    // ---------- BottomSheet ----------
    private lateinit var sheetBehavior: BottomSheetBehavior<View>

    // ---------- Layers / Labels ----------
    private var placesLayer: LabelLayer? = null
    private var myLayer: LabelLayer? = null
    private var debugLayer: LabelLayer? = null
    private val placeLabels = mutableListOf<Label>()
    private var myLabel: Label? = null

    // ---------- Constants ----------
    private val PLACES_LAYER_ID = "places_layer"
    private val MY_LAYER_ID = "me_layer"
    private val DEBUG_LAYER_ID = "debug_layer"
    private val dobongCenter = LatLng.from(37.668, 127.047)

    // ---------- Fused Location ----------
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
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
        else Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    // ---------- Lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 바텀시트 초기화
        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.placeSheet)).apply {
            isDraggable = true
            peekHeight = (120 * resources.displayMetrics.density).toInt()
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
        findViewById<View>(R.id.placeSheet).apply {
            bringToFront()
            alpha = 1f
        }

// ---------- 하단 네비게이션 ----------
        val navView: BottomNavigationView = binding.navView
        navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // 홈으로 전환
                    binding.mapContainer.isVisible = true
                    findViewById<View>(R.id.placeSheet).isVisible = true
                    binding.contentContainer.isVisible = false
                    true
                }
                R.id.navigation_chat -> {
                    // 채팅 화면으로 전환 (하단바는 그대로 보여줌)
                    openContent(ChatFragment())
                    true
                }
                R.id.navigation_notifications -> {
                    openContent(LikesFragment()); true
                }
                R.id.navigation_mypage -> {
                    openContent(MyPageFragment()); true
                }
                else -> false
            }
        }
        navView.selectedItemId = R.id.navigation_home


        printKeyHash()
        initMap()
        setupContentContainerInsets()
    }

    private fun setupContentContainerInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.contentContainer) { v, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navH = binding.navView.height
            val base = max(sys.bottom, navH + dp(12))
            val bottom = max(base, ime.bottom)
            v.updatePadding(bottom = bottom)
            insets
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun openContent(fragment: Fragment) {
        binding.mapContainer.isVisible = false
        findViewById<View>(R.id.placeSheet).isVisible = false
        binding.contentContainer.isVisible = true

        supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, fragment)
            .commit()
    }

    // ---------- Map init ----------
    private fun initMap() {
        mapView = MapView(this)
        binding.mapContainer.addView(mapView)

        mapView.start(
            object : MapLifeCycleCallback() {
                override fun onMapDestroy() {}
                override fun onMapError(e: Exception) {
                    Log.e("MapActivity", "Map init error: ${e.message}", e)
                    Toast.makeText(this@MapActivity, "지도 초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(dobongCenter))
                    map.moveCamera(CameraUpdateFactory.zoomTo(15))

                    // ✅ 라벨 클릭 리스너: onMapReady에서 한 번만
                    map.setOnLabelClickListener { _, label, _ ->
                        (label.tag as? PlaceDto)?.let {
                            showPlaceSheet(it)
                            true
                        } ?: false
                    }

                    val lm = map.labelManager
                    if (lm != null) {
                        placesLayer = lm.getLayer(PLACES_LAYER_ID)
                            ?: lm.addLayer(LabelLayerOptions.from(PLACES_LAYER_ID))
                        myLayer = lm.getLayer(MY_LAYER_ID)
                            ?: lm.addLayer(LabelLayerOptions.from(MY_LAYER_ID))
                        debugLayer = lm.getLayer(DEBUG_LAYER_ID)
                            ?: lm.addLayer(LabelLayerOptions.from(DEBUG_LAYER_ID))
                    } else {
                        Toast.makeText(this@MapActivity, "레이어 초기화 오류", Toast.LENGTH_LONG).show()
                        return
                    }

                    addDebugLabel(dobongCenter, "DEBUG PIN")
                    pingBackendOnce()
                    ensureLocationAndMove()
                    loadPlacesAndRender(dobongCenter, limit = 30)
                }

                override fun getPosition(): LatLng = dobongCenter
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    // ---------- Labels ----------
    private fun addMyLocationMarker(here: LatLng) {
        val layer = myLayer ?: return
        layer.isClickable = false
        layer.isVisible = true
        myLabel?.remove()
        val opt = LabelOptions.from(here)
            .setStyles(myPinStyle)
            .setTexts(LabelTextBuilder().setTexts("내 위치"))
        myLabel = layer.addLabel(opt)
    }

    // ✅ 디버그 라벨은 debugLayer로, 실제 장소 레이어 건드리지 않음
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

    private fun renderPlaceMarkers(map: KakaoMap, places: List<PlaceDto>) {
        val layer = placesLayer ?: return
        layer.isClickable = true
        layer.isVisible = true

        // 기존 라벨 제거
        placeLabels.forEach { it.remove() }
        placeLabels.clear()

        // 장소 라벨 추가
        places.forEach { p ->
            val position = LatLng.from(p.latitude, p.longitude)
            val title = p.name.takeIf { it.isNotBlank() } ?: "이름 없음"

            val opt = LabelOptions.from(position)
                .setStyles(placePinStyle)
                .setTexts(LabelTextBuilder().setTexts(title))
                .setClickable(true) // ✅ 중요: 옵션에서 클릭 가능

            val label = layer.addLabel(opt)
            label?.apply {
                tag = p
                isClickable = true // ✅ 중요: 라벨에서도 클릭 가능
                placeLabels += this
            }
        }
    }

    private fun showPlaceSheet(place: PlaceDto) {
        val img = findViewById<ImageView>(R.id.imgPlace)
        val name = findViewById<TextView>(R.id.txtPlaceName)
        val dist = findViewById<TextView>(R.id.txtDistance)
        val phone = findViewById<TextView>(R.id.txtPhone)
        val btn3d = findViewById<MaterialButton>(R.id.btnView3D)

        name.text = place.name
        phone.text = place.phone ?: "전화번호 없음"

        val distanceText = place.distanceText ?: run {
            val cam = kakaoMap?.cameraPosition?.position
            if (cam != null) {
                val km = haversineKm(cam.latitude, cam.longitude, place.latitude, place.longitude)
                String.format("내 위치로부터 %.1fkm", km)
            } else null
        }
        dist.text = distanceText ?: ""

        if (!place.imageUrl.isNullOrBlank()) Glide.with(this).load(place.imageUrl).into(img)
        else img.setImageResource(R.drawable.placeholder)

        btn3d.setOnClickListener {
            val url = place.mapsUrl ?: "https://maps.google.com/?q=${place.latitude},${place.longitude}"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }

        // ✅ 바텀시트 보이기
        findViewById<View>(R.id.placeSheet).isVisible = true
        sheetBehavior.isHideable = true
        sheetBehavior.state = BottomSheetBehavior.STATE_HALF_EXPANDED
    }

    // ---------- 거리 계산 ----------
    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = kotlin.math.sin(dLat/2).pow(2.0) +
                kotlin.math.cos(Math.toRadians(lat1)) *
                kotlin.math.cos(Math.toRadians(lat2)) *
                kotlin.math.sin(dLon/2).pow(2.0)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return R * c
    }

    // ---------- Permission helpers ----------
    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureLocationAndMove() {
        if (hasLocationPermission()) {
            ensureLocationSettings { moveToMyLocation() }
        } else {
            requestLocationPerms.launch(locationPerms)
        }
    }

    // ---------- Location flow ----------
    private var oneShotCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    private fun requestOneShotHighAccuracy(onFix: (Location?) -> Unit) {
        if (!hasLocationPermission()) { onFix(null); return }

        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 800L)
            .setWaitForAccurateLocation(true)
            .setMinUpdateIntervalMillis(500L)
            .setMaxUpdates(1)
            .build()

        oneShotCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation
                locationClient.removeLocationUpdates(this)
                oneShotCallback = null
                onFix(loc)
            }
        }
        locationClient.requestLocationUpdates(req, oneShotCallback!!, Looper.getMainLooper())

        // 타임아웃 3.5초: 못 받으면 null 콜백
        binding.root.postDelayed({
            oneShotCallback?.let {
                locationClient.removeLocationUpdates(it)
                oneShotCallback = null
                onFix(null)
            }
        }, 3500L)
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val tokenSource = CancellationTokenSource()
        try {
            locationClient
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, tokenSource.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        logLoc("getCurrentLocation", location)
                        centerMapTo(location)
                        loadPlacesAndRender(LatLng.from(location.latitude, location.longitude))
                    } else {
                        requestOneShotHighAccuracy { fresh ->
                            if (fresh != null) {
                                logLoc("oneShotUpdate", fresh)
                                centerMapTo(fresh)
                                loadPlacesAndRender(LatLng.from(fresh.latitude, fresh.longitude))
                            } else {
                                fetchLastLocationFallback()
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    requestOneShotHighAccuracy { fresh ->
                        if (fresh != null) {
                            logLoc("oneShotUpdate(after fail)", fresh)
                            centerMapTo(fresh)
                            loadPlacesAndRender(LatLng.from(fresh.latitude, fresh.longitude))
                        } else {
                            fetchLastLocationFallback()
                        }
                    }
                }
        } catch (_: SecurityException) {
            Toast.makeText(this@MapActivity, "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLastLocationFallback() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "위치 권한이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            locationClient.lastLocation
                .addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        logLoc("lastLocation", lastLocation)
                        centerMapTo(lastLocation)
                        loadPlacesAndRender(LatLng.from(lastLocation.latitude, lastLocation.longitude))
                    } else {
                        Toast.makeText(this@MapActivity, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@MapActivity, "현재 위치 확인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (_: SecurityException) {
            Toast.makeText(this@MapActivity, "위치 권한이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun centerMapTo(location: Location) {
        val here = LatLng.from(location.latitude, location.longitude)
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.newCenterPosition(here))
            map.moveCamera(CameraUpdateFactory.zoomTo(16))
            Toast.makeText(this, "현 위치로 이동: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
            addMyLocationMarker(here)
        }
    }

    private fun logLoc(tag: String, loc: Location) {
        Log.d("LOC", "$tag lat=${loc.latitude}, lng=${loc.longitude}, acc=${loc.accuracy}, provider=${loc.provider}, time=${loc.time}")
    }

    // ---------- Small pin styles ----------
    private fun makePinStyleDp(targetHeightDp: Int): LabelStyle {
        val dm = resources.displayMetrics
        val hPx = (targetHeightDp * dm.density + 0.5f).toInt()
        val src = BitmapFactory.decodeResource(resources, R.drawable.pin)
        val ratio = src.width.toFloat() / src.height
        val wPx = (hPx * ratio).toInt()
        val scaled: Bitmap = Bitmap.createScaledBitmap(src, wPx, hPx, true)
        if (scaled != src) src.recycle()
        // 앵커는 중앙 하단이 자연스러움
        return LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)
    }

    private val placePinStyle by lazy { makePinStyleDp(18) }
    private val myPinStyle    by lazy { makePinStyleDp(20) }
    private val debugPinStyle by lazy { makePinStyleDp(16) }

    // ---------- UI / Misc ----------
    private fun showPlaceDetailBottomSheet(p: PlaceDto) {
        val ratingText = p.rating?.let { "⭐ ${"%.1f".format(it)}" } ?: "⭐ -"
        val reviewText = "리뷰 ${p.reviewCount ?: 0}"
        val distText = p.distanceText ?: ""
        val meta = listOf(ratingText, reviewText, distText).filter { it.isNotBlank() }.joinToString(" · ")
        Toast.makeText(this, "${p.name}\n$meta", Toast.LENGTH_LONG).show()
    }

    private fun pingBackendOnce() {
        val testLat = 37.668
        val testLng = 127.047
        lifecycleScope.launch {
            try {
                val places = PlacesRepository().fetchPlaces(testLat, testLng, 1)
                Log.d("API_TEST", "OK: ${places.size}개")
                Toast.makeText(this@MapActivity, "API OK (${places.size})", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("API_TEST", "FAIL: ${e.message}", e)
                Toast.makeText(this@MapActivity, "API FAIL: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun printKeyHash() {
        try {
            val packageInfo: PackageInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
                    )
                } else {
                    @Suppress("DEPRECATION")
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                }

            val signatures: Array<Signature> =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.signingInfo?.apkContentsSigners ?: emptyArray()
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.signatures ?: emptyArray()
                }

            val md = java.security.MessageDigest.getInstance("SHA")
            for (signature in signatures) {
                md.reset()
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.i("Dobongzip", "✅ KeyHash: $keyHash")
                Toast.makeText(this, "KeyHash: $keyHash", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Dobongzip", "❌ KeyHash error", e)
        }
    }

    // ---------- Activity result ----------
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_RESOLVE_GPS) {
            if (resultCode == Activity.RESULT_OK) {
                moveToMyLocation()
            } else {
                Toast.makeText(this, "위치 설정이 꺼져 있어요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------- Lifecycle ----------
    override fun onResume() {
        super.onResume()
        if (this::mapView.isInitialized) mapView.resume()
    }

    override fun onPause() {
        oneShotCallback?.let {
            locationClient.removeLocationUpdates(it)
            oneShotCallback = null
        }
        if (this::mapView.isInitialized) mapView.pause()
        super.onPause()
    }

    override fun onDestroy() {
        if (this::mapView.isInitialized) binding.mapContainer.removeView(mapView)

        oneShotCallback?.let {
            locationClient.removeLocationUpdates(it)
            oneShotCallback = null
        }

        myLabel?.remove(); myLabel = null
        placeLabels.forEach { it.remove() }
        placeLabels.clear()

        placesLayer?.removeAll()
        myLayer?.removeAll()
        debugLayer?.removeAll()
        placesLayer = null
        myLayer = null
        debugLayer = null

        kakaoMap = null
        super.onDestroy()
    }

    // ---------- Places fetch & render ----------
    private fun loadPlacesAndRender(center: LatLng, limit: Int = 30) {
        val map = kakaoMap ?: return
        Log.d("PLACES", "→ API call lat=${center.latitude}, lng=${center.longitude}, limit=$limit")
        lifecycleScope.launch {
            try {
                val places = PlacesRepository().fetchPlaces(center.latitude, center.longitude, limit)
                Log.d("PLACES", "← API ok, size=${places.size}")
                renderPlaceMarkers(map, places)
                if (places.isEmpty()) {
                    Toast.makeText(this@MapActivity, "서버 응답은 성공, 하지만 0건", Toast.LENGTH_SHORT).show()
                    addDebugLabel(center, "TEST(0건)")
                } else {
                    Toast.makeText(this@MapActivity, "명소 ${places.size}개 표시", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PLACES", "API 실패: ${e.message}", e)
                Toast.makeText(this@MapActivity, "장소 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                addDebugLabel(center, "API FAIL")
            }
        }
    }
}

/** 간단한 핀 모델 (임시) — 필요 없으면 삭제 가능 */
data class PlacePin(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)
