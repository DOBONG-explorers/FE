package kr.ac.duksung.dobongzip.ui.like

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kr.ac.duksung.dobongzip.R

class LikesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // ✅ fragment_likes.xml 레이아웃을 연결
        return inflater.inflate(R.layout.fragment_likes, container, false)
    }
}
