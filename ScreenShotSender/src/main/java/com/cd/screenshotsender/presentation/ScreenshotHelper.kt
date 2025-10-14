package com.cd.screenshotsender.presentation

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import androidx.core.graphics.createBitmap
import com.cd.screenshotsender.presentation.utils.FunctionHelper.showToast

@SuppressLint("InternalInsetResource", "DiscouragedApi")
internal class ScreenshotHelper(
    private val context: Context,
    private val resultCode: Int,
    private val data: Intent
) {
    private val mediaProjectionManager =
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    private var mediaProjection: MediaProjection? = null

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    fun initMediaProjection() {
        if (mediaProjection == null) {
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data)
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    releaseVirtualDisplay()
                    mediaProjection = null
                }
            }, Handler(Looper.getMainLooper()))
        }

        if (virtualDisplay == null) {
            val metrics = context.resources.displayMetrics
            val density = metrics.densityDpi
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "screenshot",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader!!.surface, null, null
            )
        }
    }

    fun captureScreenshot(onScreenCaptured: (imageBitmap: Bitmap) -> Unit) {
        if (imageReader == null) {
            context.showToast("Failed to capture image")
            return
        }
        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image != null) {
                val bitmap = processImage(image)
                image.close()
                onScreenCaptured(bitmap)
            } else {
                context.showToast("Failed to capture image")
            }
        }, 200)
    }

    @SuppressLint("InternalInsetResource", "DiscouragedApi")
    private fun getStatusBarHeight(context: Context): Int {
        val resId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) context.resources.getDimensionPixelSize(resId) else 0
    }

    fun releaseVirtualDisplay() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    private fun processImage(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = createBitmap(image.width + rowPadding / pixelStride, image.height)
        bitmap.copyPixelsFromBuffer(buffer)
        val statusBarHeight = getStatusBarHeight(context)
        return Bitmap.createBitmap(
            bitmap,
            0,
            statusBarHeight,
            bitmap.width,
            bitmap.height - statusBarHeight
        )
    }
}
