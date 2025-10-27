// kr/ac/duksung/dobongzip/SignupActivity.kt
package kr.ac.duksung.dobongzip

import android.content.Intent
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
import kr.ac.duksung.dobongzip.data.local.TokenStore

class SignupActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var nextButton: Button

    private lateinit var tokenStore: TokenStore

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

        // 다음 스텝(= 회원가입 API 호출 후)으로 이동
        nextButton.setOnClickListener {
            if (!isValidInput()) return@setOnClickListener

            val email = emailEditText.text.toString().trim()
            val pw = passwordEditText.text.toString()
            val phone = phoneEditText.text.toString().trim()

            nextButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val res = ApiClient.authService.signup(
                        SignupRequest(email = email, password = pw, phoneNumber = phone)
                    )

                    if (res.success) {
                        val accessToken = res.data?.accessToken.orEmpty()
                        if (accessToken.isNotEmpty()) tokenStore.saveAccessToken(accessToken)
                        tokenStore.saveSignupEmail(email) // step2에서 email 필요

                        // profileCompleted 가 false면 프로필 입력으로, true면 바로 로그인 화면/메인으로
                        val profileCompleted = res.data?.profileCompleted == true
                        if (profileCompleted) {
                            Toast.makeText(this@SignupActivity, "회원가입 완료!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                            finish()
                        } else {
                            // Step2로 이동
                            val intent = Intent(this@SignupActivity, SignupStep2Activity::class.java)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(
                            this@SignupActivity,
                            res.message.ifBlank { "회원가입 실패" },
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SignupActivity, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    nextButton.isEnabled = true
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
        val isValid = isValidInput()
        nextButton.isEnabled = isValid
        nextButton.setBackgroundResource(
            if (isValid) R.drawable.rounded_button_blue
            else R.drawable.rounded_button_gray
        )
    }

    private fun isValidInput(): Boolean {
        val email = emailEditText.text.toString().trim()
        val pw = passwordEditText.text.toString()
        val confirm = confirmPasswordEditText.text.toString()
        val phone = phoneEditText.text.toString().trim()

        return email.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                pw.length >= 6 &&
                pw == confirm &&
                phone.isNotEmpty()
    }
}
