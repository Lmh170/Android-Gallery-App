package com.example.gallery.ui

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.annotation.RequiresApi
import androidx.core.app.SharedElementCallback
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gallery.ListItem
import com.example.gallery.MyItemDetailsLookup
import com.example.gallery.MyItemKeyProvider
import com.example.gallery.R
import com.example.gallery.adapter.GridItemAdapter
import com.google.android.material.appbar.AppBarLayout

open class MediaFrag : Fragment() {
    protected var actionMode: ActionMode? = null
    protected val viewModel: MainViewModel by activityViewModels()

    private fun scrollToPosition(recyclerView: RecyclerView) {
        recyclerView.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                recyclerView.removeOnLayoutChangeListener(this)

                val viewAtPosition =
                    recyclerView.layoutManager!!.findViewByPosition(MainActivity.currentListPosition)

                if (viewAtPosition == null || !recyclerView.layoutManager!!
                        .isViewPartiallyVisible(viewAtPosition, true, true)
                ) {
                    recyclerView.post {
                        recyclerView.layoutManager!!.scrollToPosition(MainActivity.currentListPosition)
                        startPostponedEnterTransition()
                    }
                } else {
                    startPostponedEnterTransition()
                }
            }
        })
    }

    private fun setUpSystemBarsLight() {
        val nightModeFlags: Int =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_NO ||
            nightModeFlags == Configuration.UI_MODE_NIGHT_UNDEFINED
        ) {
            WindowInsetsControllerCompat(
                requireActivity().window,
                requireView()
            ).let { controller ->
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    protected fun prepareTransitions(
        recyclerView: RecyclerView,
        appBarLayout: AppBarLayout? = null
    ) {
        setExitSharedElementCallback(
            object : SharedElementCallback() {
                override fun onMapSharedElements(
                    names: List<String>,
                    sharedElements: MutableMap<String, View>
                ) {

                    if (appBarLayout != null && (recyclerView.layoutManager as GridLayoutManager)
                            .findFirstCompletelyVisibleItemPosition() != 0
                    ) {
                        appBarLayout.setExpanded(false, false)
                    }

                    recyclerView.isTransitionGroup = false

                    val selectedViewHolder = recyclerView
                        .findViewHolderForLayoutPosition(MainActivity.currentListPosition) ?: return

                    sharedElements[names[0]] =
                        (selectedViewHolder as GridItemAdapter.MediaItemHolder).binding.image
                }
            }
        )
    }

    protected fun setUpRecyclerViewSelection(
        recyclerView: RecyclerView,
        selectionId: String,
        @MenuRes menuRes: Int = R.menu.contextual_action_bar
    ) {
        val tracker = SelectionTracker.Builder(
            selectionId,
            recyclerView,
            MyItemKeyProvider(viewModel, arguments?.getString("currentAlbumName")),
            MyItemDetailsLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).withSelectionPredicate(object : SelectionTracker.SelectionPredicate<Long>() {

            override fun canSetStateForKey(key: Long, nextState: Boolean): Boolean =
                recyclerView.findViewHolderForItemId(key) != null

            override fun canSelectMultiple(): Boolean =
                true

            override fun canSetStateAtPosition(position: Int, nextState: Boolean): Boolean =
                recyclerView.findViewHolderForLayoutPosition(position) != null

        }).build()

        (recyclerView.adapter as GridItemAdapter).tracker = tracker

        val callback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                activity?.menuInflater?.inflate(menuRes, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean =
                false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                return when (item?.itemId) {
                    R.id.miShare -> {
                        val items = mutableListOf<ListItem.MediaItem>()
                        for (id in tracker.selection) {
                            val selectedItem = (recyclerView.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        ViewPagerFrag.share(items, requireActivity())
                        tracker.clearSelection()
                        actionMode?.finish()
                        true
                    }

                    R.id.miDelete -> {
                        val items = mutableListOf<ListItem.MediaItem>()

                        for (id in tracker.selection) {
                            val selectedItem = (recyclerView.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        viewModel.deleteItems(items)
                        actionMode?.finish()
                        true
                    }

                    R.id.miDeleteFromDevice -> {
                        val items = mutableListOf<ListItem.MediaItem>()

                        for (id in tracker.selection) {
                            val selectedItem = (recyclerView.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        actionMode?.finish()

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) viewModel.permanentlyDeleteItems(items)
                        true
                    }

                    R.id.miRestore -> {
                        val items = mutableListOf<ListItem.MediaItem>()

                        for (id in tracker.selection) {
                            val selectedItem = (recyclerView.adapter as GridItemAdapter)
                                .currentList.find {
                                    it.id == id
                                } as ListItem.MediaItem? ?: return false
                            items.add(selectedItem)
                        }
                        actionMode?.finish()
                        viewModel.deleteItems(items, false)
                        true
                    }

                    else -> false
                }
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                tracker.clearSelection()
                actionMode = null
            }
        }

        tracker.addObserver(object : SelectionTracker.SelectionObserver<Long>() {
            override fun onSelectionChanged() {
                super.onSelectionChanged()

                actionMode?.title = tracker.selection.size().toString()

                if (actionMode == null) {
                    actionMode = requireView().startActionMode(callback)
                } else if (tracker.selection.size() == 0) {
                    actionMode?.finish()
                }
            }
        })

        (recyclerView.adapter as GridItemAdapter).tracker = tracker
    }

    fun onViewCreated(view: View, savedInstanceState: Bundle?, recyclerView: RecyclerView) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()
        setUpSystemBarsLight()
        scrollToPosition(
            recyclerView
        )
    }

    protected fun scrollToFirst(recyclerView: RecyclerView) {
        if ((recyclerView.layoutManager as GridLayoutManager).findFirstCompletelyVisibleItemPosition() == 0) {
            recyclerView.scrollToPosition(0)
        }
    }

    companion object {
        const val binFragID: String = "com.example.gallery.ui.BinFrag"
    }
}