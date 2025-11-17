package kr.ac.duksung.dobongzip.ui.notice

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import kr.ac.duksung.dobongzip.R
import java.net.URLEncoder

class PdfViewerActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val webView = findViewById<WebView>(R.id.webViewPdf)
        val pdfUrl = intent.getStringExtra("pdfUrl")
        val title = intent.getStringExtra("title")

        supportActionBar?.title = title ?: "PDF 보기"

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        if (!pdfUrl.isNullOrBlank()) {
            webView.loadUrl(pdfUrl)
        }
    }

    override fun onBackPressed() {
        val webView = findViewById<WebView>(R.id.webViewPdf)
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
