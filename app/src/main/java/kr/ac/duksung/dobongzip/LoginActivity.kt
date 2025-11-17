package kr.ac.duksung.dobongzip

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.user.UserApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.IdTokenRequest
import kr.ac.duksung.dobongzip.data.api.LoginRequest
import kr.ac.duksung.dobongzip.data.auth.AuthSession
import kr.ac.duksung.dobongzip.data.auth.TokenHolder
import kr.ac.duksung.dobongzip.data.local.TokenStore

class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGuest: TextView
    private lateinit var tvFindPw: TextView
    private lateinit var tvSignup: TextView

    //  소셜 로그인을 위한 아이콘
    private lateinit var iconKakao: ImageView
    private lateinit var iconGoogle: ImageView

    private lateinit var tokenStore: TokenStore
    private val TAG = "Login"

    // ✅ 소셜 로그인으로 받은 이름/이메일 임시 저장용
    private var socialName: String? = null
    private var socialEmail: String? = null

    // 공개 클라이언트 사용(Authorization 미첨부)
    private val authService by lazy { ApiClient.authServicePublic }

    //  Google Sign-In
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleLoginLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // 로그인 결과가 OK인지 확인
            if (result.resultCode != Activity.RESULT_OK) {
                Log.e(TAG, "Google Sign-In was canceled.")
                showError("구글 로그인이 취소되었습니다.")
                return@registerForActivityResult
            }

            // 로그인 작업이 성공했을 때
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)

                // ✅ 구글 계정 이름/이메일 가져오기
                socialName = account?.displayName
                socialEmail = account?.email
                Log.d(TAG, "Google account: name=$socialName, email=$socialEmail")

                // ID Token을 가져왔는지 확인
                val idToken = account?.idToken
                Log.d(TAG, "Google Sign-In successful. ID Token: $idToken")

                // ID Token이 없으면 오류 처리
                if (idToken.isNullOrBlank()) {
                    Log.e(TAG, "Google ID Token is null or empty.")
                    showError("구글 id_token을 가져오지 못했습니다.")
                    return@registerForActivityResult
                }

                // ID Token을 서버로 전송
                loginWithGoogleOnServer(idToken)

            } catch (e: ApiException) {
                // ApiException 발생 시 오류 로그
                Log.e(TAG, "Google sign-in failed: ${e.statusCode}", e)
                showError("구글 로그인 실패: ${e.message}")
            }
        }

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
        Log.d("LoginActivity", "tvGuest: $tvGuest")

        //  아이콘 연결
        iconKakao = findViewById(R.id.icon1)
        iconGoogle = findViewById(R.id.icon2)

        //  Google Sign-In 설정 (웹 클라이언트 ID 사용)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_web_client_id))  // 웹 클라이언트 ID
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // ───────────── 이메일 / 비밀번호 로그인 ─────────────
        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pw = etPassword.text.toString().trim()

            Log.d(TAG, "click login: emailLen=${email.length}, pwLen=${pw.length}")

            if (email.isEmpty() || pw.isEmpty()) {
                showError("이메일/비밀번호를 입력하세요.")
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            clearError()

            lifecycleScope.launch {
                try {
                    val res = authService.login(LoginRequest(email, pw))
                    Log.d(TAG, "resp: success=${res.success}, msg=${res.message}")

                    if (!res.success) {
                        showError(res.message.ifBlank { "이메일 또는 비밀번호가 올바르지 않습니다." })
                        return@launch
                    }

                    //  서버 스펙: accessToken 또는 token 중 하나에 JWT가 들어옴 → jwt() 헬퍼로 통일
                    val jwt = res.data?.jwt().orEmpty()
                    if (jwt.isBlank()) {
                        showError("토큰 발급에 실패했습니다.")
                        return@launch
                    }

                    //  토큰 저장 (DataStore + 메모리 + AuthSession)
                    TokenHolder.accessToken = null
                    tokenStore.saveAccessToken(jwt)
                    TokenHolder.accessToken = jwt
                    AuthSession.setToken(jwt)

                    // ✅ 일반 로그인 = 회원 로그인
                    TokenHolder.isLoggedIn = true
                    saveSocialProfileIfNeeded()
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()

                    val profileCompleted = res.data?.profileCompleted == true
                    if (profileCompleted) {
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    } else {
                        tokenStore.saveSignupEmail(email)
                        startActivity(
                            Intent(
                                this@LoginActivity,
                                SignupStep2Activity::class.java
                            )
                        )
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

        val tvGuest = findViewById<TextView>(R.id.guest_login)
        tvGuest.setOnClickListener {
            Log.d("LoginActivity", "비회원 로그인 클릭됨") // 로그 추가
            Log.d(
                "LoginActivity",
                "tvGuest isClickable: ${tvGuest.isClickable}, tvGuest isFocusable: ${tvGuest.isFocusable}"
            )

            TokenHolder.isLoggedIn = false  // 비회원 상태로 설정
            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()  // LoginActivity 종료
        }

        tvFindPw.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    kr.ac.duksung.dobongzip.ui.password.PasswordResetActivity::class.java
                )
            )
        }

        tvSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        // ───────────── 카카오 로그인 아이콘 ─────────────
        iconKakao.setOnClickListener {
            loginWithKakao()
        }

        // ─────────────  구글 로그인 아이콘 ─────────────
        iconGoogle.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLoginLauncher.launch(signInIntent)
        }
    }

    // ───────────────── 카카오 로그인 로직 ─────────────────
    private fun loginWithKakao() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "Kakao login failed", error)
                showError("카카오 로그인 실패: ${error.message}")
            } else if (token == null) {
                showError("카카오 토큰이 비어 있습니다.")
            } else {
                val idToken = token.idToken
                Log.d(TAG, "Kakao idToken: $idToken")  // idToken 로그

                if (idToken.isNullOrBlank()) {
                    Log.e(TAG, "Kakao id_token을 가져오지 못했습니다.")
                    showError("카카오 id_token을 가져오지 못했습니다. (scope에 openid 포함 여부 확인)")
                } else {

                    // ✅ 카카오 계정 정보 조회 (닉네임/이메일)
                    UserApiClient.instance.me { user, meError ->
                        if (meError != null) {
                            Log.e(TAG, "Kakao me() failed", meError)
                        } else {
                            socialName = user?.kakaoAccount?.profile?.nickname
                            socialEmail = user?.kakaoAccount?.email
                            Log.d(TAG, "Kakao account: name=$socialName, email=$socialEmail")
                        }
                    }

                    // 카카오 id_token → 서버로 전송
                    loginWithKakaoOnServer(idToken)
                    Log.d(TAG, "Sending kakao idToken to the server")
                }
            }
        }

        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            Log.d(TAG, "KakaoTalk login available. Trying to login with KakaoTalk.")
            UserApiClient.instance.loginWithKakaoTalk(this, callback = callback)
        } else {
            Log.d(TAG, "KakaoTalk login not available. Trying to login with KakaoAccount.")
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun loginWithKakaoOnServer(idToken: String) {
        lifecycleScope.launch {
            try {
                val res = authService.kakaoOidc(IdTokenRequest(idToken = idToken))
                Log.d(TAG, "kakaoOidc resp: success=${res.success}, msg=${res.message}")

                if (!res.success) {
                    showError(res.message.ifBlank { "카카오 로그인에 실패했습니다." })
                    return@launch
                }

                val jwt = res.data?.jwt().orEmpty()
                if (jwt.isBlank()) {
                    showError("서버에서 JWT를 받지 못했습니다.")
                    return@launch
                }

                //  토큰 저장
                TokenHolder.accessToken = null
                tokenStore.saveAccessToken(jwt)
                TokenHolder.accessToken = jwt
                AuthSession.setToken(jwt)

                // ✅ 소셜 로그인도 회원 로그인
                TokenHolder.isLoggedIn = true

                // ✅ 소셜 프로필(별명/이메일) 마이페이지용으로 저장
                saveSocialProfileIfNeeded()

                // ✅ 카카오 로그인은 바로 메인으로 이동 (비회원 X)
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: HttpException) {
                val body = e.response()?.errorBody()?.string()
                Log.e(TAG, "kakaoOidc HttpException: code=${e.code()} body=$body", e)
                showError("카카오 로그인 서버 오류(${e.code()})")
            } catch (e: Exception) {
                Log.e(TAG, "kakaoOidc Exception: ${e.message}", e)
                showError("네트워크 오류: ${e.message}")
            }
        }
    }

    // ───────────────── 구글 로그인 로직 ─────────────────
    private fun loginWithGoogleOnServer(idToken: String) {
        Log.d(TAG, "Sending Google ID Token to the server: $idToken")  // 로그 추가

        lifecycleScope.launch {
            try {
                // 서버에 idToken을 보내 로그인 요청
                val res = authService.googleOidc(IdTokenRequest(idToken = idToken))
                Log.d(TAG, "googleOidc response: success=${res.success}, msg=${res.message}")

                // 성공적으로 로그인 되면 JWT 처리
                if (!res.success) {
                    showError(res.message.ifBlank { "구글 로그인에 실패했습니다." })
                    return@launch
                }

                val jwt = res.data?.jwt().orEmpty()
                if (jwt.isBlank()) {
                    showError("서버에서 JWT를 받지 못했습니다.")
                    return@launch
                }

                // 토큰 저장 후 화면 전환
                TokenHolder.accessToken = null
                tokenStore.saveAccessToken(jwt)
                TokenHolder.accessToken = jwt
                AuthSession.setToken(jwt)

                // ✅ 소셜 로그인도 회원 로그인
                TokenHolder.isLoggedIn = true

                // ✅ 소셜 프로필(별명/이메일) 마이페이지용으로 저장
                saveSocialProfileIfNeeded()

                // ✅ 구글 로그인도 바로 메인으로 이동
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()

            } catch (e: HttpException) {
                Log.e(TAG, "googleOidc HttpException: ${e.message}", e)
                showError("구글 로그인 서버 오류: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception: ${e.message}", e)
                showError("네트워크 오류: ${e.message}")
            }
        }
    }

    private fun saveSocialProfileIfNeeded() {
        val email = socialEmail
        val name = socialName

        // 이메일은 기존에 쓰던 TokenStore에도 저장해두기 (코루틴으로 감싸기)
        if (!email.isNullOrBlank()) {
            lifecycleScope.launch {
                tokenStore.saveSignupEmail(email)
            }
        }

        // 마이페이지에서 쓸 프로필 정보 로컬에 저장
        val sp = getSharedPreferences("user_profile", MODE_PRIVATE)
        sp.edit()
            .putString("nickname", name)
            .putString("email", email)
            .apply()

        Log.d(TAG, "Saved social profile to SharedPreferences: name=$name, email=$email")
    }



    // ✅ 에러 표시
    private fun showError(msg: String) {
        // 토스트로 에러 메시지 표시
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

        // tvError TextView에 애니메이션으로 에러 메시지 표시
        findViewById<TextView>(R.id.tvError)?.apply {
            text = msg
            alpha = 0f
            isVisible = true
            animate().alpha(1f).setDuration(150).start()
        }
    }

    // ✅ 에러 메시지 숨기기
    private fun clearError() {
        findViewById<TextView>(R.id.tvError)?.isVisible = false
    }
}
