package kr.ac.duksung.dobongzip.ui.notice

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.repository.NoticeRepository
import kr.ac.duksung.dobongzip.model.Notice
import kr.ac.duksung.dobongzip.model.NoticeCategory
import kotlinx.coroutines.launch

class NoticeListActivity : AppCompatActivity() {

    private lateinit var adapter: NoticeAdapter
    private lateinit var btnAll: MaterialButton
    private lateinit var btnEvent: MaterialButton
    private val noticeRepository = NoticeRepository()

    // PDF 설명용 공지 1개
    private val pdfNotice = Notice(
        id = -1, // 서버와 안 겹치게 음수 같은 값
        title = "도봉마을탐험대 이용 안내(PDF)",
        date = "2025-11-18",
        category = NoticeCategory.NOTICE,
        content = "도봉마을탐험대 서비스 이용 방법을 PDF로 확인하세요.",
        pdfUrl = "https://drive.google.com/file/d/1nlInHddKBgBmPH7WsobMzNOyAspN0ol_/preview"
    )


    // 더미 데이터
    private val staticNotices = listOf(
        Notice(
            id = -1,   // 서버와 안 겹치게 음수
            title = "도봉마을탐험대 사례집 안내(PDF)",
            date = "2025-11-18",
            category = NoticeCategory.NOTICE,
            content = "도봉마을탐험대가 제작한 사례집을 PDF로 확인하세요.",
            pdfUrl = "https://drive.google.com/file/d/1nlInHddKBgBmPH7WsobMzNOyAspN0ol_/view?usp=drive_link"
        ),
        Notice(1, "서비스 개편 안내", "2025-09-01", NoticeCategory.NOTICE, "서비스가 개편됩니다."),
        Notice(2, "가을 축제 일정", "2025-09-02", NoticeCategory.EVENT, "가을 축제가 열립니다."),
        Notice(3, "추석 연휴 공지 안내", "2025-10-03", NoticeCategory.NOTICE, "즐거운 추석되세요.")
    )
    private var remoteEventNotices: List<Notice> = emptyList()
    private var currentCategory: NoticeCategory = NoticeCategory.ALL

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_list)

        // 뒤로가기
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // RecyclerView
        val rvNotice = findViewById<RecyclerView>(R.id.rvNotice)

        adapter = NoticeAdapter(emptyList()) { notice ->
            val intent = Intent(this, NoticeDetailActivity::class.java)
            intent.putExtra("notice", notice) // Notice는 Serializable/Parcelable 구현 필요
            startActivity(intent)
        }
        rvNotice.layoutManager = LinearLayoutManager(this)
        rvNotice.adapter = adapter

        // 카테고리 버튼
        btnAll = findViewById(R.id.btnAll)
        btnEvent = findViewById(R.id.btnEvent)

        // 디폴트 선택: 전체
        selectAll(btnAll, btnEvent)
        submitCurrentList()

        // 클릭 리스너
        btnAll.setOnClickListener {
            selectAll(btnAll, btnEvent)
            currentCategory = NoticeCategory.ALL
            submitCurrentList()
        }
        btnEvent.setOnClickListener {
            selectEvent(btnAll, btnEvent)
            currentCategory = NoticeCategory.EVENT
            submitCurrentList()
        }

        loadDobongEvents()
    }

    // -------------------------------
    // 확장 함수 / 색상 토글 함수
    // -------------------------------
    private fun MaterialButton.setBg(color: Int) {
        backgroundTintList = ColorStateList.valueOf(color)
    }
    private fun MaterialButton.setTxt(color: Int) {
        setTextColor(color)
    }

    private fun selectAll(btnAll: MaterialButton, btnEvent: MaterialButton) {
        val selectedBg   = Color.parseColor("#0B79D0")
        val unselectedBg = Color.parseColor("#E8F4FF")
        val selectedTxt  = Color.WHITE
        val unselectedTxt= Color.BLACK

        btnAll.setBg(selectedBg);   btnAll.setTxt(selectedTxt)
        btnEvent.setBg(unselectedBg); btnEvent.setTxt(unselectedTxt)
    }

    private fun selectEvent(btnAll: MaterialButton, btnEvent: MaterialButton) {
        val selectedBg   = Color.parseColor("#0B79D0")
        val unselectedBg = Color.parseColor("#E8F4FF")
        val selectedTxt  = Color.WHITE
        val unselectedTxt= Color.BLACK

        btnEvent.setBg(selectedBg); btnEvent.setTxt(selectedTxt)
        btnAll.setBg(unselectedBg); btnAll.setTxt(unselectedTxt)
    }

    private fun submitCurrentList() {
        val combined = staticNotices + remoteEventNotices
        val list = when (currentCategory) {
            NoticeCategory.EVENT -> combined.filter { it.category == NoticeCategory.EVENT }
            else -> combined
        }
        adapter.submitList(list.sortedByDescending { it.date })
    }

    private fun loadDobongEvents() {
        lifecycleScope.launch {
            runCatching {
                noticeRepository.fetchDobongEvents()
            }.onSuccess { events ->
                remoteEventNotices = events
                submitCurrentList()
            }
        }
    }
}
