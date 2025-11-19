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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var originalNavElevation: Float = 0f

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
