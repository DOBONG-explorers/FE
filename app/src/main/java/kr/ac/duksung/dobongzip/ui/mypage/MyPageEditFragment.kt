package kr.ac.duksung.dobongzip.ui.mypage

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import android.webkit.MimeTypeMap

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.databinding.FragmentMyPage2Binding
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel
import kr.ac.duksung.dobongzip.ui.common.loadProfile

// ▼ API 호출에 필요한 것들
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.MyPageProfilePatchReq
import kr.ac.duksung.dobongzip.data.api.ImageObjectKey

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

        // ✅ 완료 버튼: 텍스트 저장 + 이미지 업로드 2단계(필요 시)
        binding.myPageButton.setOnClickListener {
            onClickSave()
        }
    }

    /** 저장(완료) 버튼 클릭 처리: 텍스트 저장 → 이미지 업로드(임시→최종) */
    private fun onClickSave() {
        val current = profileViewModel.profileState.value

        val newName  = (pendingNickname ?: binding.editNickname.text?.toString())?.trim()
        val newBirth = (pendingBirthday ?: binding.editBirthday.text?.toString())?.trim()
        val newEmail = (pendingEmail ?: binding.editEmail.text?.toString())?.trim()
        val changedText = (newName != current.nickname) || (newBirth != current.birthday) || (newEmail != current.email)

        val changedImage = pendingUri != null && pendingUri != current.uri

        // 버튼 잠금
        val btn = binding.myPageButton
        val originalText = btn.text
        btn.isEnabled = false
        btn.text = "저장 중..."

        viewLifecycleOwner.lifecycleScope.launch {
            var textOk = true
            var imageOk = true

            // 1) 텍스트 저장 (변경 시)
            if (changedText) {
                textOk = saveTextProfile(newName, newBirth, newEmail)
            }

            // 2) 이미지 업로드 2단계 (변경 시)
            if (changedImage) {
                imageOk = uploadImageTwoSteps(pendingUri!!)
            }

            if (textOk && imageOk) {
                Toast.makeText(requireContext(), "정보가 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                // 성공 시 펜딩 초기화
                pendingUri = null
                pendingNickname = null
                pendingBirthday = null
                pendingEmail = null
                // 최신값 다시 로드 (보기 화면에서도 즉시 반영되게)
                profileViewModel.loadProfileAll()
            }

            // 버튼 복구
            btn.isEnabled = true
            btn.text = originalText
            syncEnableState()
        }
    }

    /** 텍스트 프로필 저장 */
    private suspend fun saveTextProfile(nickname: String?, birth: String?, email: String?): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val res = ApiClient.myPageService.patchProfile(
                    MyPageProfilePatchReq(nickname, birth, email)
                )
                res.success
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "프로필 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                false
            }
        }
    }

    /** 이미지 업로드 2단계(임시 업로드 → objectKey → 최종 반영) */
    private suspend fun uploadImageTwoSteps(uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // 1) Uri → Multipart 변환
                val part = makeImagePartFromUri(uri, "file") ?: run {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "이미지 처리 실패", Toast.LENGTH_SHORT).show()
                    }
                    return@withContext false
                }

                // 2) 1단계: 임시 업로드 → objectKey
                val step1 = ApiClient.myPageService.uploadProfileImageStage1(part)
                if (step1.success != true || step1.data?.objectKey.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), step1.message.ifBlank { "이미지 업로드 실패" }, Toast.LENGTH_SHORT).show()
                    }
                    return@withContext false
                }

                val objectKey = step1.data!!.objectKey

                // 3) 2단계: 최종 반영 → imageUrl
                val step2 = ApiClient.myPageService.finalizeProfileImage(ImageObjectKey(objectKey))
                if (step2.success != true) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), step2.message.ifBlank { "이미지 반영 실패" }, Toast.LENGTH_SHORT).show()
                    }
                    return@withContext false
                }

                // 미리보기는 이미 로컬 uri로 되어있지만, 서버 URL도 상태에 반영되도록 ViewModel 갱신
                withContext(Dispatchers.Main) {
                    profileViewModel.updateProfileUri(null) // 로컬 미리보기 제거
                    // 서버에서 최신 이미지 로드
                    profileViewModel.loadProfileAll()
                }
                true
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "이미지 업로드 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                false
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
    }

    private fun simpleWatcher(onChanged: (String) -> Unit) = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            onChanged(s?.toString() ?: "")
        }
        override fun afterTextChanged(s: Editable?) {}
    }

    /** Uri → MultipartBody.Part 변환 (임시파일 사용) */
    private suspend fun makeImagePartFromUri(uri: Uri, partName: String): MultipartBody.Part? {
        return withContext(Dispatchers.IO) {
            try {
                val cr = requireContext().contentResolver
                val mime = cr.getType(uri) ?: "image/jpeg"
                val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

                val input = cr.openInputStream(uri) ?: return@withContext null
                val tempFile = File.createTempFile("profile_", ".$ext", requireContext().cacheDir).apply {
                    deleteOnExit()
                }
                FileOutputStream(tempFile).use { out -> input.copyTo(out) }

                val reqBody = RequestBody.create(mime.toMediaTypeOrNull(), tempFile)
                MultipartBody.Part.createFormData(partName, "profile.$ext", reqBody)
            } catch (e: Exception) {
                null
            }
        }
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
