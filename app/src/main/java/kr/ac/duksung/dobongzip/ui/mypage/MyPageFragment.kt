package kr.ac.duksung.dobongzip.ui.mypage

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.duksung.dobongzip.databinding.FragmentMyMageRealBinding

class MyPageFragment : Fragment() {

    private var _binding: FragmentMyMageRealBinding? = null
    private val binding get() = _binding!!

    private val prefsName = "app_prefs"
    private val keyDark = "dark_mode_on"

    // 초기 isChecked 세팅 시 콜백이 불지 않도록 막는 플래그
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
            // 테마 변경 직후 백스택 조작은 충돌이 날 수 있어요 -> 필요 없으면 제거 권장
            findNavController().popBackStack()
        }

        val sp = requireActivity().getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val isDarkSaved = sp.getBoolean(keyDark, false)



        // 다른 카드들 (XML에 반드시 해당 id가 있어야 합니다)
        binding.privacySettingCard.setOnClickListener { /* navigate if needed */ }
        binding.securityCard.setOnClickListener { /* navigate if needed */ }
        binding.supportCard.setOnClickListener { /* navigate if needed */ }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 뷰 파괴 이후 콜백이 접근하지 않도록 플래그 재설정(안전)
        suppressDarkListener = true
        _binding = null
    }
}
