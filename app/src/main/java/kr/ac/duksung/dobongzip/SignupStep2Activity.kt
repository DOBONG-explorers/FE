// kr/ac/duksung/dobongzip/SignupStep2Activity.kt
package kr.ac.duksung.dobongzip

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import java.util.*
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.data.api.ApiClient
import kr.ac.duksung.dobongzip.data.api.ProfileRequest
import kr.ac.duksung.dobongzip.data.local.TokenStore

class SignupStep2Activity : AppCompatActivity() {

    private lateinit var nicknameEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var birthdateEditText: EditText
    private lateinit var completeSignupButton: Button
    private lateinit var progressImage: ImageView
    private lateinit var progressBar: ProgressBar

    private lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step2)

        tokenStore = TokenStore(this)

        nicknameEditText = findViewById(R.id.editTextNickname)
        ageEditText = findViewById(R.id.editTextAge) // 서버에는 안보내지만 클라이언트 표시용
        genderSpinner = findViewById(R.id.spinnerGender)
        birthdateEditText = findViewById(R.id.editTextBirthdate)
        completeSignupButton = findViewById(R.id.completeSignupButton)
        progressImage = findViewById(R.id.progressImage)
        progressBar = findViewById(R.id.progressBar)

        setupGenderSpinner()
        setupBirthdatePicker()
        setupValidation()

        completeSignupButton.setOnClickListener {
            // 서버로 프로필 제출
            submitProfile()
        }
    }

    private fun setupGenderSpinner() {
        val genderOptions = listOf("성별 선택", "여성", "남성")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, genderOptions)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        genderSpinner.adapter = adapter
    }

    private fun setupBirthdatePicker() {
        birthdateEditText.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val birthdate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                birthdateEditText.setText(birthdate)
                validateInputs()
            }, year, month, day)

            datePickerDialog.show()
        }
    }

    private fun setupValidation() {
        val watcher = object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) = validateInputs()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        nicknameEditText.addTextChangedListener(watcher)
        ageEditText.addTextChangedListener(watcher)
        birthdateEditText.addTextChangedListener(watcher)
        genderSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View, position: Int, id: Long) {
                validateInputs()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun validateInputs() {
        val nickname = nicknameEditText.text.toString().trim()
        val age = ageEditText.text.toString().trim()
        val birthdate = birthdateEditText.text.toString().trim()
        val genderSelected = genderSpinner.selectedItemPosition != 0

        val isAllValid = nickname.isNotEmpty() && age.isNotEmpty() && birthdate.isNotEmpty() && genderSelected

        completeSignupButton.isEnabled = isAllValid
        completeSignupButton.setBackgroundResource(
            if (isAllValid) R.drawable.rounded_button_blue else R.drawable.rounded_button_gray
        )

        if (isAllValid) {
            progressBar.progress = 100
            progressImage.setImageResource(R.drawable.re_3)
        }
    }

    private fun submitProfile() {
        val nickname = nicknameEditText.text.toString().trim()
        val birth = birthdateEditText.text.toString().trim()
        val genderSpinnerText = genderSpinner.selectedItem.toString()
        val gender = when (genderSpinnerText) {
            "여성" -> "FEMALE"
            "남성" -> "MALE"
            else -> "UNKNOWN"
        }

        lifecycleScope.launch {
            try {
                val email = tokenStore.getSignupEmail() ?: run {
                    Toast.makeText(this@SignupStep2Activity, "이메일 정보를 찾을 수 없습니다. 처음부터 다시 진행해주세요.", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // 서버 스펙: name, nickname, gender, birth 필요
                // name은 필수이므로 일단 nickname으로 채워 보낸다 (필요 시 별도 입력 필드 추가)
                val req = ProfileRequest(
                    name = nickname,
                    nickname = nickname,
                    gender = gender,
                    birth = birth
                )

                val res = ApiClient.authService.submitProfile(
                    email = email,
                    loginType = "APP",
                    body = req
                )

                if (res.success) {
                    Toast.makeText(this@SignupStep2Activity, "프로필이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupStep2Activity, LoginActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this@SignupStep2Activity, res.message.ifBlank { "프로필 저장 실패" }, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupStep2Activity, "오류: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
