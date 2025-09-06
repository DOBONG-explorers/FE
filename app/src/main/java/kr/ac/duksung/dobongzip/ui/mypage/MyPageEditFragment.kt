package kr.ac.duksung.dobongzip.ui.mypage

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.databinding.FragmentMyPage2Binding
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel
import kr.ac.duksung.dobongzip.ui.common.loadProfile

class MyPageEditFragment : Fragment() {

    private var _binding: FragmentMyPage2Binding? = null
    private val binding get() = _binding!!

    private val profileViewModel: ProfileViewModel by activityViewModels()

    // ▶ 완료 전까지 임시 보관할 값들
    private var pendingUri: Uri? = null
    private var pendingNickname: String? = null
    private var pendingBirthday: String? = null
    private var pendingEmail: String? = null

    companion object { private const val STATE_PENDING_URI = "state_pending_uri" }

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let { previewOnly(it) }
        }

    private val openDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                previewOnly(it)
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMyPage2Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        pendingUri = savedInstanceState?.getString(STATE_PENDING_URI)?.let { Uri.parse(it) }

        // ✅ 전역 상태 구독 → 초기값 표시 (펜딩이 있으면 펜딩 우선)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    val shownUri = pendingUri ?: state.uri
                    binding.profileImage.loadProfile(shownUri)

                    // EditText 초기값 (사용자가 이미 편집중이면 펜딩을 우선 표시)
                    if (pendingNickname == null) binding.editNickname.setText(state.nickname ?: "")
                    if (pendingBirthday == null) binding.editBirthday.setText(state.birthday ?: "")
                    if (pendingEmail == null)    binding.editEmail.setText(state.email ?: "")

                    updateDoneButtonEnabled(
                        candidateUri = pendingUri,
                        globalUri = state.uri,
                        candidateName = pendingNickname ?: binding.editNickname.text?.toString(),
                        globalName = state.nickname,
                        candidateBirth = pendingBirthday ?: binding.editBirthday.text?.toString(),
                        globalBirth = state.birthday,
                        candidateEmail = pendingEmail ?: binding.editEmail.text?.toString(),
                        globalEmail = state.email
                    )
                }
            }
        }

        // 뒤로가기
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        // 사진 선택
        binding.editProfileText.setOnClickListener { openPicker() }
        // binding.profileImage.setOnClickListener { openPicker() } // 원하면 사용

        // ✅ 텍스트 변경 감지 (완료 버튼 활성화 갱신)
        binding.editNickname.addTextChangedListener(simpleWatcher { s ->
            pendingNickname = s
            syncEnableState()
        })
        // 생년월일: yyyy-MM-dd 형식 강제 + 최대 10자
        binding.editBirthday.filters = arrayOf(android.text.InputFilter.LengthFilter(10))

        binding.editBirthday.addTextChangedListener(object : TextWatcher {
            private var isEditing = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isEditing) return
                isEditing = true

                val raw = s?.toString().orEmpty()
                val digits = raw.replace("-", "")           // 숫자만
                val builder = StringBuilder()

                // yyyy-MM-dd 포맷팅
                for (i in digits.indices) {
                    builder.append(digits[i])
                    if (i == 3 || i == 5) builder.append("-") // 4번째, 6번째 뒤에 '-'
                }
                var formatted = builder.toString()
                if (formatted.length > 10) formatted = formatted.substring(0, 10)

                if (formatted != raw) {
                    binding.editBirthday.setText(formatted)
                    binding.editBirthday.setSelection(formatted.length)
                }

                // ✅ 변경사항 반영: pendingBirthday 갱신 + 버튼 상태 갱신
                pendingBirthday = formatted
                syncEnableState()

                isEditing = false
            }
        })


        binding.editEmail.addTextChangedListener(simpleWatcher { s ->
            pendingEmail = s
            syncEnableState()
        })

        // ✅ 완료 버튼: 이때만 전역 저장 적용
        binding.myPageButton.setOnClickListener {
            val current = profileViewModel.profileState.value
            val changedImage   = pendingUri != null && pendingUri != current.uri
            val changedName    = (pendingNickname ?: binding.editNickname.text?.toString())?.trim() != (current.nickname ?: "")
            val changedBirth   = (pendingBirthday ?: binding.editBirthday.text?.toString())?.trim() != (current.birthday ?: "")
            val changedEmail   = (pendingEmail ?: binding.editEmail.text?.toString())?.trim() != (current.email ?: "")

            if (changedImage || changedName || changedBirth || changedEmail) {
                if (changedImage) profileViewModel.updateProfileUri(pendingUri)
                if (changedName)  profileViewModel.updateNickname((pendingNickname ?: binding.editNickname.text?.toString())?.trim())
                if (changedBirth) profileViewModel.updateBirthday((pendingBirthday ?: binding.editBirthday.text?.toString())?.trim())
                if (changedEmail) profileViewModel.updateEmail((pendingEmail ?: binding.editEmail.text?.toString())?.trim())

                Toast.makeText(requireContext(), "정보가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()

                // 펜딩 초기화
                pendingUri = null
                pendingNickname = null
                pendingBirthday = null
                pendingEmail = null

                // 즉시 뒤로 가고 싶다면:
                // findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "변경된 내용이 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openPicker() {
        try {
            pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } catch (_: Exception) {
            openDocumentLauncher.launch(arrayOf("image/*"))
        }
    }

    private fun previewOnly(uri: Uri) {
        pendingUri = uri
        binding.profileImage.loadProfile(uri)
        syncEnableState()
    }

    private fun syncEnableState() {
        val state = profileViewModel.profileState.value
        updateDoneButtonEnabled(
            candidateUri = pendingUri,
            globalUri = state.uri,
            candidateName = pendingNickname ?: binding.editNickname.text?.toString(),
            globalName = state.nickname,
            candidateBirth = pendingBirthday ?: binding.editBirthday.text?.toString(),
            globalBirth = state.birthday,
            candidateEmail = pendingEmail ?: binding.editEmail.text?.toString(),
            globalEmail = state.email
        )
    }

    private fun updateDoneButtonEnabled(
        candidateUri: Uri?, globalUri: Uri?,
        candidateName: String?, globalName: String?,
        candidateBirth: String?, globalBirth: String?,
        candidateEmail: String?, globalEmail: String?
    ) {
        val changed = (candidateUri != null && candidateUri != globalUri) ||
                ((candidateName ?: "").trim()  != (globalName ?: "")) ||
                ((candidateBirth ?: "").trim() != (globalBirth ?: "")) ||
                ((candidateEmail ?: "").trim() != (globalEmail ?: ""))
        binding.myPageButton.isEnabled = changed
        // binding.myPageButton.alpha = if (changed) 1f else 0.5f
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString() ?: "")
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        pendingUri?.let { outState.putString(STATE_PENDING_URI, it.toString()) }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
