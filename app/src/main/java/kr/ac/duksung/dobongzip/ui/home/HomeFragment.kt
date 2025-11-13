package kr.ac.duksung.dobongzip.ui.home

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.repository.NoticeRepository
import kr.ac.duksung.dobongzip.databinding.FragmentHomeBinding
import kr.ac.duksung.dobongzip.databinding.ItemPosterBinding
import kr.ac.duksung.dobongzip.model.Notice
import kr.ac.duksung.dobongzip.model.NoticeCategory
import kr.ac.duksung.dobongzip.ui.notice.NoticeDetailActivity
import kr.ac.duksung.dobongzip.ui.notice.NoticeListActivity
import kr.ac.duksung.dobongzip.ui.top.TopPlacesActivity
import kr.ac.duksung.dobongzip.ui.heritage.CulturalHeritageActivity
import kotlinx.coroutines.launch
import kotlin.math.abs

class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val noticeRepository = NoticeRepository()

    private val fallbackPosterItems = listOf(
        PosterItem(localRes = R.drawable.poster1),
        PosterItem(localRes = R.drawable.poster2),
        PosterItem(localRes = R.drawable.poster3)
    )

    private lateinit var posterAdapter: PosterAdapter

    private var isAuto = true
    private val autoHandler = Handler(Looper.getMainLooper())
    private val autoIntervalMs = 3000L

    private val autoSlide = object : Runnable {
        override fun run() {
            if (!isAuto || posterAdapter.realSize == 0) return
            val next = b.posterPager.currentItem + 1
            b.posterPager.setCurrentItem(next, true)
            autoHandler.postDelayed(this, autoIntervalMs)
        }
    }

    private fun setupListeners() {
        b.btnRandomRecommend.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_recommend)
        }
        b.btnTopPlaces.setOnClickListener {
            startActivity(Intent(requireContext(), TopPlacesActivity::class.java))
        }
    }

    private fun startAuto() {
        autoHandler.removeCallbacks(autoSlide)
        if (isAuto && posterAdapter.realSize > 1) {
            autoHandler.postDelayed(autoSlide, autoIntervalMs)
        }
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
        super.onViewCreated(view, savedInstanceState)
        posterAdapter = PosterAdapter(emptyList(), ::onPosterClicked)
        b.posterPager.layoutDirection = View.LAYOUT_DIRECTION_LTR
        (b.posterPager.getChildAt(0) as? RecyclerView)?.layoutDirection = View.LAYOUT_DIRECTION_LTR
        b.posterPager.adapter = posterAdapter
        b.posterPager.offscreenPageLimit = 1
        b.posterPager.clipToPadding = false
        b.posterPager.clipChildren = false
        val margin = resources.getDimensionPixelSize(R.dimen.poster_page_margin_8dp)
        b.posterPager.setPageTransformer { page, position ->
            MarginPageTransformer(margin).transformPage(page, position)
            val scale = 0.95f + (1 - abs(position)) * 0.05f
            page.scaleY = scale
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
        applyPosterItems(fallbackPosterItems)
        loadPosterImages()
        b.btnNotice.setOnClickListener {
            startActivity(Intent(requireContext(), NoticeListActivity::class.java))
        }
        b.btnNearbyCard.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_mapFragment)
        }
        b.btnHeritage.setOnClickListener {
            startActivity(Intent(requireContext(), CulturalHeritageActivity::class.java))
        }
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

    private fun updateCounterByAdapterPos(pos: Int) {
        val size = posterAdapter.realSize
        if (size == 0) {
            b.tvCounter.text = "0/0"
            return
        }
        val real = posterAdapter.realIndex(pos)
        b.tvCounter.text = "${real + 1}/$size"
    }

    private fun applyPosterItems(items: List<PosterItem>) {
        val data = items.ifEmpty { fallbackPosterItems }
        stopAuto()
        posterAdapter.submitList(data)
        val size = posterAdapter.realSize
        if (size == 0) {
            b.tvCounter.text = "0/0"
            return
        }
        val base = Int.MAX_VALUE / 2
        val start = base - (base % size)
        b.posterPager.setCurrentItem(start, false)
        updateCounterByAdapterPos(start)
        startAuto()
    }

    private fun loadPosterImages() {
        lifecycleScope.launch {
            runCatching {
                noticeRepository.fetchDobongEventImages()
            }.onSuccess { items ->
                val mapped = items.map { PosterItem(id = it.id, imageUrl = it.imageUrl) }
                applyPosterItems(mapped)
            }.onFailure {
                applyPosterItems(fallbackPosterItems)
            }
        }
    }

    private fun onPosterClicked(item: PosterItem) {
        val remoteId = item.id ?: return
        val notice = Notice(
            id = remoteId.toIntOrNull() ?: remoteId.hashCode(),
            title = "",
            date = "",
            category = NoticeCategory.EVENT,
            content = "",
            remoteId = remoteId,
            imageUrl = item.imageUrl
        )
        val intent = Intent(requireContext(), NoticeDetailActivity::class.java)
        intent.putExtra("notice", notice)
        startActivity(intent)
    }

    private data class PosterItem(
        val id: String? = null,
        val imageUrl: String? = null,
        val localRes: Int? = null
    )

    private class PosterAdapter(
        items: List<PosterItem>,
        private val onClick: (PosterItem) -> Unit
    ) : RecyclerView.Adapter<PosterAdapter.VH>() {

        companion object { private const val LOOP_COUNT = Int.MAX_VALUE }

        private var posterItems: List<PosterItem> = items

        val realSize get() = posterItems.size

        override fun getItemCount(): Int = if (posterItems.isEmpty()) 0 else LOOP_COUNT

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemPosterBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            if (posterItems.isEmpty()) return
            val item = posterItems[realPos(position)]
            holder.bind(item, onClick)
        }

        private fun realPos(adapterPos: Int) =
            if (posterItems.isEmpty()) 0 else adapterPos % posterItems.size

        fun realIndex(position: Int) = realPos(position)

        fun submitList(newItems: List<PosterItem>) {
            posterItems = newItems
            notifyDataSetChanged()
        }

        class VH(val b: ItemPosterBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(item: PosterItem, onClick: (PosterItem) -> Unit) {
                val imageView = b.image
                when {
                    !item.imageUrl.isNullOrBlank() -> {
                        Glide.with(imageView.context)
                            .load(item.imageUrl)
                            .placeholder(item.localRes ?: R.drawable.poster1)
                            .error(item.localRes ?: R.drawable.poster1)
                            .into(imageView)
                    }
                    item.localRes != null -> imageView.setImageResource(item.localRes)
                    else -> imageView.setImageResource(R.drawable.poster1)
                }
                if (item.id != null) {
                    b.root.isClickable = true
                    b.root.setOnClickListener { onClick(item) }
                } else {
                    b.root.isClickable = false
                    b.root.setOnClickListener(null)
                }
            }
        }
    }
}
