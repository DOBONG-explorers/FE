package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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

        val horizontalSpacing = ( 12* resources.displayMetrics.density).toInt() // 좌우 여유
        val verticalSpacing = (10 * resources.displayMetrics.density).toInt()   // 위아래 여유
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

        // ✅ 예시 데이터 (Mutable)
        val dummyList = mutableListOf(
            LikeItem(placeName = "서울문화유산", imageResId = R.drawable.chagdong),
            LikeItem(placeName = "도봉산 생태공원", imageResId = R.drawable.chagdong),
            LikeItem(placeName = "창포원", imageResId = R.drawable.chagdong),
            LikeItem(placeName = "창포원", imageResId = R.drawable.chagdong),
            LikeItem(placeName = "창포원", imageResId = R.drawable.chagdong),
            LikeItem(placeName = "창포원", imageResId = R.drawable.chagdong),

            LikeItem(placeName = "둘리뮤지엄", imageResId = R.drawable.chagdong) ,
            LikeItem(placeName = "둘리뮤지엄", imageResId = R.drawable.chagdong)

        )

        recyclerView.adapter = LikesAdapter(dummyList) { removed ->
            // 필요 시 서버에 좋아요 취소 API 호출 자리
            // viewLifecycleOwner.lifecycleScope.launch { repository.unlike(removed.id) ... }

            Toast.makeText(requireContext(), " ${removed.placeName}이(가) 목록에서 삭제되었어요", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}