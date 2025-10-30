package kr.ac.duksung.dobongzip.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.recyclerview.widget.LinearLayoutManager
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentChatBinding
import kotlin.math.max


class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ✅ RecyclerView & Adapter 세팅
        adapter = ChatAdapter(messages)
        binding.chatRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatRecyclerView.adapter = adapter

        // ✅ 첫 진입 시 챗봇 환영 메시지
        val welcomeMessage = """
        안녕하세요! 
        저는 당신의 장소 탐험을 도와줄 
        AI 챗봇 ‘AI도봉’입니다 😊  
        어떤 분위기나 장소를 찾고 계신가요?

        궁금한 걸 자유롭게 물어보세요.  
        리뷰와 3D 화면까지 함께 보여드릴게요!
    """.trimIndent()

        adapter.addMessage(ChatMessage(welcomeMessage, false))
        binding.chatRecyclerView.scrollToPosition(messages.size - 1)

        // ✅ 전송 버튼 클릭 이벤트
        binding.btnSend.setOnClickListener {
            val text = binding.inputMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                adapter.addMessage(ChatMessage(text, true))
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                binding.inputMessage.setText("")

                adapter.addMessage(ChatMessage("AI 도봉: \"$text\" 잘 받았어요!", false))
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            } else {
                Toast.makeText(requireContext(), "메시지를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }
// ✅ 인셋 처리 (IME/시스템바 + 앱 하단바 높이 보정)
        val root = view.findViewById<View>(R.id.chatRoot) ?: binding.root
        val recycler = binding.chatRecyclerView
        val inputBar = binding.inputBar

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())

            // 시스템바/IME 중 큰 값
            val bottomInset = max(sys.bottom, ime.bottom)

            // 🔹 앱 하단 네비게이션뷰 높이: 실측값이 0이면 fallback 사용
            val navView = activity?.findViewById<BottomNavigationView>(R.id.mobile_navigation)
            val fallbackNavH =
                resources.getDimensionPixelSize(R.dimen.bottom_nav_height) // ex) 56dp
            val appNavH =
                if (navView != null && navView.height > 0) navView.height else fallbackNavH

            // 입력바 높이도 초기엔 0일 수 있으니 안전하게 보정
            val inputBarH = if (inputBar.height > 0) inputBar.height
            else inputBar.measuredHeight.takeIf { it > 0 } ?: 0

            recycler.updatePadding(
                left = recycler.paddingLeft,
                top = recycler.paddingTop,
                right = recycler.paddingRight,
                bottom = bottomInset + appNavH + inputBarH
            )

            // 루트에도 하단 패딩 추가(겹침 방지)
            root.updatePadding(
                left = root.paddingLeft,
                top = root.paddingTop,
                right = root.paddingRight,
                bottom = appNavH
            )

            insets
        }

// 🔹 전환 직후/레이아웃 이후 한 번 더 인셋 재요청 (지도→채팅 높이 0 이슈 방지)
        root.post { ViewCompat.requestApplyInsets(root) }
        activity?.findViewById<BottomNavigationView>(R.id.mobile_navigation)?.let { nav ->
            nav.viewTreeObserver.addOnGlobalLayoutListener {
                ViewCompat.requestApplyInsets(root)
            }
        }
    }
        override fun onDestroyView() {
            _binding = null
            super.onDestroyView()
        }
    }
