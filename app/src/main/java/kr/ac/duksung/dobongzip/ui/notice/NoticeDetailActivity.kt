package kr.ac.duksung.dobongzip.ui.notice

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.model.Notice

class NoticeDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_detail)

        val notice = intent.getSerializableExtra("notice") as? Notice

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvContent = findViewById<TextView>(R.id.tvContent)

        // 뒤로가기 버튼
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        notice?.let {
            tvTitle.text = it.title
            tvDate.text = it.date
            tvContent.text = it.content
        }
    }
}
