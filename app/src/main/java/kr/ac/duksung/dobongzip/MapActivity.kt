package kr.ac.duksung.dobongzip.ui.map

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.databinding.ActivityMapBinding
import com.kakao.vectormap.*

import com.kakao.vectormap.camera.CameraUpdateFactory
import kotlinx.coroutines.*

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView
    private var kakaoMap: KakaoMap? = null

    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)
    private val dobongCenter = LatLng.from(37.668, 127.047) // 도봉구 중심

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ 앱 실행 시 KeyHash 로그 출력
        printKeyHash()

        // ✅ 지도 초기화
        initMap()
    }

    /** 지도 초기화 */
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

                    // ✅ 임시 마커 추가
                    addPlaceMarkers(
                        listOf(
                            PlacePin("place-1", "도봉산입구", 37.6885, 127.0447),
                            PlacePin("place-2", "도봉구청", 37.6688, 127.0471),
                            PlacePin("place-3", "쌍문역", 37.6480, 127.0346)
                        )
                    )
                }

                override fun getPosition(): LatLng = dobongCenter
                override fun getZoomLevel(): Int = 15
            }
        )
    }

    /** 마커 추가 (Label 예시) */
    private fun addPlaceMarkers(pins: List<PlacePin>) {
        val map = kakaoMap ?: return
        try {
            // TODO: 실제 LabelManager API로 교체 필요
            Toast.makeText(this, "마커 ${pins.size}개 준비 완료", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MapActivity", "addPlaceMarkers error: ${e.message}", e)
        }
    }

    /** 🔐 현재 앱의 KeyHash 출력 */
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
                md.update(signature.toByteArray())
                val keyHash = Base64.encodeToString(md.digest(), Base64.NO_WRAP)
                Log.i("Dobongzip", "✅ KeyHash: $keyHash")
                Toast.makeText(this, "KeyHash: $keyHash", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("Dobongzip", "❌ KeyHash error", e)
        }
    }

    override fun onResume() { super.onResume(); if (this::mapView.isInitialized) mapView.resume() }
    override fun onPause()  { if (this::mapView.isInitialized) mapView.pause(); super.onPause() }
    override fun onDestroy() {
        if (this::mapView.isInitialized) binding.mapContainer.removeView(mapView)
        job.cancel()
        kakaoMap = null
        super.onDestroy()
    }
}

/** 간단한 핀 모델 */
data class PlacePin(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double
)
