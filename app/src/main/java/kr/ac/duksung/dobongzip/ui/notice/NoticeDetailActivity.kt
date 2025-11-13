package kr.ac.duksung.dobongzip.ui.notice

import android.os.Build
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Patterns
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.data.repository.NoticeRepository
import kr.ac.duksung.dobongzip.model.Notice
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

class NoticeDetailActivity : AppCompatActivity() {

    private val noticeRepository = NoticeRepository()

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notice_detail)

        val notice = intent.getSerializableExtra("notice") as? Notice

        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvDate = findViewById<TextView>(R.id.tvDate)
        val tvContent = findViewById<TextView>(R.id.tvContent)
        val tvContact = findViewById<TextView>(R.id.tvContact)
        val imageView = findViewById<ImageView>(R.id.noticeImageView)

        // 뒤로가기 버튼
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }

        notice?.let { item ->
            tvTitle.text = item.title.takeIf { it.isNotBlank() } ?: "행사 정보를 불러오는 중..."
            tvDate.text = item.date
            tvContent.text = item.content.takeIf { it.isNotBlank() } ?: "행사 상세 내용을 불러오는 중..."
            tvContact.visibility = View.GONE

            val cleanedImageUrl = sanitizeUrl(item.imageUrl)
            if (cleanedImageUrl.isNullOrBlank()) {
                imageView.visibility = View.GONE
            } else {
                imageView.visibility = View.VISIBLE
                loadImage(cleanedImageUrl, imageView)
            }

            if (!item.remoteId.isNullOrBlank()) {
                val detailDateParam = extractDateParam(item.date)
                lifecycleScope.launch {
                    runCatching {
                        noticeRepository.fetchDobongEventDetail(
                            id = item.remoteId,
                            date = detailDateParam
                        )
                    }.onSuccess { detail ->
                        val finalTitle = detail.TITLE?.takeIf { it.isNotBlank() } ?: item.title
                        if (finalTitle.isNotBlank()) {
                            tvTitle.text = finalTitle
                        }

                        val start = formatDateTime(detail.STRTDATE)
                        val end = formatDateTime(detail.END_DATE)
                        val scheduleText = when {
                            start != null && end != null -> "$start ~ $end"
                            start != null -> start
                            end != null -> end
                            else -> null
                        }
                        scheduleText?.let { tvDate.text = it }

                        val contentLines = mutableListOf<String>()
                        detail.PROGRAM?.takeIf { it.isNotBlank() }?.let(contentLines::add)
                        detail.PLACE?.takeIf { it.isNotBlank() }?.let { place ->
                            contentLines.add("장소: $place")
                        }
                        detail.USE_TRGT?.takeIf { it.isNotBlank() }?.let { target ->
                            contentLines.add("대상: $target")
                        }
                        detail.USE_FEE?.takeIf { it.isNotBlank() }?.let { fee ->
                            contentLines.add("이용요금: $fee")
                        }
                        detail.ETC_DESC?.takeIf { it.isNotBlank() }?.let { etc ->
                            contentLines.add(etc)
                        }

                        val contentText = contentLines.joinToString("\n").trim()
                        if (contentText.isNotBlank()) {
                            tvContent.text = contentText
                            tvContent.textAlignment = View.TEXT_ALIGNMENT_VIEW_START
                        }

                        val contactText = sanitizeUrl(detail.ORG_LINK)
                            ?: detail.CODE_NAME?.takeIf { it.isNotBlank() }?.trim()
                        setContactText(tvContact, contactText)

                        val detailImage = sanitizeUrl(detail.MAIN_IMG)
                        if (!detailImage.isNullOrBlank()) {
                            imageView.visibility = View.VISIBLE
                            loadImage(detailImage, imageView)
                        }
                    }
                }
            }
        }
    }

    private fun extractDateParam(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val firstPart = raw.split("~").firstOrNull()?.trim().orEmpty()
        val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
        return firstPart.takeIf { dateRegex.matches(it) }
    }

    private fun sanitizeUrl(raw: String?): String? {
        if (raw.isNullOrBlank() || raw == "null") return null
        return raw.removePrefix("[").removeSuffix("]").trim().takeIf { it.isNotBlank() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun formatDateTime(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val normalized = raw.trim().replace('T', ' ')
        val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val patterns = listOf(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        )

        for (formatter in patterns) {
            try {
                val dateTime = LocalDateTime.parse(normalized, formatter)
                return dateTime.format(outputFormatter)
            } catch (_: DateTimeParseException) { }
        }

        val regex = Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}""")
        return regex.find(normalized)?.value
    }

    private fun setContactText(textView: TextView, contact: String?) {
        val value = contact?.trim()
        if (value.isNullOrEmpty()) {
            textView.visibility = View.GONE
            textView.movementMethod = null
            return
        }

        textView.visibility = View.VISIBLE
        if (Patterns.WEB_URL.matcher(value).matches()) {
            val label = "문의: $value"
            val spannable = SpannableString(label).apply {
                setSpan(
                    URLSpan(value),
                    4,
                    label.length,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            textView.text = spannable
            textView.movementMethod = LinkMovementMethod.getInstance()
        } else {
            textView.text = "문의: $value"
            textView.movementMethod = null
        }
    }

    private fun loadImage(url: String, imageView: ImageView) {
        runCatching {
            val glideClass = Class.forName("com.bumptech.glide.Glide")
            val withMethod = glideClass.getMethod("with", android.content.Context::class.java)
            val requestManager = withMethod.invoke(null, this)
            val loadMethod = requestManager.javaClass.getMethod("load", Any::class.java)
            val requestBuilder = loadMethod.invoke(requestManager, url)
            val intoMethod = requestBuilder.javaClass.getMethod("into", ImageView::class.java)
            intoMethod.invoke(requestBuilder, imageView)
        }.onFailure {
            imageView.visibility = View.GONE
        }
    }
}
