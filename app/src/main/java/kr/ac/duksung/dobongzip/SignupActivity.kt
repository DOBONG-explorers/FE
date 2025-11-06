package kr.ac.duksung.dobongzip

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.SignupRequest
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import kr.ac.duksung.dobongzip.data.local.TokenStore
import kr.ac.duksung.dobongzip.signup.uploadProfileImageAfterSignup
import retrofit2.HttpException

class SignupActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var nextButton: Button

    private lateinit var tokenStore: TokenStore

    // Step1에서 이미 이미지를 골랐다면 여기에 담아둔다 (기본 null)
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step1)

        tokenStore = TokenStore(this)

        // View 연결
        emailEditText = findViewById(R.id.editTextEmail)
        passwordEditText = findViewById(R.id.editTextPassword)
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword)
        phoneEditText = findViewById(R.id.editTextPhone)
        nextButton = findViewById(R.id.nextButton)

        // 텍스트 변경 리스너 적용
        applyTextWatcher(emailEditText)
        applyTextWatcher(passwordEditText)
        applyTextWatcher(confirmPasswordEditText)
        applyTextWatcher(phoneEditText)

        // 회원가입 → (이미지 있으면) 서버 업로드까지 완료 → 다음 스텝
        nextButton.setOnClickListener {
            if (!isValidInput(showToast = true)) return@setOnClickListener

            val email = emailEditText.text.toString().trim()
            val pw = passwordEditText.text.toString()
            val phone = sanitizePhone(phoneEditText.text.toString())

            nextButton.isEnabled = false
            val originalText = nextButton.text
            nextButton.text = "처리 중..."

            lifecycleScope.launch {
                try {
                    val res = ApiClient.authService.signup(
                        SignupRequest(email = email, password = pw, phoneNumber = phone)
                    )

                    if (res.success) {
                        val accessToken = res.data?.accessToken.orEmpty()
                        if (accessToken.isNotEmpty()) {
                            // 1) 토큰을 로컬/메모리에 저장 (Authorization 헤더 자동 부착용)
                            tokenStore.saveAccessToken(accessToken)
                            TokenHolder.accessToken = accessToken
                        }

                        // 2) Step2에서 이메일 필요하면 저장
                        tokenStore.saveSignupEmail(email)

                        // 3) 프로필이 이미 끝난 계정이면 바로 로그인 화면으로
                        val profileCompleted = res.data?.profileCompleted == true
                        if (profileCompleted) {
                            Toast.makeText(this@SignupActivity, "회원가입 완료!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                            return@launch
                        }

                        // 4) ✨ 이미지가 있다면 여기서 서버에 '확실히' 저장하고 넘어감
                        selectedImageUri?.let { uri ->
                            // Uri → Multipart → /upload → objectKey → /finalize (서버 저장 완료까지 대기)
                            uploadProfileImageAfterSignup(this@SignupActivity, uri)
                            Toast.makeText(this@SignupActivity, "프로필 이미지가 서버에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                        }

                        // 5) 다음 스텝으로 이동 (프로필 닉네임/생일/성별은 Step2에서 저장)
                        val intent = Intent(this@SignupActivity, SignupStep2Activity::class.java).apply {
                            // 선택 이미지를 Step2 미리보기에 보여주고 싶다면 전달
                            putExtra("profileUri", selectedImageUri?.toString())
                        }
                        startActivity(intent)

                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            res.message.ifBlank { "회원가입 실패" },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    val msg = when (e) {
                        is HttpException -> {
                            when (e.code()) {
                                400 -> "요청 형식이 올바르지 않습니다."
                                401 -> "인증 정보가 없습니다."
                                409 -> "이미 사용 중인 이메일입니다."
                                500 -> "서버 오류가 발생했습니다."
                                else -> "오류(${e.code()}): ${e.message()}"
                            }
                        }
                        else -> "네트워크 오류: ${e.message}"
                    }
                    Toast.makeText(this@SignupActivity, msg, Toast.LENGTH_LONG).show()
                } finally {
                    nextButton.isEnabled = true
                    nextButton.text = originalText
                }
            }
        }
    }

    private fun applyTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { updateButtonState() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun updateButtonState() {
        val isValid = isValidInput(showToast = false)
        nextButton.isEnabled = isValid
        nextButton.setBackgroundResource(
            if (isValid) R.drawable.rounded_button_blue
            else R.drawable.rounded_button_gray
        )
    }

    /** 입력값 유효성 검사 */
    private fun isValidInput(showToast: Boolean): Boolean {
        val email = emailEditText.text.toString().trim()
        val pw = passwordEditText.text.toString()
        val confirm = confirmPasswordEditText.text.toString()
        val phone = sanitizePhone(phoneEditText.text.toString())

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            if (showToast) toast("유효한 이메일을 입력하세요.")
            return false
        }
        if (!isValidPassword(pw)) {
            if (showToast) toast("비밀번호는 6~20자이며, 대/소문자/숫자/특수문자 중 2가지 이상을 포함해야 합니다.")
            return false
        }
        if (pw != confirm) {
            if (showToast) toast("비밀번호가 일치하지 않습니다.")
            return false
        }
        if (phone.isEmpty()) {
            if (showToast) toast("휴대폰 번호를 입력하세요.")
            return false
        }
        return true
    }

    /** 휴대폰 번호에서 숫자만 추출 */
    private fun sanitizePhone(raw: String): String =
        raw.filter { it.isDigit() }

    /** 6~20자 & (대문자/소문자/숫자/특수문자 중 2종 이상) */
    private fun isValidPassword(pw: String): Boolean {
        if (pw.length !in 6..20) return false
        var kinds = 0
        if (pw.any { it.isLowerCase() }) kinds++
        if (pw.any { it.isUpperCase() }) kinds++
        if (pw.any { it.isDigit() }) kinds++
        if (pw.any { "!@#\$%^&*()-_=+[]{}|;:'\",.<>?/`~\\".contains(it) }) kinds++
        return kinds >= 2
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}