package com.example.gallery.ui

import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.navigation.navArgs
import com.example.gallery.databinding.VideoPlayerBinding

class VideoPlayer : AppCompatActivity() {

    private var handler: Handler = Handler(Looper.myLooper()!!)
    private lateinit var binding: VideoPlayerBinding
    private lateinit var mediaController: MediaController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = VideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.tbVideo)

        supportActionBar.let {
            it?.setDisplayShowTitleEnabled(false)
            it?.setDisplayHomeAsUpEnabled(true)
        }
        window.statusBarColor = resources.getColor(android.R.color.black, theme)

        println("intent: data ${intent.data} type ${intent.type} flags ${intent.flags} action: ${intent.action}" +
                "categories: ${intent.categories} scheme ${intent.scheme} extras size ${intent.extras?.size()}")

        if (intent.data == null) {
            throw Exception("Video Player requires videoUri")
        }

        mediaController = object: MediaController(this) {
            override fun show() {
                super.show()
                supportActionBar?.show()
                WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }

            override fun hide() {
                super.hide()
                supportActionBar?.hide()
                WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        mediaController.setAnchorView(binding.videoPlayer)

        binding.videoPlayer.setMediaController(mediaController)
        binding.videoPlayer.setVideoURI(intent.data)
        binding.videoPlayer.requestFocus()
        binding.videoPlayer.start()

        handler.postDelayed(
            { mediaController.show(0) },
            100
        )
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun finish() {
        super.finish()
        mediaController.hide()
    }

    override fun onResume() {
        super.onResume()
        supportActionBar?.show()
    }
}