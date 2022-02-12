package com.example.gallery.ui

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.gallery.databinding.FragmentVideoPlayerBinding

class VideoPlayerFrag : Fragment() {

    private var handler: Handler = Handler(Looper.myLooper()!!)

    private lateinit var mediaController: MediaController
    private lateinit var _binding: FragmentVideoPlayerBinding
    val binding get() = _binding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        if (::_binding.isInitialized) return binding.root
        _binding = FragmentVideoPlayerBinding.inflate(inflater, container, false)
        requireActivity().window.statusBarColor = resources.getColor(android.R.color.black, requireActivity().theme)
        if (requireArguments().getParcelable<Uri>("videoUri") == null) {
            throw Exception("Video Player requires videoUri")
        }
        mediaController = object: MediaController(context) {
            override fun show() {
                super.show()
                binding.tbVideo.visibility = View.VISIBLE
                WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView).show(WindowInsetsCompat.Type.systemBars())
            }

            override fun hide() {
                super.hide()
                binding.tbVideo.visibility = View.GONE
                WindowInsetsControllerCompat(requireActivity().window, requireActivity().window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            }
        }
        mediaController.setAnchorView(binding.videoPlayer)

        binding.videoPlayer.setMediaController(mediaController)
        binding.videoPlayer.setVideoURI(requireArguments().getParcelable("videoUri"))
        binding.videoPlayer.requestFocus()
        binding.videoPlayer.start()

        handler.postDelayed(
            { mediaController.show(0) },
            100
        )
        binding.tbVideo.setNavigationOnClickListener {
            mediaController.hide()
            findNavController().navigateUp()
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        binding.tbVideo.visibility = View.VISIBLE
    }
}