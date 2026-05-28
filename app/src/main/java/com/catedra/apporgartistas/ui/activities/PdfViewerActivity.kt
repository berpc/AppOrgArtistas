package com.catedra.apporgartistas.ui.activities

import android.os.Bundle
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.catedra.apporgartistas.R
import java.net.URLEncoder

class PdfViewerActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_viewer)

        val webView = findViewById<WebView>(R.id.webViewPdf)
        val tvCargando = findViewById<TextView>(R.id.tvCargandoPdf)

        val pdfUrl = intent.getStringExtra("PDF_URL") ?: ""
        val obraTitulo = intent.getStringExtra("OBRA_TITULO") ?: "Partitura"

        supportActionBar?.title = obraTitulo
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            builtInZoomControls = true
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                tvCargando.visibility = View.GONE
            }
        }
        // Requerido para que algunos scripts de Google Docs funcionen bien
        webView.webChromeClient = WebChromeClient()

        val urlCodificada = URLEncoder.encode(pdfUrl, "UTF-8")
        val urlVisorGoogle = "https://docs.google.com/gview?embedded=true&url=$urlCodificada"

        webView.loadUrl(urlVisorGoogle)
    }
    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }


}