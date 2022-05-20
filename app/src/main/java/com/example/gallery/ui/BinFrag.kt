package com.example.gallery.ui

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.gallery.ListItem
import com.example.gallery.adapter.GridItemAdapter
import com.example.gallery.databinding.FragmentBinBinding
import com.google.android.material.transition.MaterialSharedAxis

class BinFrag : Fragment() {
    private lateinit var _binding: FragmentBinBinding
    private val binding get() = _binding
    private val viewModel: MainViewModel by activityViewModels()

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel.loadBin()

        viewModel.bin.observe(viewLifecycleOwner) {
            val position = (binding.rvBin.layoutManager as GridLayoutManager)
                .findFirstCompletelyVisibleItemPosition()

            (binding.rvBin.adapter as GridItemAdapter).submitList(it) {
                if (position == 0) binding.rvBin.scrollToPosition(0)
            }
        }

        if (::_binding.isInitialized) return binding.root

        _binding = FragmentBinBinding.inflate(inflater)

        binding.rvBin.apply {
            adapter = GridItemAdapter(this@BinFrag, false) { _, position ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val senderRequest = MediaStore.createTrashRequest(
                        requireActivity().application.contentResolver,
                        listOf(((adapter as GridItemAdapter).currentList[position] as ListItem.MediaItem).uri),
                        false
                    ).intentSender

                    val intentSenderRequest =
                        IntentSenderRequest.Builder(senderRequest).build()

                    (requireActivity() as MainActivity).restoreRequest.launch(
                        intentSenderRequest
                    )
                }
            }
            setHasFixedSize(true)
        }

        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true)
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false)

        binding.tbBin.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        return binding.root
    }
}