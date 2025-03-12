package com.app.secretmessenger

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import android.content.pm.PackageManager
import android.widget.Toast

class IgActivity : AppCompatActivity() {
    private var isLongPressTriggered = false
    private val longPressThreshold = 3000L // 3 seconds
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var downloadButton: Button
    private var videoUrl: String? = null
    private val STORAGE_PERMISSION_CODE = 100
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ig)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("IgActivityPrefs", MODE_PRIVATE)

        // Find the WebView and Button
        val webView = findViewById<WebView>(R.id.InstaWebView)
        downloadButton = findViewById(R.id.downloadButton)

        // Initially hide download button
        downloadButton.visibility = View.GONE

        // Check and request permissions only if not granted and not previously asked
        if (!arePermissionsGranted() && !sharedPreferences.getBoolean("permissions_requested", false)) {
            requestPermissions()
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()
                if (url.contains(".mp4") || url.contains("video")) {
                    videoUrl = url
                    downloadButton.visibility = View.VISIBLE
                }
                return super.shouldOverrideUrlLoading(view, request)
            }
        }

        webView.webChromeClient = WebChromeClient()
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("https://www.instagram.com/")

        downloadButton.setOnClickListener {
            if (arePermissionsGranted()) {
                videoUrl?.let { url ->
                    downloadVideo(url)
                }
            } else {
                requestPermissions()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun arePermissionsGranted(): Boolean {
        val writePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        val readPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        return writePermission == PackageManager.PERMISSION_GRANTED &&
                readPermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )

        // Mark permissions as requested
        with(sharedPreferences.edit()) {
            putBoolean("permissions_requested", true)
            apply()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                videoUrl?.let { downloadVideo(it) }
            } else {
                Toast.makeText(this, "Storage permissions denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun downloadVideo(url: String) {
        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()

        Thread {
            try {
                val connection = URL(url).openConnection()
                connection.connect()

                val fileName = "video_${System.currentTimeMillis()}.mp4"
                val file = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
                )

                val input = connection.getInputStream()
                val output = FileOutputStream(file)

                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }

                output.flush()
                output.close()
                input.close()

                runOnUiThread {
                    Toast.makeText(this, "Video saved to Downloads", Toast.LENGTH_SHORT).show()
                    downloadButton.visibility = View.GONE
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            isLongPressTriggered = false

            // Start a delayed action for long press
            handler.postDelayed({
                isLongPressTriggered = true
                openMainActivity() // Open MainActivity if long press detected
            }, longPressThreshold)

            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            handler.removeCallbacksAndMessages(null) // Cancel long press detection

            if (!isLongPressTriggered) {
                // Short press: Adjust volume
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}