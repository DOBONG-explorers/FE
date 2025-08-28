package kr.ac.duksung.dobongzip.ui.mypage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentMyMageRealBinding

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyMageRealBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "app_prefs"
    private val keyDark = "dark_mode_on"

    // 초기 isChecked 세팅 시 콜백이 불지 않도록 막는 플래그 (현재 다크모드 스위치 미사용)
    private var suppressDarkListener = false

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

        binding.backButton.setOnClickListener {
            findNavController().popBackStack()
        }

        val sp = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isDarkSaved = sp.getBoolean(keyDark, false)

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
