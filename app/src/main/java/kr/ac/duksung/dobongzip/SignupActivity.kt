package kr.ac.duksung.dobongzip

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener

class SignupActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var nextButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step1)

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

        // 다음 스텝으로 이동
        nextButton.setOnClickListener {
            val intent = Intent(this, SignupStep2Activity::class.java)
            startActivity(intent)
        }
    }

    /**
     * 텍스트 변경 리스너를 EditText에 적용
     */
    private fun applyTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                updateButtonState()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    /**
     * 유효성 검사 후 버튼 상태 업데이트
     */
    private fun updateButtonState() {
        val isValid = isValidInput()

        // 로그 찍기
        android.util.Log.d("SignupDebug", "email=${emailEditText.text}, pw=${passwordEditText.text}, confirm=${confirmPasswordEditText.text}, phone=${phoneEditText.text}, isValid=$isValid")

        nextButton.isEnabled = isValid
        nextButton.setBackgroundResource(
            if (isValid) R.drawable.rounded_button_blue
            else R.drawable.rounded_button_gray
        )
    }

    /**
     * 모든 입력값 유효성 검사
     */
    private fun isValidInput(): Boolean {
        val email = emailEditText.text.toString().trim()
        val pw = passwordEditText.text.toString()
        val confirm = confirmPasswordEditText.text.toString()
        val phone = phoneEditText.text.toString().trim()

        return email.isNotEmpty() &&
                pw.length >= 6 &&
                pw == confirm &&
                phone.isNotEmpty()
    }
}
