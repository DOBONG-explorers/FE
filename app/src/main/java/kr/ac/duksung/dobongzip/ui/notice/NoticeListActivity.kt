package kr.ac.duksung.dobongzip.ui.notice

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.model.Notice
import kr.ac.duksung.dobongzip.model.NoticeCategory

class NoticeListActivity : AppCompatActivity() {

    private lateinit var adapter: NoticeAdapter
    private lateinit var btnAll: MaterialButton
    private lateinit var btnEvent: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_list)

        // 뒤로가기
        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        // RecyclerView
        val rvNotice = findViewById<RecyclerView>(R.id.rvNotice)

        // 더미 데이터 (전체 목록)
        val allItems = listOf(
            Notice(1, "서비스 개편 안내", "2025-09-01", NoticeCategory.NOTICE, "서비스가 개편됩니다."),
            Notice(2, "가을 축제 일정", "2025-09-02", NoticeCategory.EVENT, "가을 축제가 열립니다."),
            Notice(3, "추석 연휴 공지 안내", "2025-10-03", NoticeCategory.NOTICE, "즐거운 추석되세요.")
        )
        // 행사만 필터된 목록
        val eventItems = allItems.filter { it.category == NoticeCategory.EVENT }

        // 어댑터 연결 (초기: 전체)
        adapter = NoticeAdapter(allItems) { notice ->
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
        adapter.submitList(allItems)

        // 클릭 리스너
        btnAll.setOnClickListener {
            selectAll(btnAll, btnEvent)
            adapter.submitList(allItems)
        }
        btnEvent.setOnClickListener {
            selectEvent(btnAll, btnEvent)
            adapter.submitList(eventItems)
        }
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
}
