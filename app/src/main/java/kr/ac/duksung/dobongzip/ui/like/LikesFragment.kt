// ui/like/LikesFragment.kt
package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.network.RetrofitProvider
import kr.ac.duksung.dobongzip.data.repository.LikeRepository
import kr.ac.duksung.dobongzip.databinding.FragmentLikesBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LikesFragment : Fragment(R.layout.fragment_likes) {

    private var _binding: FragmentLikesBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: LikesAdapter
    private var currentOrder: String = "latest"   // latest | oldest | (client: 가나다)

    private val vm: LikesViewModel by viewModels(factoryProducer = {
        val repo = LikeRepository(RetrofitProvider.placeLikeApi)
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return LikesViewModel(repo) as T
            }
        }
    })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentLikesBinding.bind(view)

        // 정렬 드롭다운
        val sortOptions = listOf("최신순", "오래된순", "가나다순")
        binding.actvSort.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, sortOptions)
        )
        binding.actvSort.setText("최신순", false)

        // RecyclerView
        val span = 2
        val h = (12 * resources.displayMetrics.density).toInt()
        val v = (10 * resources.displayMetrics.density).toInt()
        binding.rvLikes.layoutManager = GridLayoutManager(requireContext(), span)
        binding.rvLikes.setPadding(h, v, h, v)
        binding.rvLikes.clipToPadding = false
        binding.rvLikes.addItemDecoration(
            GridSpacingItemDecoration(spanCount = span, horizontalSpacingPx = h, verticalSpacingPx = v, includeEdge = false)
        )

        adapter = LikesAdapter { item ->
            // 낙관적 해제
            vm.unlike(item.placeId) {
                Toast.makeText(requireContext(), "해제 실패: 다시 시도해주세요", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvLikes.adapter = adapter

        // 정렬 선택
        binding.actvSort.setOnItemClickListener { _, _, pos, _ ->
            when (pos) {
                0 -> { // 최신순
                    currentOrder = "latest"
                    vm.load(order = "latest")
                }
                1 -> { // 오래된순
                    currentOrder = "oldest"
                    vm.load(order = "oldest")
                }
                2 -> { // 가나다(클라 정렬)
                    currentOrder = "latest" // 서버 콜은 최신으로 받아오고
                    vm.load(order = "latest")
                }
            }
        }

        // 수집
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.items.collectLatest { list ->
                    val out = if (binding.actvSort.text.toString() == "가나다순") {
                        list.sortedBy { it.placeName }
                    } else list
                    adapter.submitList(out)
                }
            }
        }

        // 최초 로드
        vm.load(order = "latest", size = 30)

        // 예: onViewCreated 끝부분에 임시 실행 (실서비스에선 버튼에 묶어 호출!)
        val defaultDobongLat = 37.668    // 도봉구 중심 근사값 예시
        val defaultDobongLng = 127.031
        vm.likeFirstAndRefresh(defaultDobongLat, defaultDobongLng) {
            Toast.makeText(requireContext(), "첫 장소 좋아요 실패: ${it.message}", Toast.LENGTH_SHORT).show()
        }

    }

    override fun onResume() {
        super.onResume()
        // 맵에서 좋아요 토글 후 복귀 시 최신 동기화
        when (binding.actvSort.text.toString()) {
            "최신순"   -> vm.load(order = "latest")
            "오래된순" -> vm.load(order = "oldest")
            "가나다순" -> vm.load(order = "latest")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
