package org.allatra.calendar.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


object PictureLoaderHelper {

    /**
     *  this.insertUri = Objects.requireNonNull(insertUri);
    final long now = System.currentTimeMillis() / 1000;
    this.insertValues = new ContentValues();
    this.insertValues.put(MediaColumns.DISPLAY_NAME, Objects.requireNonNull(displayName));
    this.insertValues.put(MediaColumns.MIME_TYPE, Objects.requireNonNull(mimeType));
    this.insertValues.put(MediaColumns.DATE_ADDED, now);
    this.insertValues.put(MediaColumns.DATE_MODIFIED, now);
    this.insertValues.put(MediaColumns.IS_PENDING, 1);
    this.insertValues.put(MediaColumns.DATE_EXPIRES,
    (System.currentTimeMillis() + DateUtils.DAY_IN_MILLIS) / 1000);
     */
    fun getContentValues(displayName: String, mimeType: String): ContentValues{
        val now = System.currentTimeMillis() / 1000;

        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, Objects.requireNonNull(displayName))
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, Objects.requireNonNull(mimeType))
        contentValues.put(MediaStore.MediaColumns.DATE_ADDED, now)
        contentValues.put(MediaStore.MediaColumns.DATE_MODIFIED, now)

        return contentValues
    }


    open fun createPending(
        context: Context,
        insertUri: Uri,
        contentValues: ContentValues
    ): Uri {
        return context.contentResolver.insert(insertUri, contentValues)!!
    }

    fun writeBitmapToLocalFile(bmp: Bitmap, file: File): Uri? {
        var bmpUri: Uri? = null
        try {
            val out = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.close()
            bmpUri = Uri.fromFile(file)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bmpUri
    }

}