package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.data.repository.LikeRepository

class LikesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var actvSort: MaterialAutoCompleteTextView
    private lateinit var adapter: LikesAdapter

    // ✅ ViewModel 생성 (Hilt 없이 팩토리 직접 생성)
    private val vm: LikesViewModel by viewModels(factoryProducer = {
        val api = RetrofitProvider.placeLikeApi
        val repo = LikeRepository(api)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LikesViewModel(repo) as T
            }
        }
    })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_likes, container, false)
        recyclerView = view.findViewById(R.id.rvLikes)
        actvSort = view.findViewById(R.id.actvSort)

        // ✅ 드롭다운 메뉴
        val sortOptions = listOf("최신순", "오래된순", "가나다순")
        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
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

        // ✅ Adapter (URL 기반 이미지 + 하트 해제 기능)
        adapter = LikesAdapter { item ->
            vm.unlike(item.placeId) {
                Toast.makeText(requireContext(), "해제 실패: 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        recyclerView.adapter = adapter

        // ✅ 정렬 이벤트 → API 호출 or 클라이언트 정렬
        actvSort.setOnItemClickListener { _, _, position, _ ->
            when (position) {
                0 -> vm.load(order = "latest") // 최신순 (기본)
                1 -> vm.load(order = "oldest") // 오래된순
                2 -> { // 가나다순은 서버 지원 없음 → 클라 정렬
                    vm.load(order = "latest")
                    viewLifecycleOwner.lifecycleScope.launch {
                        vm.items.collectLatest { list ->
                            adapter.submitList(list.sortedBy { it.placeName })
                        }
                    }
                    return@setOnItemClickListener
                }
            }
        }

        // ✅ 데이터 옵저빙
        viewLifecycleOwner.lifecycleScope.launch {
            vm.items.collectLatest { list ->
                if (list.isEmpty()) {
                    adapter.submitList(
                        listOf(
                            LikeItemUi("1", "북서울미술관", "https://picsum.photos/300?random=3"),
                            LikeItemUi("2", "둘리뮤지엄", "https://picsum.photos/300?random=4")
                        )
                    )
                } else {
                    adapter.submitList(list)
                }
            }
        }


        // ✅ 처음 진입 시 데이터 로드
        vm.load(order = "latest", size = 30)

        return view
    }
}
