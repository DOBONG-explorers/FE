// kr/ac/duksung/dobongzip/ui/mypage/MyPageFragment.kt
package kr.ac.duksung.dobongzip.ui.mypage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentMyMageRealBinding
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel
import kr.ac.duksung.dobongzip.data.auth.TokenHolder

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyMageRealBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "app_prefs"
    private val keyDark = "dark_mode_on"

    // Activity 범위 ViewModel (프로필 공용 상태)
    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyMageRealBinding.inflate(inflater, container, false)
        return binding.root
    }

    // 화면 진입 시 최초 로드
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 서버에서 최신 프로필 + 이미지 로드
        profileViewModel.loadProfileAll()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->

                    // 1) 프로필 이미지 (그대로 유지)
                    when {
                        !state.imageUrl.isNullOrBlank() -> {
                            Glide.with(this@MyPageFragment)
                                .load(state.imageUrl)
                                .centerCrop()
                                .into(binding.profileImage)
                        }
                        state.uri != null -> {
                            Glide.with(this@MyPageFragment)
                                .load(state.uri)
                                .centerCrop()
                                .into(binding.profileImage)
                        }
                        else -> binding.profileImage.setImageResource(R.drawable.prf3)
                    }

                    // ✅ SharedPreferences에서 소셜 프로필 fallback 가져오기
                    val spProfile = requireActivity().getSharedPreferences("user_profile", Context.MODE_PRIVATE)
                    val localNickname = spProfile.getString("nickname", null)
                    val localEmail = spProfile.getString("email", null)

                    // 2) 텍스트 정보: 서버 값 > 로컬 소셜 프로필 > 기본값
                    val finalName = when {
                        !state.nickname.isNullOrBlank() -> state.nickname
                        !localNickname.isNullOrBlank() -> localNickname
                        else -> "입맛까다로운햄스터"
                    }

                    val finalEmail = when {
                        !state.email.isNullOrBlank() -> state.email
                        !localEmail.isNullOrBlank() -> localEmail
                        else -> "dobongzip@gmail.com"
                    }

                    binding.myname.text = finalName
                    binding.mymail.text = finalEmail

                    // 전화번호/아이디는 필요하면 여기에 추가
                }
            }
        }


        // 네비게이션
        binding.backButton.setOnClickListener { findNavController().popBackStack() }
        binding.privacySettingCard.setOnClickListener {
            findNavController().navigate(R.id.myPageSetFragment)
        }
        binding.securityCard.setOnClickListener {
            findNavController().navigate(R.id.securityFragment)
        }
        binding.supportCard.setOnClickListener {
            findNavController().navigate(R.id.supportFragment)
        }

        // (선택) 로그아웃 카드가 있다면 처리
        binding.signoutview?.setOnClickListener {
            // 서버 /api/v1/auth/logout 연동이 필요하면 여기서 호출 후 토큰을 비우세요.
            // 현재는 토큰만 비우고 로그인 화면으로 유도하는 예시입니다.
            TokenHolder.accessToken = null
            // startActivity(Intent(requireContext(), LoginActivity::class.java))
            // requireActivity().finish()
        }

        // (참고) 다크모드 스위치 저장 값 불러오기 — 스위치 UI를 쓰지 않아도 로컬 유지 필요 시 사용
        val sp = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        sp.getBoolean(keyDark, false)
    }

    // 편집 화면에서 돌아왔을 때도 최신 상태로 보이도록
    override fun onResume() {
        super.onResume()
        profileViewModel.loadProfileAll()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
