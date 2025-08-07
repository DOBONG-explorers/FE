package kr.ac.duksung.dobongzip

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class SignupStep2Activity : AppCompatActivity() {

    private lateinit var nicknameEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var birthdateEditText: EditText
    private lateinit var completeSignupButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_step2) // XML 파일 이름에 맞게 변경

        nicknameEditText = findViewById(R.id.editTextNickname)
        ageEditText = findViewById(R.id.editTextAge)
        genderSpinner = findViewById(R.id.spinnerGender)
        birthdateEditText = findViewById(R.id.editTextBirthdate)
        completeSignupButton = findViewById(R.id.completeSignupButton)

        setupGenderSpinner()
        setupBirthdatePicker()
        setupValidation()
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
    }
}
