package kr.ac.duksung.dobongzip.ui.password

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.LoginActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.ui.password.PasswordResetConfirmActivity

class PasswordResetConfirmActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset_confirm)

        val back = findViewById<ImageButton>(R.id.backButton)
        val etEmailReadOnly = findViewById<EditText>(R.id.etEmailReadOnly)
        val etPw = findViewById<EditText>(R.id.etNewPassword)
        val etPw2 = findViewById<EditText>(R.id.etNewPasswordConfirm)
        val btn = findViewById<Button>(R.id.btnPwResetConfirm)

        // 앞 화면에서 받은 이메일을 읽기 전용 칸에 표시
        val email = intent.getStringExtra("email").orEmpty()
        etEmailReadOnly.setText(email)

        back.setOnClickListener { finish() }

        btn.setOnClickListener {
            val pw  = etPw.text.toString()
            val pw2 = etPw2.text.toString()

            // ─ 기본 검증 ─
            if (pw.isBlank()) {
                etPw.error = "비밀번호를 입력하세요"
                return@setOnClickListener
            }
            if (pw2.isBlank()) {
                etPw2.error = "비밀번호 확인을 입력하세요"
                return@setOnClickListener
            }
            if (pw != pw2) {
                etPw2.error = "비밀번호가 일치하지 않습니다"
                return@setOnClickListener
            }
            if (pw.length !in 6..20) {
                etPw.error = "6~20자 범위로 입력하세요"
                return@setOnClickListener
            }
            // TODO: 필요하면 영/대/소/특수 2종 이상 조합 검증 로직 추가

            // TODO: 서버에 (email, pw)로 최종 재설정 API 호출

            Toast.makeText(this, "비밀번호가 변경되었습니다. 다시 로그인해 주세요.", Toast.LENGTH_SHORT).show()

            // 로그인 화면으로 돌아가기 (백스택 정리)
            val goLogin = Intent(this, LoginActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(goLogin)
            finish()
        }
    }
}
