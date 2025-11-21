// kr/ac/duksung/dobongzip/ui/mypage/MyPageSetFragment.kt
package kr.ac.duksung.dobongzip.ui.mypage

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
// import android.widget.Button  // ❌ 이제 안 씀
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.LoginActivity
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.DELETE

class MyPageSetFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvBirth: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnToEdit: android.widget.Button
    private lateinit var tvDeleteAccount: TextView

    private val profileViewModel: ProfileViewModel by activityViewModels()

    // ✅ 회원 탈퇴 API 호출용 Retrofit 클라이언트
    private val authApi: AuthApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                // Authorization 헤더에 토큰 붙이기
                TokenHolder.accessToken?.let { token ->
                    builder.header("Authorization", "Bearer $token")
                }
                chain.proceed(builder.build())
            }
            .build()

        Retrofit.Builder()
            .baseUrl("https://dobongzip.com")   // TODO: 실제 서버 주소에 맞게 수정
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.fragment_my_page, container, false)

        // XML id 연결
        profileImage    = v.findViewById(R.id.profileImage)
        tvNickname      = v.findViewById(R.id.editNickname)
        tvBirth         = v.findViewById(R.id.editBirthday)
        tvEmail         = v.findViewById(R.id.editEmail)
        btnBack         = v.findViewById(R.id.backButton)
        btnToEdit       = v.findViewById(R.id.myPageButton)
        tvDeleteAccount = v.findViewById(R.id.deleteAccountText)

        // 뒤로가기
        btnBack.setOnClickListener { findNavController().popBackStack() }

        // "개인정보 수정" → 편집 화면 이동
        btnToEdit.setOnClickListener {
            findNavController().navigate(R.id.myPageEditFragment)
        }

        // ✅ 계정 탈퇴 클릭 리스너 (커스텀 다이얼로그만 띄움)
        tvDeleteAccount.setOnClickListener {
            showWithdrawDialog()
        }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 진입 시 서버 최신값 로드
        profileViewModel.loadProfileAll()

        // 상태 구독 → UI 반영
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    // 1) 프로필 이미지: 서버 URL 우선 → 로컬 uri → 기본 이미지
                    when {
                        !state.imageUrl.isNullOrBlank() -> {
                            Glide.with(this@MyPageSetFragment)
                                .load(state.imageUrl)
                                .centerCrop()
                                .into(profileImage)
                        }
                        state.uri != null -> {
                            Glide.with(this@MyPageSetFragment)
                                .load(state.uri)
                                .centerCrop()
                                .into(profileImage)
                        }
                        else -> profileImage.setImageResource(R.drawable.prf3)
                    }

                    // 2) 텍스트 정보
                    tvNickname.text = state.nickname ?: "-"
                    tvBirth.text    = state.birthday ?: "-"
                    tvEmail.text    = state.email ?: "-"
                }
            }
        }
    }

    /**
     * 회원탈퇴 확인용 커스텀 다이얼로그
     * 레이아웃: res/layout/dialog_withdraw_confirm.xml
     */
    private fun showWithdrawDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_withdraw_confirm, null)

        // ❗ XML에서 btnCancel/btnConfirm 는 TextView 이므로 TextView로 받아야 함
        val btnCancel  = dialogView.findViewById<TextView>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btnConfirm)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // 배경 투명하게 해서 둥근 모서리 레이아웃 살리기 (선택)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            // 🔥 실제 탈퇴 API 호출
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val res = withContext(Dispatchers.IO) {
                        authApi.withdraw()
                    }

                    if (res.success) {
                        // 토큰, 로컬 프로필 정보 삭제
                        TokenHolder.accessToken = null
                        val sp = requireActivity()
                            .getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                        sp.edit().clear().apply()

                        // 로그인 화면으로 완전 이동 (백 스택 비우기)
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags =
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)

                        requireActivity().finish()
                    } else {
                        android.widget.Toast.makeText(
                            requireContext(),
                            res.message ?: "탈퇴에 실패했습니다.",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.widget.Toast.makeText(
                        requireContext(),
                        "네트워크 오류로 탈퇴 요청에 실패했습니다.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            }

            dialog.dismiss()
        }

        dialog.show()
    }
}

/**
 * 백엔드 회원탈퇴 응답 스펙에 맞춘 데이터 클래스
 * {
 *   "success": true,
 *   "httpStatus": 0,
 *   "message": "string",
 *   "data": "string"
 * }
 */
data class WithdrawResponse(
    val success: Boolean,
    val httpStatus: Int,
    val message: String?,
    val data: String?
)

// 회원탈퇴 API 정의
interface AuthApi {
    @DELETE("/api/v1/auth/withdraw")
    suspend fun withdraw(): WithdrawResponse
}
