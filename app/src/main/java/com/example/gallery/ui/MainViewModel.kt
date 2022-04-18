package com.example.gallery.ui

import android.app.Application
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.gallery.Album
import com.example.gallery.ListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val _recyclerViewItems = MutableLiveData<List<ListItem>>()
    val recyclerViewItems: LiveData<List<ListItem>> get() = _recyclerViewItems

    private val _viewPagerImages = MutableLiveData<List<ListItem.MediaItem>>()
    val viewPagerImages: LiveData<List<ListItem.MediaItem>> get() = _viewPagerImages

    private val _albums = MutableLiveData<List<Album>>()
    val albums: LiveData<List<Album>> get() = _albums

    private var pendingDeleteImage: ListItem.MediaItem? = null
    private val _permissionNeededForDelete = MutableLiveData<IntentSender>()
    val permissionNeededForDelete: LiveData<IntentSender> = _permissionNeededForDelete

    /**
     * Performs a one shot load of images from [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] into
     * the [_recyclerViewItems] [LiveData] above.
     */
    fun loadItems() {
        viewModelScope.launch {
            val imageList = queryImages()
            val viewPagerImageList = extractItems(imageList)
            _viewPagerImages.postValue(viewPagerImageList)
            _recyclerViewItems.postValue(imageList)
            _albums.postValue(getAlbums(viewPagerImageList))
        }
    }

    suspend fun postImages(images: List<ListItem>) {
        viewModelScope.launch {
            _recyclerViewItems.postValue(images)
        }
    }

    fun deleteImage(image: ListItem.MediaItem?) {
        if (image == null) return
        viewModelScope.launch {
            performDeleteImage(image)
        }
    }

    fun deletePendingImage() {
        pendingDeleteImage?.let { image ->
            pendingDeleteImage = null
            deleteImage(image)
        }
    }

    private suspend fun extractItems(items: List<ListItem>): List<ListItem.MediaItem> {
        val viewPagerImages = mutableListOf<ListItem.MediaItem>()
        for (item in items ) {
            if (item is ListItem.MediaItem) viewPagerImages += item
        }
        return viewPagerImages
    }

    private suspend fun queryImages(): List<ListItem> {
        val images = mutableListOf<ListItem>()
        var adapterPosition = -1 // because the first item has the index 0
        var itemPosition = -1 // because the first item has the index 0

        withContext(Dispatchers.IO) {
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_TAKEN,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DURATION,
                    MediaStore.Files.FileColumns.RELATIVE_PATH
                    )
            } else {
                arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Files.FileColumns.DATE_TAKEN,
                    MediaStore.Files.FileColumns.DATE_ADDED,
                    MediaStore.Files.FileColumns.DATE_MODIFIED,
                    MediaStore.Files.FileColumns.DISPLAY_NAME,
                    MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Files.FileColumns._ID,
                    MediaStore.Files.FileColumns.SIZE,
                    MediaStore.Files.FileColumns.DURATION,
                    MediaStore.Files.FileColumns.DATA,
                )
            }

            val selection = (MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "="
                    + MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO)

            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            val contentUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                // TODO()
            }
            getApplication<Application>().contentResolver.query(
                contentUri,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val typeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dateTakenColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                val dateAddedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val buckedDisplayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                val sizeColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val durationColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DURATION)
                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.RELATIVE_PATH)
                } else {
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                }
                var lastDate: Date? = null
                while (cursor.moveToNext()) {
                    val type = cursor.getInt(typeColumn)
                    val id = cursor.getLong(idColumn)
                    var dateAdded = cursor.getLong(dateAddedColumn)
                    var dateTaken = cursor.getLong(dateTakenColumn)
                    var dateModified = cursor.getLong(dateModifiedColumn)

                    // convert seconds to milliseconds
                    if (dateAdded < 1000000000000L) dateAdded *= 1000
                    if (dateTaken < 1000000000000L) dateTaken *= 1000
                    if (dateModified < 1000000000000L) dateModified *= 1000

                    val displayName = cursor.getString(displayNameColumn)
                    val album = cursor.getString(buckedDisplayNameColumn)
                    val size = cursor.getLong(sizeColumn)
                    val duration = cursor.getInt(durationColumn)
                    val uri = if (type == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                        ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    } else {
                        ContentUris.withAppendedId(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    }
                    val path = cursor.getString(pathColumn)
                    val selectedDate = Date(dateAdded)
                    if (lastDate == null || lastDate.date > selectedDate.date || lastDate.month > selectedDate.month
                        || lastDate.year > selectedDate.year)  {
                        images += ListItem.Header(dateAdded)
                        lastDate = selectedDate
                        adapterPosition += 1
                    }
                    itemPosition += 1
                    adapterPosition += 1
                    images += ListItem.MediaItem(displayName, size, id, uri, dateAdded, dateTaken,
                        dateModified, album, duration, type, adapterPosition, itemPosition,
                        path)
                }
            }
        }
        return images
    }

    private suspend fun getAlbums(mediaItems: List<ListItem.MediaItem>?): List<Album> {
        val albums = mutableListOf<Album>()
        if (mediaItems == null) return albums
        withContext(Dispatchers.Main) {
            albums.add(Album("null", MutableLiveData<List<ListItem.MediaItem>>()))

            for (item in mediaItems) {
                for (i in albums.indices) {
                    if (item.album == albums[i].name){
                        break
                    } else if (i == albums.lastIndex){
                        albums.add(Album(item.album, MutableLiveData<List<ListItem.MediaItem>>()))
                    }
                }
            }
            albums.removeAt(0)

            for (album in albums) {
                val items: MutableList<ListItem.MediaItem> = mutableListOf()
                for (item in mediaItems) {
                    if (item.album == album.name) {
                        items.add(item)
                    }
                }
                album._mediaItems.postValue(items)
            }
        }
       return albums


/*


        withContext(Dispatchers.Main){
            albums.add(Album("null", MutableLiveData<List<ListItem.MediaItem>>()))

            for (item in mediaItems) {
                for (i in albums.indices) {
                    if (albums[i].name == item.album) {
                        albums[i]._mediaItems.postValue(listOf(item)) += item
                        break
                    } else if (i == albums.size - 1) {
                        albums += Album(item.album, listOf())
                        albums[i+1].mediaItems += item
                    }
                }
            }
            albums.removeAt(0)
        }
        return albums

 */
    }

    private suspend fun performDeleteImage(image: ListItem.MediaItem) {
        withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val pendingIntent = MediaStore.createTrashRequest(getApplication<Application>().contentResolver,
                        listOf(image.uri), true)
                    pendingDeleteImage = null // because the item will be deleted with request == no further action needed
                    _permissionNeededForDelete.postValue(pendingIntent.intentSender)
                } else {
                    getApplication<Application>().contentResolver.delete(
                        image.uri,
                        "${MediaStore.Files.FileColumns._ID} = ?",
                        arrayOf(image.id.toString())
                    )
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) loadItems()
                }
            } catch (e: SecurityException) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    pendingDeleteImage = image
                   /* val intentSender: IntentSender = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        pendingDeleteImage = null // to avoid two requests
                        MediaStore.createTrashRequest(getApplication<Application>().contentResolver,
                            listOf(image.uri), true).intentSender
                    } else { */
                        val recoverableSecurityException =
                            e as? RecoverableSecurityException
                                ?: throw e
                        val intentSender = recoverableSecurityException.userAction.actionIntent.intentSender
                 //   }
                    //    val recoverableSecurityException =
                          //  e as? RecoverableSecurityException
                           //     ?: throw e

                        // Signal to the Activity that it needs to request permission and
                        // try the delete again if it succeeds.
                    //    pendingDeleteImage = image
                        _permissionNeededForDelete.postValue(
                            intentSender
                        )


                } else {
                    throw e
                }
            } finally {
             //   loadItems()
            }
        }
    }
}