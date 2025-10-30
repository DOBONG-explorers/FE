// kr/ac/duksung/dobongzip/SignupStep2Activity.kt
package kr.ac.duksung.dobongzip

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.ProfileRequest
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import kr.ac.duksung.dobongzip.data.local.TokenStore
import kr.ac.duksung.dobongzip.signup.uploadProfileImageAfterSignup
import retrofit2.HttpException
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

    // 선택된 이미지 Uri (제출 시 업로드에 사용)
    private var selectedImageUri: Uri? = null

    // 갤러리에서 이미지 선택
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) handlePickedImage(uri)
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

        // XML에 ivProfile, tvEditProfile 존재해야 함
        ivProfile = findViewById(R.id.ivProfile)
        tvEditProfile = findViewById(R.id.tvEditProfile)

        setupGenderSpinner()
        setupBirthdatePicker()
        setupValidation()

        // 프로필 이미지 선택
        tvEditProfile.setOnClickListener { pickImage.launch("image/*") }

        // “완료” 클릭 → [이미지 업로드 2단계] → [프로필 저장] 모두 수행
        completeSignupButton.setOnClickListener {
            submitAllAndFinish()
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

    /** 이미지 선택: 미리보기 + Uri 저장(제출 시 업로드) */
    private fun handlePickedImage(uri: Uri) {
        selectedImageUri = uri
        ivProfile.setImageURI(uri)
        toast("이미지를 선택했습니다. 완료 버튼을 눌러 저장하세요.")
    }

    /** 이미지 + 프로필을 모두 서버에 저장하고 완료 처리 */
    private fun submitAllAndFinish() {
        val nickname = nicknameEditText.text.toString().trim()
        val birth = birthdateEditText.text.toString().trim()
        val genderSpinnerText = genderSpinner.selectedItem.toString()
        val gender = when (genderSpinnerText) {
            "여성" -> "FEMALE"
            "남성" -> "MALE"
            else -> "UNKNOWN"
        }

        if (!completeSignupButton.isEnabled) return
        val originalText = completeSignupButton.text
        completeSignupButton.isEnabled = false
        completeSignupButton.text = "저장 중..."

        lifecycleScope.launch {
            try {
                // 0) 토큰 확인
                val token = TokenHolder.accessToken
                if (token.isNullOrBlank()) {
                    toast("인증 토큰이 없습니다. 다시 로그인해주세요.")
                    return@launch
                }

                // 1) (선택) 이미지가 있으면 2단계 업로드 수행: upload → finalize
                selectedImageUri?.let { uri ->
                    uploadProfileImageAfterSignup(this@SignupStep2Activity, uri)
                }

                // 2) 프로필 정보 저장
                val email = tokenStore.getSignupEmail() ?: run {
                    toast("이메일 정보를 찾을 수 없습니다. 처음부터 다시 진행해주세요.")
                    return@launch
                }

                val req = ProfileRequest(
                    name = nickname,          // name 입력칸이 없으므로 닉네임 사용
                    nickname = nickname,
                    gender = gender,
                    birth = birth
                )

                val res = ApiClient.authService.submitProfile(
                    email = email,
                    loginType = "APP",
                    body = req
                )

                if (!res.success) {
                    toast(res.message.ifBlank { "프로필 저장 실패" })
                    return@launch
                }

                // 3) (선택) 서버 최종 이미지 URL 재조회해 미리보기 반영
                runCatching { ApiClient.myPageService.getProfileImage() }
                    .getOrNull()
                    ?.data
                    ?.imageUrl
                    ?.takeIf { it?.isNotBlank() == true }
                    ?.let { url ->
                        Glide.with(this@SignupStep2Activity).load(url).into(ivProfile)
                    }

                toast("모든 정보가 저장되었습니다.")
                startActivity(Intent(this@SignupStep2Activity, LoginActivity::class.java))
                finish()

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
            } finally {
                completeSignupButton.isEnabled = true
                completeSignupButton.text = originalText
            }
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
