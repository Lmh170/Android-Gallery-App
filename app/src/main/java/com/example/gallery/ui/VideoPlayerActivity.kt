package com.example.gallery.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.transition.TransitionManager
import com.example.gallery.R
import com.example.gallery.databinding.ActivityVideoPlayerBinding
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.util.Util
import com.google.android.material.transition.MaterialFade

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var player: ExoPlayer? = null
    private var currentWindow = 0
    private var playbackPosition = 0L
    private var isUiHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = resources.getColor(android.R.color.black, theme)
        }

        binding.tbVideo.setNavigationOnClickListener {
            finish()
        }

        if (intent.data == null) {
            throw Exception("Video Player requires videoUri")
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.exoPlayer.updatePadding(bottom = insets.bottom)
            windowInsets
        }

        binding.exoPlayer.apply {
            setShowNextButton(false)
            setShowPreviousButton(false)
            setControllerVisibilityListener {
                if (binding.exoPlayer.isControllerFullyVisible) {
                    showSystemUi()
                } else {
                    hideSystemUi()
                }
                println("notified ${binding.exoPlayer.isControllerFullyVisible}")
            }
        }
    }

    private fun toggleSystemUi() =
        if (isUiHidden) {
            showSystemUi()
        } else {
            hideSystemUi()
        }

    private fun hideSystemUi() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 180L
        })
        binding.tbVideo.visibility = View.GONE
        WindowInsetsControllerCompat(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        isUiHidden = true
    }

    private fun showSystemUi() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 250L
        })
        binding.tbVideo.visibility = View.VISIBLE
        WindowInsetsControllerCompat(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
        isUiHidden = false
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    override fun onResume() {
        super.onResume()
        if ((Util.SDK_INT < 24 || player == null)) {
            initializePlayer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                binding.exoPlayer.player = exoPlayer
                val mediaItem = MediaItem.fromUri(intent.data!!)
                exoPlayer.setMediaItem(mediaItem)
            }
        player!!.playWhenReady = true
        player!!.seekTo(currentWindow, playbackPosition)
        player!!.prepare()
    }

    private fun releasePlayer() {
        player?.run {
            playbackPosition = this.currentPosition
            currentWindow = this.currentMediaItemIndex
            playWhenReady = this.playWhenReady
            release()
        }
        player = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}