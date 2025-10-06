package kr.ac.duksung.dobongzip.ui.notice

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.model.Notice

class NoticeAdapter(
    private var items: List<Notice>,                 // 공지사항 리스트
    private val onItemClick: (Notice) -> Unit        // 아이템 클릭 시 실행할 함수
) : RecyclerView.Adapter<NoticeAdapter.NoticeViewHolder>() {

    // ViewHolder : item_notice.xml 의 뷰들을 담는 역할
    class NoticeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivArrow: ImageView = itemView.findViewById(R.id.ivArrow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoticeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notice, parent, false)   // item_notice.xml 연결
        return NoticeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoticeViewHolder, position: Int) {
        val notice = items[position]

        holder.tvTitle.text = notice.title
        holder.tvDate.text = notice.date

        // 클릭 이벤트
        holder.itemView.setOnClickListener {
            onItemClick(notice)
        }
    }

    override fun getItemCount(): Int = items.size

    // 리스트 갱신 (필터 적용 시 호출)
    fun submitList(newList: List<Notice>) {
        items = newList
        notifyDataSetChanged()
    }
}
