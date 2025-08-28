package kr.ac.duksung.dobongzip.ui.mypage

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kr.ac.duksung.dobongzip.databinding.FragmentSupportBinding

class MyPageSupportFragment : Fragment() {

    private var _binding: FragmentSupportBinding? = null
    private val binding get() = _binding!!

    private val supportEmail = "support@dobongzip.com"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSupportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뒤로가기
        binding.backButton.setOnClickListener { findNavController().popBackStack() }

        // 이메일 텍스트 클릭: 메일앱 열기
        //binding.tvEmail.setOnClickListener { openEmailClient() }

        // "이메일로 문의하기" 버튼: 메일앱 열기
        binding.btnEmail.setOnClickListener { openEmailClient() }

        // "브라우저에서 Gmail 열기" 버튼: Gmail 웹 열기
        binding.btnGmailWeb.setOnClickListener { openGmailInBrowser() }

        // 길게 눌러 복사 (선택)
        binding.tvEmail.setOnLongClickListener {
            copyToClipboard(supportEmail)
            true
        }
    }

    private fun openEmailClient() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:") // 중요: mailto 스킴
            putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
            putExtra(Intent.EXTRA_SUBJECT, "고객센터 문의")
            putExtra(
                Intent.EXTRA_TEXT,
                "아래 양식에 맞춰 문의 내용을 작성해주세요.\n\n- 닉네임:\n- 사용 기기/OS:\n- 문제 발생 화면:\n- 상세 내용:\n"
            )
        }
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(requireContext(), "메일 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGmailInBrowser() {
        // Gmail 웹 컴포즈 URL (to/subject를 미리 채움)
        val url =
            "https://mail.google.com/mail/?view=cm&fs=1&to=$supportEmail&su=${Uri.encode("고객센터 문의")}"
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    private fun copyToClipboard(text: String) {
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("email", text))
        Toast.makeText(requireContext(), "이메일 주소가 복사되었습니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
