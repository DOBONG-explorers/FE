package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import kr.ac.duksung.dobongzip.SignupActivity


class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.login_button).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish() // 뒤로가기 눌러도 로그인 화면으로 안 돌아오게
        }
        findViewById<TextView>(R.id.guest_login).setOnClickListener {
            // 비회원 로그인 로직
        }

        findViewById<TextView>(R.id.find_id_pw).setOnClickListener {
            // 아이디/비밀번호 찾기 이동  ....   ..
        }
        findViewById<TextView>(R.id.signup_text).setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

    }
}
