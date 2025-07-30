package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 예: 로그인 버튼 클릭 시 MainActivity로 이동
        findViewById<Button>(R.id.login_button).setOnClickListener {
            // 로그인 성공했다고 가정
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 로그인 화면 종료
        }
        findViewById<TextView>(R.id.guest_login).setOnClickListener {
            // 비회원 로그인 로직
        }

        findViewById<TextView>(R.id.find_id_pw).setOnClickListener {
            // 아이디/비밀번호 찾기 이동  ....   ..
        }

    }
}
