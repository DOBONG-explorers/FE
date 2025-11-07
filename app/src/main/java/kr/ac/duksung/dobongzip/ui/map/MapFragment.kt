package kr.ac.duksung.dobongzip.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.card.MaterialCardView
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository
import kr.ac.duksung.dobongzip.databinding.FragmentMapBinding
import kotlin.math.pow
import kotlin.math.sin

class MapFragment : Fragment(R.layout.fragment_map) {

    private var _b: FragmentMapBinding? = null
    private val b get() = _b!!

    // Kakao Map
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    // Layers / Labels
    private var placesLayer: LodLabelLayer? = null      // 🔁 LodLabelLayer 사용
    private var myLayer: LabelLayer? = null
    private var debugLayer: LabelLayer? = null
    private val placeLabels = mutableListOf<LodLabel>() // 🔁 LodLabel 리스트로 보관
    private var myLabel: Label? = null

    private lateinit var sheetBehavior: BottomSheetBehavior<MaterialCardView>

    // Constants
    private val PLACES_LAYER_ID = "places_layer"
    private val MY_LAYER_ID = "me_layer"
    private val DEBUG_LAYER_ID = "debug_layer"
    private val dobongCenter = LatLng.from(37.668, 127.047)

    // Location
    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }
    private val settingsClient by lazy { LocationServices.getSettingsClient(requireContext()) }
    private val REQUEST_RESOLVE_GPS = 1001

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
        else Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = FragmentMapBinding.inflate(inflater, container, false)

        // 바텀시트 세팅
        sheetBehavior = BottomSheetBehavior.from(b.placeSheet).apply {
            isDraggable = true
            peekHeight = (120 * resources.displayMetrics.density).toInt()
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
        b.placeSheet.apply {
            bringToFront()
            alpha = 1f
            isVisible = false
        }

        return b.root
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
                    Toast.makeText(requireContext(), "지도 초기화 오류: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
            object : KakaoMapReadyCallback() {
                override fun onMapReady(map: KakaoMap) {
                    kakaoMap = map

                    // ✅ LodLabel 클릭 리스너 (SDK가 제공)
                    map.setOnLodLabelClickListener { _, _, lodLabel ->
                        (lodLabel.tag as? PlaceDto)?.let { place ->
                            showPlaceSheet(place)
                            true
                        } ?: false
                    }

                    // 레이어 생성/획득
                    val lm = map.labelManager ?: run {
                        Toast.makeText(requireContext(), "레이어 매니저 초기화 실패", Toast.LENGTH_LONG).show()
                        return
                    }
                    // LodLabel 전용 레이어
                    placesLayer = lm.lodLayer?.apply { isClickable = true }
                    // 일반 Label 레이어 (내 위치/디버그)
                    myLayer     = lm.getLayer(MY_LAYER_ID)     ?: lm.addLayer(LabelLayerOptions.from(MY_LAYER_ID))
                    debugLayer  = lm.getLayer(DEBUG_LAYER_ID)  ?: lm.addLayer(LabelLayerOptions.from(DEBUG_LAYER_ID))

                    // 카메라
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(dobongCenter))
                    map.moveCamera(CameraUpdateFactory.zoomTo(15))

                    // 위치 & 장소 로드
                    ensureLocationAndMove()
                    loadPlacesAndRender(dobongCenter, limit = 30)
                }

                override fun getPosition(): LatLng = dobongCenter
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    // Places load & render
    private fun loadPlacesAndRender(center: LatLng, limit: Int = 30) {
        val map = kakaoMap ?: return
        lifecycleScope.launch {
            try {
                val places = PlacesRepository().fetchPlaces(center.latitude, center.longitude, limit)
                renderPlaceMarkers(map, places)
                if (places.isEmpty()) {
                    Toast.makeText(requireContext(), "서버 응답 성공, 하지만 0건", Toast.LENGTH_SHORT).show()
                    addDebugLabel(center, "0 places")
                } else {
                    Toast.makeText(requireContext(), "명소 ${places.size}개 표시", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("PLACES", "API 실패: ${e.message}", e)
                Toast.makeText(requireContext(), "장소 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
                addDebugLabel(center, "API FAIL")
            }
        }
    }

    // 🔁 LodLabel 로 마커 렌더링
    private fun renderPlaceMarkers(map: KakaoMap, places: List<PlaceDto>) {
        val layer = placesLayer ?: return
        layer.isClickable = true
        layer.isVisible = true

        // 기존 라벨 정리
        placeLabels.forEach { it.remove() }
        placeLabels.clear()

        // addLabels() 로 한번에 추가해도 됨. 여기선 가독성 위해 개별 추가
        places.forEachIndexed { idx, p ->
            val position = LatLng.from(p.latitude, p.longitude)
            val title = p.name.takeIf { it.isNotBlank() } ?: "이름 없음"

            // LodLabel 은 id 가 필요 (고유)
            val options = LabelOptions.from("place_$idx", position)
                .setStyles(placePinStyle)
                .setTexts(LabelTextBuilder().setTexts(title))
                .setClickable(true)
                .setTag(p) // 클릭시 꺼낼 PlaceDto

            val lodLabel = layer.addLodLabel(options)
            lodLabel?.let { placeLabels += it }
        }
    }

    // BottomSheet: place detail
    private fun showPlaceSheet(place: PlaceDto) {
        val img = b.imgPlace
        val name = b.txtPlaceName
        val dist = b.txtDistance
        val phone = b.txtPhone
        val btn3d = b.btnView3D

        name.text = place.name
        phone.text = place.phone ?: "전화번호 없음"

        val cam = kakaoMap?.cameraPosition?.position
        val distanceText = place.distanceText ?: cam?.let {
            val km = haversineKm(it.latitude, it.longitude, place.latitude, place.longitude)
            String.format("내 위치로부터 %.1fkm", km)
        } ?: ""
        dist.text = distanceText

        if (!place.imageUrl.isNullOrBlank()) Glide.with(this).load(place.imageUrl).into(img)
        else img.setImageResource(R.drawable.placeholder)

        btn3d.setOnClickListener {
            val url = place.mapsUrl ?: "https://maps.google.com/?q=${place.latitude},${place.longitude}"
            startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
        }

        b.placeSheet.isVisible = true
        sheetBehavior.isHideable = true
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
        val dLon = Math.toRadians(lat2 - lon1)
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
                if (e is ResolvableApiException) {
                    try { e.startResolutionForResult(requireActivity(), REQUEST_RESOLVE_GPS) }
                    catch (_: IntentSender.SendIntentException) {
                        Toast.makeText(requireContext(), "위치 설정 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "위치 설정을 켜주세요(고정밀/GPS).", Toast.LENGTH_LONG).show()
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
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.newCenterPosition(here))
            map.moveCamera(CameraUpdateFactory.zoomTo(16))
            Toast.makeText(requireContext(), "현 위치로 이동: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
            addMyLocationMarker(here)
        }
    }

    // Pin styles (dp sizing)
    private fun makePinStyleDp(targetHeightDp: Int): LabelStyle {
        val dm = resources.displayMetrics
        val hPx = (targetHeightDp * dm.density + 0.5f).toInt()
        val src = BitmapFactory.decodeResource(resources, R.drawable.pin)
        val ratio = src.width.toFloat() / src.height
        val wPx = (hPx * ratio).toInt()
        val scaled: Bitmap = Bitmap.createScaledBitmap(src, wPx, hPx, true)
        if (scaled != src) src.recycle()
        return LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)
    }
    private val placePinStyle by lazy { makePinStyleDp(18) }
    private val myPinStyle    by lazy { makePinStyleDp(20) }
    private val debugPinStyle by lazy { makePinStyleDp(16) }

    // Lifecycle
    override fun onResume() {
        super.onResume()
        initMap()
    }

    override fun onPause() {
        if (this::mapView.isInitialized) mapView.pause()
        super.onPause()
    }

    override fun onDestroyView() {
        if (this::mapView.isInitialized) b.mapContainer.removeView(mapView)

        myLabel?.remove(); myLabel = null
        placeLabels.forEach { it.remove() }
        placeLabels.clear()

        placesLayer?.removeAll()
        myLayer?.removeAll()
        debugLayer?.removeAll()

        kakaoMap = null
        _b = null
        super.onDestroyView()
    }
}
