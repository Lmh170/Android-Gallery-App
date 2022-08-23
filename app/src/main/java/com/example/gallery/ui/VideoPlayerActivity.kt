package com.example.gallery.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.example.gallery.databinding.ActivityVideoPlayerBinding

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var source: Uri? = null

    private var handler: Handler = Handler(Looper.myLooper()!!)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.tbVideoPlayer)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        if (intent.data == null) {
            throw Exception("Video Player requires videoUri")
        }

        source = intent.data

        val videoView = binding.videoPlayer

        val mediaController = object : MediaController(this) {
            override fun show() {
                super.show()
                supportActionBar?.show()
            }

            override fun hide() {
                super.hide()
                supportActionBar?.hide()
            }
        }
        mediaController.setAnchorView(videoView)

        videoView.setMediaController(mediaController)
        videoView.setVideoURI(source)
        videoView.requestFocus()
        videoView.start()

        handler.postDelayed(
            { mediaController.show(0) },
            100
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onResume() {
        super.onResume()
        supportActionBar?.show()
    }
}