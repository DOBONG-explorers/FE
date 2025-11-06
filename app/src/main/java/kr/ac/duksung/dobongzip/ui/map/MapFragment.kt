package kr.ac.duksung.dobongzip.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.models.PlaceDto
import kr.ac.duksung.dobongzip.data.repository.PlacesRepository
import kr.ac.duksung.dobongzip.databinding.FragmentMapBinding
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.pow

class MapFragment : Fragment(R.layout.fragment_map) {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    // 카카오맵 관련 변수들
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    // 내 위치 마커를 위한 변수 추가
    private var myLabel: Label? = null

    private val locationClient by lazy { LocationServices.getFusedLocationProviderClient(requireContext()) }

    // ---------- Location settings ----------
    private val REQUEST_RESOLVE_GPS = 1001
    private val settingsClient: SettingsClient by lazy { LocationServices.getSettingsClient(requireContext()) }

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
                        e.startResolutionForResult(requireActivity(), REQUEST_RESOLVE_GPS)
                    } catch (_: IntentSender.SendIntentException) {
                        Toast.makeText(requireContext(), "위치 설정 요청 실패", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "위치 설정을 켜주세요(고정밀/GPS).", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---------- Fused Location ----------
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

    // ---------- onCreateView ----------
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    // ---------- Map init ----------
    private fun initMap() {
        mapView = MapView(requireContext())
        binding.mapContainer.addView(mapView)

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
                    map.moveCamera(CameraUpdateFactory.newCenterPosition(LatLng.from(37.668, 127.047)))
                    map.moveCamera(CameraUpdateFactory.zoomTo(15))
                    loadPlacesAndRender(LatLng.from(37.668, 127.047))  // 주어진 위치로 장소 로드
                }

                override fun getPosition(): LatLng = LatLng.from(37.668, 127.047)
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    // ---------- 거리 계산 및 장소 로딩 ----------
    private fun loadPlacesAndRender(center: LatLng, limit: Int = 30) {
        lifecycleScope.launch {
            try {
                val places = PlacesRepository().fetchPlaces(center.latitude, center.longitude, limit)
                renderPlaceMarkers(places)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "장소 로드 실패: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // ---------- 장소 마커 표시 ----------
    private fun renderPlaceMarkers(places: List<PlaceDto>) {
        val layer = kakaoMap?.labelManager?.getLayer("places_layer") ?: return
        layer.isClickable = true
        layer.isVisible = true

        places.forEach { p ->
            val position = LatLng.from(p.latitude, p.longitude)

            // 핀 스타일 설정 (예: placePinStyle 사용)
            val pinStyle = placePinStyle

            val opt = LabelOptions.from(position)
                .setStyles(pinStyle)
                .setTexts(LabelTextBuilder().setTexts(p.name))
                .setClickable(true)

            val label = layer.addLabel(opt)
            label?.apply { tag = p }
        }
    }

    // ---------- 내 위치로 이동 시 호출되는 함수 ----------
    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            Toast.makeText(requireContext(), "위치 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        // 위치를 요청하여 얻은 후 centerMapTo 호출
        locationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                centerMapTo(it) // 현재 위치로 지도 이동
            }
        }
    }

    // ---------- 내 위치로 지도 이동 및 마커 추가 ----------
    private fun centerMapTo(location: Location) {
        val here = LatLng.from(location.latitude, location.longitude) // 현재 위치를 LatLng으로 변환

        kakaoMap?.let { map ->
            // 카메라를 해당 위치로 이동
            map.moveCamera(CameraUpdateFactory.newCenterPosition(here))
            // 줌 레벨을 16으로 설정
            map.moveCamera(CameraUpdateFactory.zoomTo(16))

            // 사용자에게 위치 이동 완료 메시지 출력
            Toast.makeText(requireContext(), "현 위치로 이동: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()

            // 내 위치 마커 추가
            addMyLocationMarker(here)
        }
    }

    // ---------- 내 위치 마커 추가 ----------
    private fun addMyLocationMarker(here: LatLng) {
        val layer = kakaoMap?.labelManager?.getLayer("me_layer") ?: return
        layer.isClickable = false
        layer.isVisible = true

        // 기존 마커가 있으면 제거
        myLabel?.remove()

        // 내 위치 마커 스타일 설정 (myPinStyle)
        val opt = LabelOptions.from(here)
            .setStyles(myPinStyle)
            .setTexts(LabelTextBuilder().setTexts("내 위치")) // "내 위치"라는 텍스트 설정
        myLabel = layer.addLabel(opt) // 레이어에 마커 추가
    }

    // ---------- Small pin styles ----------
    private fun makePinStyleDp(targetHeightDp: Int): LabelStyle {
        val dm = resources.displayMetrics
        // Convert dp to pixels (height)
        val hPx = (targetHeightDp * dm.density + 0.5f).toInt()

        // Get the bitmap for the pin (ensure it exists in your resources)
        val src = BitmapFactory.decodeResource(resources, R.drawable.pin)

        // Calculate the width based on the original aspect ratio
        val ratio = src.width.toFloat() / src.height
        val wPx = (hPx * ratio).toInt()

        // Scale the image to the desired size
        val scaled: Bitmap = Bitmap.createScaledBitmap(src, wPx, hPx, true)

        // Recycle the original bitmap if it's not the same as the scaled version
        if (scaled != src) src.recycle()

        // Create the LabelStyle from the scaled bitmap and set the anchor point to the center-bottom
        return LabelStyle.from(scaled).setAnchorPoint(0.5f, 1.0f)
    }

    // Define different pin styles for different usages
    private val placePinStyle by lazy { makePinStyleDp(18) }  // 18dp height for place pins
    private val myPinStyle by lazy { makePinStyleDp(20) }     // 20dp height for "my" location pin
    private val debugPinStyle by lazy { makePinStyleDp(16) }  // 16dp height for debug pins


    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    // ---------- onResume and onPause ----------
    override fun onResume() {
        super.onResume()
        initMap() // 지도 초기화
    }

    override fun onPause() {
        super.onPause()
        mapView.pause() // 지도 일시 정지
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.removeAllViews() // 지도 뷰 제거
        _binding = null
    }
}
