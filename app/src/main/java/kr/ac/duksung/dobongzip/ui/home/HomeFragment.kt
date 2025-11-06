package kr.ac.duksung.dobongzip.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentHomeBinding
import kr.ac.duksung.dobongzip.databinding.ItemPosterBinding
import kr.ac.duksung.dobongzip.ui.map.MapActivity
import kr.ac.duksung.dobongzip.ui.notice.NoticeListActivity
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private var isAuto = true
    private val autoHandler = Handler(Looper.getMainLooper())
    private val autoIntervalMs = 3000L

    private val autoSlide = object : Runnable {
        override fun run() {
            if (!isAuto) return
            val next = b.posterPager.currentItem + 1
            b.posterPager.setCurrentItem(next, true)
            autoHandler.postDelayed(this, autoIntervalMs)
        }
    }

    private fun setupListeners() {
        _b?.btnRandomRecommend?.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_recommend)
        }
    }

    private fun startAuto() {
        autoHandler.removeCallbacks(autoSlide)
        if (isAuto) autoHandler.postDelayed(autoSlide, autoIntervalMs)
    }

    private fun stopAuto() {
        autoHandler.removeCallbacks(autoSlide)
    }

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

        val start = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % posters.size)
        b.posterPager.setCurrentItem(start, false)

        val margin = resources.getDimensionPixelSize(R.dimen.poster_page_margin_8dp)
        b.posterPager.setPageTransformer { page, position ->
            MarginPageTransformer(margin).transformPage(page, position)
            val scale = 0.95f + (1 - abs(position)) * 0.05f
            page.scaleY = scale
        }

        fun updateCounterByAdapterPos(pos: Int) {
            val real = adapter.realIndex(pos)
            b.tvCounter.text = "${real + 1}/${posters.size}"
        }

        b.posterPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateCounterByAdapterPos(position)
            override fun onPageScrollStateChanged(state: Int) {
                when (state) {
                    ViewPager2.SCROLL_STATE_DRAGGING -> stopAuto()
                    ViewPager2.SCROLL_STATE_IDLE -> startAuto()
                }
            }
        })
        updateCounterByAdapterPos(start)

        b.btnNotice.setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }

        b.btnNearbyCard.setOnClickListener {
            startActivity(Intent(requireContext(), MapActivity::class.java))
        }

        // ✅ [수정됨] setupListeners() 함수를 여기서 호출해야 합니다.
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        startAuto()
    }

    override fun onPause() {
        super.onPause()
        stopAuto()
    }

    override fun onDestroyView() {
        stopAuto()
        _b = null
        super.onDestroyView()
    }

    private class PosterAdapter(private val items: List<Int>) :
        RecyclerView.Adapter<PosterAdapter.VH>() {

        companion object { private const val LOOP_COUNT = Int.MAX_VALUE }

        class VH(val b: ItemPosterBinding) : RecyclerView.ViewHolder(b.root)

        override fun getItemCount(): Int = if (items.isEmpty()) 0 else LOOP_COUNT
        private fun realPos(adapterPos: Int) = adapterPos % items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemPosterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.b.image.setImageResource(realPos(position).let(items::get))
        }

        fun realIndex(position: Int) = realPos(position)
    }
}