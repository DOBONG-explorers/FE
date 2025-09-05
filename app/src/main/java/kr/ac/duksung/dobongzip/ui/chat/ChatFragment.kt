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
                // 사용자 메시지 추가
                adapter.addMessage(ChatMessage(text, true))
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)

                binding.inputMessage.setText("")

                // 챗봇 응답 (예시)
                adapter.addMessage(ChatMessage("AI 도봉: \"$text\" 잘 받았어요!", false))
                binding.chatRecyclerView.scrollToPosition(messages.size - 1)
            } else {
                Toast.makeText(requireContext(), "메시지를 입력하세요", Toast.LENGTH_SHORT).show()
            }
        }

        // ✅ 인셋 처리 (IME/네비게이션 바)
        val root = view.findViewById<View>(R.id.chatRoot) ?: binding.root
        val recycler = view.findViewById<View>(R.id.chatRecyclerView)
        val inputBar = view.findViewById<View>(R.id.inputBar)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val sys = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val bottomInset = max(sys.bottom, ime.bottom)

            if (recycler != null && inputBar != null) {
                recycler.updatePadding(
                    left = recycler.paddingLeft,
                    top = recycler.paddingTop,
                    right = recycler.paddingRight,
                    bottom = bottomInset + inputBar.height
                )
            } else {
                root.updatePadding(
                    left = root.paddingLeft,
                    top = root.paddingTop,
                    right = root.paddingRight,
                    bottom = bottomInset
                )
            }
            insets
        }
    }


    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
