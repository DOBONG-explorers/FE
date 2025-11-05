package kr.ac.duksung.dobongzip.ui.password

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import kr.ac.duksung.dobongzip.LoginActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.PasswordService
import kr.ac.duksung.dobongzip.data.api.ResetPasswordReq

class PasswordResetConfirmActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "PwResetConfirm"
    }

    private lateinit var service: PasswordService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")
        setContentView(R.layout.activity_password_reset_confirm)

        // 반드시 공개 클라이언트
        service = ApiClient.passwordServicePublic
        Log.d(TAG, "service created with PUBLIC retrofit: $service")

        val back = findViewById<ImageButton>(R.id.backButton)
        val etEmailReadOnly = findViewById<EditText>(R.id.etEmailReadOnly)
        val etPw = findViewById<EditText>(R.id.etNewPassword)
        val etPw2 = findViewById<EditText>(R.id.etNewPasswordConfirm)
        val btn = findViewById<Button>(R.id.btnPwResetConfirm)

        val email = intent.getStringExtra("email").orEmpty().trim()
        etEmailReadOnly.setText(email)
        Log.d(TAG, "email from intent='$email' (len=${email.length})")

        back.setOnClickListener { finish() }

        btn.setOnClickListener {
            // 여기에 trim() 적용 (요청 직전)
            val pw  = etPw.text.toString().trim()
            val pw2 = etPw2.text.toString().trim()

            val hasUpper = pw.any { it.isUpperCase() }
            val hasLower = pw.any { it.isLowerCase() }
            val hasDigit = pw.any { it.isDigit() }
            val hasSpecial = pw.any { !it.isLetterOrDigit() }

            Log.d(TAG, "click: start validate: pwLen=${pw.length}, pw2Len=${pw2.length}," +
                    " kinds=${listOf(hasUpper,hasLower,hasDigit,hasSpecial).count{it}}," +
                    " flags(U=$hasUpper L=$hasLower D=$hasDigit S=$hasSpecial)")

            // ─ 클라 검증 ─
            if (pw.isBlank()) { etPw.error = "비밀번호를 입력하세요"; Log.w(TAG, "validation: pw blank"); return@setOnClickListener }
            if (pw2.isBlank()) { etPw2.error = "비밀번호 확인을 입력하세요"; Log.w(TAG, "validation: pw2 blank"); return@setOnClickListener }
            if (pw != pw2)     { etPw2.error = "비밀번호가 일치하지 않습니다"; Log.w(TAG, "validation: mismatch"); return@setOnClickListener }
            if (pw.length !in 6..20) { etPw.error = "6~20자 범위로 입력하세요"; Log.w(TAG, "validation: length invalid"); return@setOnClickListener }

            val kinds = listOf(hasUpper, hasLower, hasDigit, hasSpecial).count { it }
            if (kinds < 2) {
                etPw.error = "영문 대/소문자, 숫자, 특수문자 중 2가지 이상 조합하세요"
                Log.w(TAG, "validation: kinds < 2")
                return@setOnClickListener
            }

            // ─ 서버 호출 ─
            lifecycleScope.launch {
                try {
                    Log.d(TAG, "call resetPassword: email=$email, pwLen=${pw.length}")
                    val resp = service.resetPassword(
                        ResetPasswordReq(
                            email = email,
                            newPassword = pw,
                            confirmPassword = pw2
                        )
                    )
                    Log.d(TAG, "resp: success=${resp.success}, http=${resp.httpStatus}, msg=${resp.message}, data=${resp.data}")

                    if (resp.success) {
                        Toast.makeText(this@PasswordResetConfirmActivity, "비밀번호가 변경되었습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()
                        Log.i(TAG, "password reset success. go login")
                        val goLogin = Intent(this@PasswordResetConfirmActivity, LoginActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        }
                        startActivity(goLogin)
                        finish()
                    } else {
                        Log.e(TAG, "server resp failed: msg=${resp.message}")
                        Toast.makeText(this@PasswordResetConfirmActivity, resp.message ?: "변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: HttpException) {
                    val errBody = e.response()?.errorBody()?.string()
                    Log.e(TAG, "HttpException: code=${e.code()} body=$errBody", e)
                    Toast.makeText(this@PasswordResetConfirmActivity,
                        "서버 오류 ${e.code()}: ${errBody ?: e.message()}",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: IOException) {
                    Log.e(TAG, "Network IOException", e)
                    Toast.makeText(this@PasswordResetConfirmActivity, "네트워크 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Unknown Exception", e)
                    Toast.makeText(this@PasswordResetConfirmActivity, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
