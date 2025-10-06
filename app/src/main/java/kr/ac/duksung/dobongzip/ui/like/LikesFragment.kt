package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kr.ac.duksung.dobongzip.R

class LikesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: LikesAdapter
    private lateinit var actvSort: MaterialAutoCompleteTextView

    private val dummyList = mutableListOf(
        LikeItem(placeName = "서울문화유산", imageResId = R.drawable.chagdong),
        LikeItem(placeName = "도봉산 생태공원", imageResId = R.drawable.chagdong),
        LikeItem(placeName = "창포원", imageResId = R.drawable.chagdong),
        LikeItem(placeName = "둘리뮤지엄", imageResId = R.drawable.chagdong),
        LikeItem(placeName = "광장시장", imageResId = R.drawable.chagdong),
        LikeItem(placeName = "북서울미술관", imageResId = R.drawable.chagdong)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_likes, container, false)
        recyclerView = view.findViewById(R.id.rvLikes)
        actvSort = view.findViewById(R.id.actvSort)

        // ✅ 드롭다운 메뉴 설정
        val sortOptions = listOf("최신순", "오래된순", "가나다순")
        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,   // ✅ 기본 item 레이아웃 사용
            sortOptions
        )
        actvSort.setAdapter(sortAdapter)

        // ✅ RecyclerView 설정
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        val horizontalSpacing = (12 * resources.displayMetrics.density).toInt()
        val verticalSpacing = (10 * resources.displayMetrics.density).toInt()
        recyclerView.setPadding(horizontalSpacing, verticalSpacing, horizontalSpacing, verticalSpacing)
        recyclerView.clipToPadding = false
        recyclerView.addItemDecoration(
            GridSpacingItemDecoration(
                spanCount = 2,
                horizontalSpacingPx = horizontalSpacing,
                verticalSpacingPx = verticalSpacing,
                includeEdge = false
            )
        )

        adapter = LikesAdapter(dummyList) { removed ->
            Toast.makeText(requireContext(), "‘${removed.placeName}’이(가) 삭제되었어요", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter

        // ✅ 정렬 선택 시 이벤트
        actvSort.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> sortByRecent()
                1 -> sortByOldest()
                2 -> sortByName()
            }
        }

        return view
    }

    // ✅ 최신순 (여기서는 단순히 리스트 역순)
    private fun sortByRecent() {
        dummyList.reverse()
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "최신순으로 정렬했어요", Toast.LENGTH_SHORT).show()
    }

    // ✅ 오래된순 (그냥 원래 순서)
    private fun sortByOldest() {
        dummyList.sortBy { it.id }
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "오래된순으로 정렬했어요", Toast.LENGTH_SHORT).show()
    }

    // ✅ 가나다순 (한글 이름 오름차순)
    private fun sortByName() {
        dummyList.sortBy { it.placeName }
        adapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "가나다순으로 정렬했어요", Toast.LENGTH_SHORT).show()
    }
}
