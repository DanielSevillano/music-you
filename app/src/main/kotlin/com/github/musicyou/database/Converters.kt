package com.github.musicyou.database

import android.os.Parcel
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaLibraryInfo
import androidx.media3.common.util.UnstableApi
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@OptIn(UnstableApi::class)
@TypeConverters
object Converters {
    @TypeConverter
    fun mediaItemFromByteArray(value: ByteArray?): MediaItem? {
        return value?.let { byteArray ->
            runCatching {
                val parcel = Parcel.obtain()
                parcel.unmarshall(byteArray, 0, byteArray.size)
                parcel.setDataPosition(0)
                val bundle = parcel.readBundle(MediaItem::class.java.classLoader)
                parcel.recycle()

                bundle?.let { MediaItem.fromBundle(it, MediaLibraryInfo.INTERFACE_VERSION) }
            }.getOrNull()
        }
    }

    @TypeConverter
    fun mediaItemToByteArray(mediaItem: MediaItem?): ByteArray? {
        return mediaItem?.toBundle(MediaLibraryInfo.INTERFACE_VERSION)?.let { bundle ->
            val parcel = Parcel.obtain()
            parcel.writeBundle(bundle)
            val bytes = parcel.marshall()
            parcel.recycle()

            bytes
        }
    }
}