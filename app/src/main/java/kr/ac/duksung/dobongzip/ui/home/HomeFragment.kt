package kr.ac.duksung.dobongzip.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentHomeBinding
import kr.ac.duksung.dobongzip.databinding.ItemPosterBinding
import kr.ac.duksung.dobongzip.ui.notice.NoticeListActivity
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private var isAuto = true
    private val autoHandler = Handler(Looper.getMainLooper())
    private val autoIntervalMs = 3000L



    private fun startAuto() {
        autoHandler.removeCallbacks(autoSlide)
        if (isAuto) autoHandler.postDelayed(autoSlide, autoIntervalMs)
    }

    private fun stopAuto() {
        autoHandler.removeCallbacks(autoSlide)
    }

    // ✅ 바인딩 inflate (필수)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val posters = listOf(R.drawable.poster1, R.drawable.poster2, R.drawable.poster3)

        b.posterPager.layoutDirection = View.LAYOUT_DIRECTION_LTR
        (b.posterPager.getChildAt(0) as? RecyclerView)?.layoutDirection = View.LAYOUT_DIRECTION_LTR

        val adapter = PosterAdapter(posters)
        b.posterPager.adapter = adapter
        b.posterPager.offscreenPageLimit = 1
        b.posterPager.clipToPadding = false
        b.posterPager.clipChildren = false

        // ✅ 중앙 근처에서 시작 (애니메이션 항상 한 방향 유지)
        val start = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % posters.size)
        b.posterPager.setCurrentItem(start, false)

        val margin = resources.getDimensionPixelSize(R.dimen.poster_page_margin_8dp)
        b.posterPager.setPageTransformer { page, position ->
            MarginPageTransformer(margin).transformPage(page, position)
            val scale = 0.95f + (1 - kotlin.math.abs(position)) * 0.05f
            page.scaleY = scale
        }

        // 카운터는 실제 인덱스로 표시
        fun updateCounterByAdapterPos(pos: Int) {
            val real = adapter.realIndex(pos)
            b.tvCounter.text = "${real + 1}/${posters.size}"
        }

        b.posterPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateCounterByAdapterPos(position)
            }
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAuto()
                    ViewPager2.SCROLL_STATE_IDLE -> startAuto()
                }
            }
        })
        updateCounterByAdapterPos(start)

        // 공지사항 버튼 클릭 -> 공지사항으로 이동
        b.btnNotice.setOnClickListener {
            val intent = Intent(requireContext(), NoticeListActivity::class.java)
            startActivity(intent)
        }
    }

    private val autoSlide = object : Runnable {
        override fun run() {
            if (!isAuto) return
            val next = b.posterPager.currentItem + 1
            b.posterPager.setCurrentItem(next, true) // ✅ 항상 +1로 같은 방향
            autoHandler.postDelayed(this, autoIntervalMs)
        }
    }


    /*
            // 3) TabLayoutMediator + 커스텀 점 뷰 적용
            mediator = TabLayoutMediator(b.pagerIndicator, b.posterPager) { tab, _ ->
                tab.setCustomView(R.layout.item_indicator_dot) // dot 뷰(3dp + 마진)
            }.also { it.attach() }

            // 초기 선택 상태 반영
            for (i in 0 until b.pagerIndicator.tabCount) {
                b.pagerIndicator.getTabAt(i)?.customView?.isSelected =
                    (i == b.posterPager.currentItem)
            }

            tabListener = object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) { tab.customView?.isSelected = true }
                override fun onTabUnselected(tab: TabLayout.Tab) { tab.customView?.isSelected = false }
                override fun onTabReselected(tab: TabLayout.Tab) {}
            }
            b.pagerIndicator.addOnTabSelectedListener(tabListener)
    */


    override fun onResume() {
        super.onResume()
        startAuto()
    }

    override fun onPause() {
        super.onPause()
        stopAuto()
    }

    // ✅ 리소스/콜백 정리 (메모리 누수 및 크래시 방지)
    override fun onDestroyView() {
        stopAuto()
        _b = null
        super.onDestroyView()
    }

    /*
        override fun onDestroyView() {
            autoHandler.removeCallbacks(autoSlide)
            if (::tabListener.isInitialized) {
                b.pagerIndicator.removeOnTabSelectedListener(tabListener)
            }
            mediator?.detach()
            _b = null
            super.onDestroyView()
        }
    */
    /** ViewPager2 어댑터 (둥근 모서리는 레이아웃/카드에서 처리) */
    private class PosterAdapter(private val items: List<Int>) :
        RecyclerView.Adapter<PosterAdapter.VH>() {

        companion object {
            private const val LOOP_COUNT = Int.MAX_VALUE
        }

        class VH(val b: ItemPosterBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else LOOP_COUNT

        private fun realPos(adapterPos: Int): Int = adapterPos % items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemPosterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val rp = realPos(position)
            holder.b.image.setImageResource(items[rp])
        }

        fun realIndex(position: Int): Int = realPos(position)
    }

}
