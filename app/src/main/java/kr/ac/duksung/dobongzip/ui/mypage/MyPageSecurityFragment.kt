package kr.ac.duksung.dobongzip.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.duksung.dobongzip.databinding.FragmentSecurityChangeBinding

class MyPageSecurityFragment : Fragment() {

    private var _binding: FragmentSecurityChangeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        // 완료(변경하기)
        binding.btnChange.setOnClickListener {
            val current = binding.editCurrentPw.text?.toString().orEmpty()
            val newPw   = binding.editNewPw.text?.toString().orEmpty()
            val confirm = binding.editConfirmPw.text?.toString().orEmpty()

            if (!validate(current, newPw, confirm)) return@setOnClickListener

            // TODO: 서버 API 호출 (예: viewModel.changePassword(current, newPw))
            // 성공 시:
            toast("비밀번호가 변경되었습니다.")
            findNavController().popBackStack()
        }
    }

    private fun validate(current: String, newPw: String, confirm: String): Boolean {
        if (current.isBlank()) { toast("현재 비밀번호를 입력하세요."); return false }
        if (!isStrong(newPw))  { toast("영문+숫자 조합, 8자 이상으로 설정해주세요."); return false }
        if (newPw != confirm)  { toast("새 비밀번호가 일치하지 않습니다."); return false }
        if (current == newPw)  { toast("현재 비밀번호와 다른 비밀번호를 사용하세요."); return false }
        return true
    }

    private fun isStrong(pw: String): Boolean {
        val hasLetter = pw.any { it.isLetter() }
        val hasDigit  = pw.any { it.isDigit() }
        return pw.length >= 8 && hasLetter && hasDigit
    }

    private fun toast(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
