package com.example.gallery.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.transition.TransitionManager
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

    companion object {
        const val KEY_PLAYER_POSITION: String = "current_position"
        const val KEY_PLAYER_WINDOW: String = "current_window"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = resources.getColor(android.R.color.black, theme)

        binding.tbVideo.setNavigationOnClickListener {
            finish()
        }

        if (intent.data == null) {
            throw Exception("Video Player requires videoUri")
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding.exoPlayer.apply {
            setShowNextButton(false)
            setShowPreviousButton(false)

            if (savedInstanceState != null) {
                playbackPosition = savedInstanceState.getLong(KEY_PLAYER_POSITION)
                currentWindow = savedInstanceState.getInt(KEY_PLAYER_WINDOW)
            }

            setControllerVisibilityListener {
                if (binding.exoPlayer.isControllerFullyVisible) {
                    showSystemUi()
                } else {
                    hideSystemUi()
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_PLAYER_POSITION, playbackPosition)
        outState.putInt(KEY_PLAYER_WINDOW, currentWindow)
    }

    private fun hideSystemUi() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 180L
        })

        binding.tbVideo.isVisible = false

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemUi() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 250L
        })

        binding.tbVideo.isVisible = true

        WindowInsetsControllerCompat(
            window,
            window.decorView
        ).show(WindowInsetsCompat.Type.systemBars())
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
            release()
        }

        player = null
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}