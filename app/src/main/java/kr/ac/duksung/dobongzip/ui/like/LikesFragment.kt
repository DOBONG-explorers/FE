package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kr.ac.duksung.dobongzip.R

class LikesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_likes, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvLikes)

        // ✅ 2열(Grid) 설정
        val layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.layoutManager = layoutManager

        // ✅ 카드 간격 완전히 밀착 (인스타그램 스타일)
        val horizontalSpacing = 0
        val verticalSpacing = 0
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

        // ✅ 예시 데이터
        val dummyList = listOf(
            LikeItem("서울문화유산", R.drawable.chagdong),
            LikeItem("도봉산 생태공원", R.drawable.chagdong),
            LikeItem("창포원", R.drawable.chagdong),
            LikeItem("둘리뮤지엄", R.drawable.chagdong)
        )

        recyclerView.adapter = LikesAdapter(dummyList)

        return view
    }
}
