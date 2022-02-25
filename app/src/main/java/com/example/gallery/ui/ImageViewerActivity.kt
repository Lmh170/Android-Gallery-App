package com.example.gallery.ui

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.PersistableBundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.*
import androidx.transition.TransitionManager
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.MediaStoreSignature
import com.example.gallery.GlideApp
import com.example.gallery.ListItem
import com.example.gallery.adapter.ViewPagerAdapter
import com.example.gallery.databinding.ActivityImageViewerBinding
import com.example.gallery.databinding.FragmentViewPagerBinding
import com.example.gallery.databinding.ViewPagerItemHolderBinding
import com.google.android.material.transition.MaterialFade

class ImageViewerActivity: AppCompatActivity() {
    lateinit var binding: ActivityImageViewerBinding
    private var isSystemUiVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tbViewPager.setNavigationOnClickListener {
            finish()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.tbViewPager.updateLayoutParams<ViewGroup.MarginLayoutParams>{
                topMargin = insets.top
            }
            binding.cvShare.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            binding.cvEdit.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = insets.bottom + 5
            }
            windowInsets
        }

        binding.pagerImage.enableZooming()
        GlideApp.with(binding.pagerImage).load(intent.data).into(binding.pagerImage)
        binding.pagerImage.gActivity = this
        binding.pagerImage.setOnClickListener {
            toggleSystemUI()
        }
        binding.cvEdit.setOnClickListener {
            val editIntent = Intent(Intent.ACTION_EDIT)
            editIntent.type = contentResolver?.getType(intent.data!!)
            editIntent.data = intent.data
            editIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(Intent.createChooser(editIntent, "Edit with"))
        }
        binding.cvShare.setOnClickListener {
            ViewPagerFrag.share(ListItem.MediaItem(0L, intent.data!!, "", 0,
                0L, 0, 0), this)
        }
    }

    fun showSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 250L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })
        binding.cvEdit.visibility = View.VISIBLE
        binding.tbViewPager.visibility = View.VISIBLE
        binding.cvShare.visibility = View.VISIBLE
        binding.ivGradTop.visibility = View.VISIBLE
        binding.ivGardBottom.visibility = View.VISIBLE
        WindowInsetsControllerCompat(window, window.decorView)
            .show(WindowInsetsCompat.Type.systemBars())
    }

    fun hideSystemUI() {
        TransitionManager.beginDelayedTransition(binding.root, MaterialFade().apply {
            duration = 180L
            excludeTarget(binding.ivGradTop, true)
            excludeTarget(binding.ivGardBottom, true)
        })

        binding.tbViewPager.visibility = View.GONE
        binding.cvShare.visibility = View.GONE
        binding.cvEdit.visibility = View.GONE
        binding.ivGradTop.visibility = View.GONE
        binding.ivGardBottom.visibility = View.GONE
        WindowInsetsControllerCompat(window, window.decorView)
            .hide(WindowInsetsCompat.Type.systemBars())
    }

    fun toggleSystemUI() {
        if (isSystemUiVisible) hideSystemUI() else showSystemUI()
        isSystemUiVisible = !isSystemUiVisible
    }
}