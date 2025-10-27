package kr.ac.duksung.dobongzip.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.databinding.FragmentSecurityChangeBinding

import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.widget.EditText
import android.widget.ImageButton
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel

class MyPageSecurityFragment : Fragment() {

    private var _binding: FragmentSecurityChangeBinding? = null
    private val binding get() = _binding!!

    // ✅ /api/v1/mypage/password 호출용
    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ 눈 토글 연결 (각 입력칸 + 아이콘)
        wirePasswordToggle(binding.editCurrentPw, binding.btnToggleCurrent)
        wirePasswordToggle(binding.editNewPw, binding.btnToggleNew)
        wirePasswordToggle(binding.editConfirmPw, binding.btnToggleConfirm)

        // 뒤로가기
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        // 완료(변경하기)
        binding.btnChange.setOnClickListener {
            val current = binding.editCurrentPw.text?.toString().orEmpty()
            val newPw   = binding.editNewPw.text?.toString().orEmpty()
            val confirm = binding.editConfirmPw.text?.toString().orEmpty()

            if (!validate(current, newPw, confirm)) return@setOnClickListener

            // 버튼 잠금 + 진행 표시
            val origin = binding.btnChange.text
            binding.btnChange.isEnabled = false
            binding.btnChange.text = "변경 중..."

            viewLifecycleOwner.lifecycleScope.launch {
                profileViewModel.changePassword(current, newPw, confirm) { ok, msg ->
                    if (ok) {
                        toast("비밀번호가 변경되었습니다.")
                        // 입력칸 초기화
                        binding.editCurrentPw.setText("")
                        binding.editNewPw.setText("")
                        binding.editConfirmPw.setText("")
                        findNavController().popBackStack()
                    } else {
                        toast(msg ?: "변경에 실패했습니다.")
                    }
                    binding.btnChange.isEnabled = true
                    binding.btnChange.text = origin
                }
            }
        }
    }

    private fun validate(current: String, newPw: String, confirm: String): Boolean {
        if (current.isBlank()) { toast("현재 비밀번호를 입력하세요."); return false }
        if (!isStrong(newPw))  { toast("영문+숫자 조합, 8자 이상으로 설정해주세요."); return false }
        if (newPw != confirm)  { toast("새 비밀번호가 일치하지 않습니다."); return false }
        if (current == newPw)  { toast("현재 비밀번호와 다른 비밀번호를 사용하세요."); return false }
        return true
    }

    /** 최소 8자 & 영문/숫자 모두 포함 */
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

/** 👁️ 눈 아이콘 토글 공통 로직 */
private fun wirePasswordToggle(edit: EditText, btn: ImageButton) {
    var visible = false // 기본: 숨김
    btn.setOnClickListener {
        visible = !visible
        val cursor = edit.selectionStart
        edit.transformationMethod =
            if (visible) HideReturnsTransformationMethod.getInstance()
            else PasswordTransformationMethod.getInstance()
        btn.setImageResource(if (visible) R.drawable.ic_eye else R.drawable.ic_eye_off)
        edit.setSelection(if (cursor >= 0) cursor else edit.text?.length ?: 0)
    }
}
