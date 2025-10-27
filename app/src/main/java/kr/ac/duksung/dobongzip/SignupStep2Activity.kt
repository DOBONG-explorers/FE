// kr/ac/duksung/dobongzip/SignupStep2Activity.kt
package kr.ac.duksung.dobongzip

import android.app.DatePickerDialog
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.ProfileRequest
import kr.ac.duksung.dobongzip.data.local.TokenStore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.HttpException
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

class SignupStep2Activity : AppCompatActivity() {

    private lateinit var nicknameEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var birthdateEditText: EditText
    private lateinit var completeSignupButton: Button
    private lateinit var progressImage: ImageView
    private lateinit var progressBar: ProgressBar

    private lateinit var tokenStore: TokenStore

    // 프로필 이미지 미리보기 대상
    private lateinit var ivProfile: ImageView
    // "프로필 수정" 버튼(TextView)
    private lateinit var tvEditProfile: TextView

    // 갤러리에서 이미지 선택
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { handlePickedImage(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step2)

        tokenStore = TokenStore(this)

        // View 연결
        nicknameEditText = findViewById(R.id.editTextNickname)
        ageEditText = findViewById(R.id.editTextAge) // 서버에는 안보내지만 클라이언트 표시용
        genderSpinner = findViewById(R.id.spinnerGender)
        birthdateEditText = findViewById(R.id.editTextBirthdate)
        completeSignupButton = findViewById(R.id.completeSignupButton)
        progressImage = findViewById(R.id.progressImage)
        progressBar = findViewById(R.id.progressBar)

        // 추가된 id
        ivProfile = findViewById(R.id.ivProfile)       // ⬅ XML에서 추가 필요
        tvEditProfile = findViewById(R.id.tvEditProfile) // ⬅ XML에서 추가 필요

        setupGenderSpinner()
        setupBirthdatePicker()
        setupValidation()

        // 프로필 이미지 수정(선택) 버튼
        tvEditProfile.setOnClickListener {
            pickImage.launch("image/*")
        }

        // 프로필 제출
        completeSignupButton.setOnClickListener {
            submitProfile()
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = listOf("성별 선택", "여성", "남성")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, genderOptions)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        genderSpinner.adapter = adapter
    }

    private fun setupBirthdatePicker() {
        birthdateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, y, m, d ->
                val birthdate = String.format("%04d-%02d-%02d", y, m + 1, d)
                birthdateEditText.setText(birthdate)
                validateInputs()
            }, year, month, day).show()
        }
    }

    private fun setupValidation() {
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = validateInputs()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        nicknameEditText.addTextChangedListener(watcher)
        ageEditText.addTextChangedListener(watcher)
        birthdateEditText.addTextChangedListener(watcher)
        genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                validateInputs()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun validateInputs() {
        val nickname = nicknameEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val birthdate = birthdateEditText.text.toString().trim()
        val genderSelected = genderSpinner.selectedItemPosition != 0

        val isAllValid = nickname.isNotEmpty() && age.isNotEmpty() && birthdate.isNotEmpty() && genderSelected

        completeSignupButton.isEnabled = isAllValid
        completeSignupButton.setBackgroundResource(
            if (isAllValid) R.drawable.rounded_button_blue else R.drawable.rounded_button_gray
        )

        if (isAllValid) {
            progressBar.progress = 100
            progressImage.setImageResource(R.drawable.re_3)
        }
    }

    /** 갤러리에서 선택된 이미지를 미리보기 & 서버 업로드 */
    private fun handlePickedImage(uri: Uri) {
        // 미리보기
        ivProfile.setImageURI(uri)

        // 업로드 진행
        lifecycleScope.launch {
            val part = withContext(Dispatchers.IO) { makeImagePartFromUri(uri, "file") }
            if (part == null) {
                toast("이미지 처리에 실패했습니다.")
                return@launch
            }

            // 버튼/문구 잠깐 비활성화
            val originalText = tvEditProfile.text
            tvEditProfile.isEnabled = false
            tvEditProfile.text = "업로드 중..."

            try {
                val res = ApiClient.authService.uploadProfileImage(part)
                if (res.success) {
                    toast("프로필 이미지가 업로드되었습니다.")
                    // 서버에서 이미지 URL을 내려주면 Glide로 로드 (선택)
                    res.data?.let { url ->
                        Glide.with(this@SignupStep2Activity)
                            .load(url)
                            .into(ivProfile)
                    }
                } else {
                    toast(res.message.ifBlank { "이미지 업로드 실패" })
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is HttpException -> "업로드 오류(${e.code()}): ${e.message()}"
                    else -> "업로드 오류: ${e.message}"
                }
                toast(msg)
            } finally {
                tvEditProfile.isEnabled = true
                tvEditProfile.text = originalText
            }
        }
    }

    /** Uri → MultipartBody.Part 변환 (캐시 임시파일로 복사) */
    private fun makeImagePartFromUri(uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val cr: ContentResolver = contentResolver
            val mime = cr.getType(uri) ?: "image/jpeg"
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime) ?: "jpg"

            val input = cr.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("profile_", ".$ext", cacheDir).apply { deleteOnExit() }
            FileOutputStream(tempFile).use { out -> input.copyTo(out) }

            val reqBody = RequestBody.create(mime.toMediaTypeOrNull(), tempFile)
            MultipartBody.Part.createFormData(partName, "profile.$ext", reqBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** 프로필 정보 제출 */
    private fun submitProfile() {
        val nickname = nicknameEditText.text.toString().trim()
        val birth = birthdateEditText.text.toString().trim()
        val genderSpinnerText = genderSpinner.selectedItem.toString()
        val gender = when (genderSpinnerText) {
            "여성" -> "FEMALE"
            "남성" -> "MALE"
            else -> "UNKNOWN"
        }

        lifecycleScope.launch {
            try {
                val email = tokenStore.getSignupEmail() ?: run {
                    toast("이메일 정보를 찾을 수 없습니다. 처음부터 다시 진행해주세요.")
                    return@launch
                }

                val req = ProfileRequest(
                    name = nickname,          // name 입력칸이 없으므로 임시로 nickname 사용
                    nickname = nickname,
                    gender = gender,
                    birth = birth
                )

                val res = ApiClient.authService.submitProfile(
                    email = email,
                    loginType = "APP",
                    body = req
                )

                if (res.success) {
                    toast("프로필이 저장되었습니다.")
                    startActivity(Intent(this@SignupStep2Activity, LoginActivity::class.java))
                    finish()
                } else {
                    toast(res.message.ifBlank { "프로필 저장 실패" })
                }
            } catch (e: Exception) {
                val msg = when (e) {
                    is HttpException -> {
                        when (e.code()) {
                            400 -> "요청 형식이 올바르지 않습니다."
                            401 -> "인증 정보가 없습니다."
                            409 -> "이미 등록된 프로필입니다."
                            500 -> "서버 오류가 발생했습니다."
                            else -> "오류(${e.code()}): ${e.message()}"
                        }
                    }
                    else -> "네트워크 오류: ${e.message}"
                }
                toast(msg)
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
