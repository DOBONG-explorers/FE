package kr.ac.duksung.dobongzip.ui.chat

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import kr.ac.duksung.dobongzip.R
import kr.ac.duksung.dobongzip.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.chatRoot.setPadding(0, 0, 0, 0)
        binding.webView.setPadding(0, 0, 0, 0)

        val webView = binding.webView
        val progressBar = binding.progressBar

        // 🔹 하단 바 숨기기
        activity?.findViewById<BottomNavigationView>(R.id.mobile_navigation)?.isVisible = false

        // 🔹 툴바 뒤로가기 버튼
        binding.chatToolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 🔹 WebView 설정
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        // 🔹 WebViewClient (로딩/에러 처리)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                progressBar.isVisible = true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.isVisible = false
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    progressBar.isVisible = false
                    Toast.makeText(requireContext(), "페이지 로드 실패", Toast.LENGTH_SHORT).show()
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                
                if (url.startsWith("http://3.36.34.210:5000/") || url.startsWith("https://3.36.34.210:5000/")) {
                    return false
                }
                
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "링크를 열 수 없습니다", Toast.LENGTH_SHORT).show()
                    return true
                }
            }
        }

        webView.webChromeClient = WebChromeClient()

        // 🔹 URL 로드
        webView.loadUrl("http://3.36.34.210:5000/")

        // ✅ OnBackPressedCallback 방식으로 수정 (에러 해결)
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        parentFragmentManager.popBackStack()
                    }
                }
            }
        )
    }

    override fun onDestroyView() {
        // 하단 바 다시 표시
        activity?.findViewById<BottomNavigationView>(R.id.mobile_navigation)?.isVisible = true

        // WebView 정리
        binding.webView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }

        _binding = null
        super.onDestroyView()
    }
}
