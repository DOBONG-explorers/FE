// kr/ac/duksung/dobongzip/ui/mypage/MyPageSetFragment.kt
package kr.ac.duksung.dobongzip.ui.mypage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import kotlinx.coroutines.launch
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.ui.common.ProfileViewModel

class MyPageSetFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvBirth: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var btnToEdit: Button

    private val profileViewModel: ProfileViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val v = inflater.inflate(R.layout.fragment_my_page, container, false)

        // XML id 연결
        profileImage = v.findViewById(R.id.profileImage)
        tvNickname   = v.findViewById(R.id.editNickname)
        tvBirth      = v.findViewById(R.id.editBirthday)
        tvEmail      = v.findViewById(R.id.editEmail)
        btnBack      = v.findViewById(R.id.backButton)
        btnToEdit    = v.findViewById(R.id.myPageButton)

        // 뒤로가기
        btnBack.setOnClickListener { findNavController().popBackStack() }

        // "개인정보 수정" → 편집 화면 이동 (fragment_mypage2.xml 사용하는 프래그먼트)
        btnToEdit.setOnClickListener {
            findNavController().navigate(R.id.myPageEditFragment)
        }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 진입 시 서버 최신값 로드 (Authorization 헤더가 붙도록 토큰 세팅 필요)
        profileViewModel.loadProfileAll()

        // 상태 구독 → UI 반영
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                profileViewModel.profileState.collect { state ->
                    // 1) 프로필 이미지: 서버 URL 우선 → 로컬 uri → 기본 이미지
                    when {
                        !state.imageUrl.isNullOrBlank() -> {
                            Glide.with(this@MyPageSetFragment)
                                .load(state.imageUrl)
                                .centerCrop()
                                .into(profileImage)
                        }
                        state.uri != null -> {
                            Glide.with(this@MyPageSetFragment)
                                .load(state.uri)
                                .centerCrop()
                                .into(profileImage)
                        }
                        else -> profileImage.setImageResource(R.drawable.prf3)
                    }

                    // 2) 텍스트 정보
                    tvNickname.text = state.nickname ?: "-"
                    tvBirth.text    = state.birthday ?: "-"
                    tvEmail.text    = state.email ?: "-"
                }
            }
        }
    }
}
