package kr.ac.duksung.dobongzip.ui.mypage

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
import kr.ac.duksung.dobongzip.databinding.FragmentMyPageBinding
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel

class MyPageSetFragment : Fragment() {

    private var _binding: FragmentMyPageBinding? = null
    private val binding get() = _binding!!

    // ✅ 전역 프로필 상태 (Activity 스코프)
    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyPageBinding.inflate(inflater, container, false)

        // "개인정보 수정" → 편집 화면 이동
        binding.myPageButton.setOnClickListener {
            findNavController().navigate(R.id.myPageEditFragment)
        }

        // ✅ 뒤로가기 버튼 (XML의 @+id/backButton 와 연결)
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 전역 상태 구독 → 이미지/텍스트 뷰 반영
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    // 프로필 이미지
                    if (state.uri == null) {
                        binding.profileImage.setImageResource(R.drawable.prf3)
                    } else {
                        Glide.with(this@MyPageSetFragment)
                            .load(state.uri)
                            .centerCrop()
                            .into(binding.profileImage)
                    }

                    // 닉네임/생년월일/이메일 (이 페이지는 TextView)
                    binding.editNickname.text = state.nickname ?: "홍길동"
                    binding.editBirthday.text = state.birthday ?: "1990-01-01"
                    binding.editEmail.text    = state.email ?: "user@example.com"
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
