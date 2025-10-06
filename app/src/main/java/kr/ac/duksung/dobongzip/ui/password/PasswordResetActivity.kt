package kr.ac.duksung.dobongzip.ui.password

import android.os.Bundle
import android.content.Intent
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.R


class PasswordResetActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_reset2)

        val backButton = findViewById<ImageButton>(R.id.backButton)
        val etEmail    = findViewById<EditText>(R.id.etEmail)
        val btnConfirm = findViewById<Button>(R.id.btnConfirm)

        backButton.setOnClickListener { finish() }

        btnConfirm.setOnClickListener {
            val email = etEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "이메일을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // TODO: 서버에 email 전달하여 비밀번호 재설정 요청 API 연동
            Toast.makeText(this, "입력된 이메일: $email", Toast.LENGTH_SHORT).show()

            //
            val intent = Intent(this, PasswordResetConfirmActivity::class.java)
                .putExtra("email", email)
            startActivity(intent)
        }

    }
}