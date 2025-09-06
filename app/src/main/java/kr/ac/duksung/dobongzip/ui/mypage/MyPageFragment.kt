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

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyMageRealBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "app_prefs"
    private val keyDark = "dark_mode_on"
    private var suppressDarkListener = false

    // ✅ 전역 프로필 상태 (Activity 스코프)
    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyMageRealBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔄 전역 상태 구독
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->

                    // 이미지
                    if (state.uri == null) {
                        binding.profileImage.setImageResource(R.drawable.prf3)
                    } else {
                        Glide.with(this@MyPageFragment).load(state.uri).centerCrop().into(binding.profileImage)
                    }
                    // 닉네임/메일 (해당 id가 있는 경우)
                    binding.myname?.text = state.nickname ?: "입맛까다로운햄스터"
                    binding.mymail?.text = state.email ?: "dobongzip@gmail.com"
                }
            }
        }


        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        val sp = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isDarkSaved = sp.getBoolean(keyDark, false)
        // (다크 모드 스위치는 현재 미사용 로직 그대로 유지)

        binding.privacySettingCard.setOnClickListener {
            findNavController().navigate(R.id.myPageSetFragment)
        }
        binding.securityCard.setOnClickListener {
            findNavController().navigate(R.id.securityFragment)
        }
        binding.supportCard.setOnClickListener {
            findNavController().navigate(R.id.supportFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        suppressDarkListener = true
        _binding = null
    }
}
