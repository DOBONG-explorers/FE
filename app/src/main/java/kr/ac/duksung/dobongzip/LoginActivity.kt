package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.LoginRequest
import kr.ac.duksung.dobongzip.data.local.TokenStore
import kr.ac.duksung.dobongzip.data.auth.TokenHolder

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGuest: TextView
    private lateinit var tvFindPw: TextView
    private lateinit var tvSignup: TextView

    private lateinit var tokenStore: TokenStore
    private val TAG = "Login"

    // ✅ 공개 클라이언트 사용(Authorization 미첨부)
    private val authService by lazy { ApiClient.authServicePublic }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tokenStore = TokenStore(applicationContext)

        etEmail = findViewById(R.id.editTextEmail)
        etPassword = findViewById(R.id.editTextPassword)
        btnLogin = findViewById(R.id.login_button)
        tvGuest = findViewById(R.id.guest_login)
        tvFindPw = findViewById(R.id.find_id_pw)
        tvSignup = findViewById(R.id.signup_text)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw = etPassword.text.toString().trim() // ✅ 공백 제거

            Log.d(TAG, "click login: emailLen=${email.length}, pwLen=${pw.length}")

            if (email.isEmpty() || pw.isEmpty()) {
                showError("이메일/비밀번호를 입력하세요.")
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            clearError()

            lifecycleScope.launch {
                try {
                    // ✅ 공개 클라이언트로 로그인 요청
                    val res = authService.login(LoginRequest(email, pw))
                    Log.d(TAG, "resp: success=${res.success}, msg=${res.message}")

                    if (!res.success) {
                        showError(res.message.ifBlank { "이메일 또는 비밀번호가 올바르지 않습니다." })
                        return@launch
                    }

                    val accessToken = res.data?.accessToken.orEmpty()
                    if (accessToken.isBlank()) {
                        showError("토큰 발급에 실패했습니다.")
                        return@launch
                    }

                    // ✅ 로그인 성공 → 토큰 저장 (DataStore + 메모리)
                    // 기존 세션 정리 후 저장하는 것이 안전
                    TokenHolder.accessToken = null
                    tokenStore.saveAccessToken(accessToken)
                    TokenHolder.accessToken = accessToken

                    // 프로필 완료 여부에 따라 분기
                    val profileCompleted = res.data?.profileCompleted == true
                    if (profileCompleted) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        tokenStore.saveSignupEmail(email)
                        startActivity(Intent(this@LoginActivity, SignupStep2Activity::class.java))
                    }
                    finish()

                } catch (e: HttpException) {
                    val body = e.response()?.errorBody()?.string()
                    Log.e(TAG, "HttpException: code=${e.code()} body=$body", e)
                    when (e.code()) {
                        404 -> showError("가입된 계정을 찾을 수 없습니다.")
                        401 -> showError(body ?: "이메일 또는 비밀번호가 올바르지 않습니다.")
                        else -> showError("서버 오류(${e.code()})")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception: ${e.message}", e)
                    showError("네트워크 오류: ${e.message}")
                } finally {
                    btnLogin.isEnabled = true
                }
            }
        }

        tvGuest.setOnClickListener {
            // TODO 비회원 로직
        }
        tvFindPw.setOnClickListener {
            startActivity(Intent(this, kr.ac.duksung.dobongzip.ui.password.PasswordResetActivity::class.java))
        }
        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    // ✅ 토글 알림 + 화면 에러 라벨
    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        findViewById<TextView>(R.id.tvError)?.apply {
            text = msg
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(150).start()
        }
    }

    private fun clearError() {
        findViewById<TextView>(R.id.tvError)?.isVisible = false
    }
}
