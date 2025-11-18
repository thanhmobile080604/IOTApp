package com.example.iotapp.base

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

fun View?.show() {
    this?.visibility = View.VISIBLE
}

fun View?.hide() {
    this?.visibility = View.GONE
}

fun View?.invisible() {
    this?.visibility = View.INVISIBLE
}

fun Context.isNetworkAvailable(): Boolean {
    val connectivityManager =
        getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    val nw = connectivityManager?.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        //for other device how are able to connect with Ethernet
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        //for check internet over Bluetooth
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
        else -> false
    }

}

fun hasImagePermission(context: Context): Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        }
        else -> {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }
}

 inline fun Fragment.checkIfFragmentAttached(crossinline block: () -> Unit) {
    if (!isAdded || view == null || parentFragmentManager.isStateSaved) return
    activity?.runOnUiThread { block() }
}

//fun getBitmapFromAssets(context: Context, path: String): Bitmap? {
//    return try {
//        val inputStream = context.assets.open(path)
//        BitmapFactory.decodeStream(inputStream)
//    }
//    catch (e: Exception){
//        Log.e("Error", "MINH + error: $e")
//        null
//    }
//}

fun getBitmapFromAssets(
    context: Context,
    path: String,
    scaleToScreenFraction: Float = 1f,
    heapFraction: Float = 1f / 8f,
    prefer565: Boolean = true
): Bitmap? {
    val am = context.assets
    var input: InputStream? = null
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        input = am.open(path)
        BitmapFactory.decodeStream(input, null, bounds)
        input.close()

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val screenMax = getScreenMaxSidePx(context)
        val targetMaxSide = (screenMax * scaleToScreenFraction.coerceIn(0.1f, 1f))
            .roundToInt().coerceAtLeast(1)

        val bytesPerPixel = if (prefer565) 2 else 4
        val maxAllocBytes = (Runtime.getRuntime().maxMemory() * heapFraction.coerceIn(0.05f, 0.5f)).toLong()
        val maxPixelsByHeap = (maxAllocBytes / bytesPerPixel).coerceAtLeast(1L)

        fun calcSample(): Int {
            var s = 1
            while (true) {
                val decW = bounds.outWidth / s
                val decH = bounds.outHeight / s
                val tooLargeBySide = max(decW, decH) > targetMaxSide
                val tooLargeByHeap = (decW.toLong() * decH.toLong()) > maxPixelsByHeap
                if (tooLargeBySide || tooLargeByHeap) {
                    s = s shl 1
                } else break
            }
            return s.coerceAtLeast(1)
        }

        val opts = BitmapFactory.Options().apply {
            inJustDecodeBounds = false
            inSampleSize = calcSample()
            if (prefer565) {
                inPreferredConfig = Bitmap.Config.RGB_565
                inDither = true
            } else {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
        }

        input = am.open(path)
        BitmapFactory.decodeStream(input, null, opts)
    } catch (_: OutOfMemoryError) {
        System.gc(); null
    } catch (e: Exception) {
        Log.e("getBitmapFromAssets", "error: $e"); null
    } finally {
        try { input?.close() } catch (_: Exception) {}
    }
}

@Suppress("DEPRECATION")
private fun getScreenMaxSidePx(context: Context): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val wm = context.getSystemService(WindowManager::class.java)
        val b = wm.currentWindowMetrics.bounds
        max(b.width(), b.height())
    } else {
        val dm = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getRealMetrics(dm)
        max(dm.widthPixels, dm.heightPixels)
    }
}


fun decodeScaledBitmapFromAsset(
    context: Context,
    fileName: String,
    reqWidth: Int,
    reqHeight: Int,
): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.assets.open(fileName).use {
            BitmapFactory.decodeStream(it, null, options)
        }

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        options.inJustDecodeBounds = false
        options.inPreferredConfig = Bitmap.Config.RGB_565

        context.assets.open(fileName).use {
            BitmapFactory.decodeStream(it, null, options)!!
        }
    } catch (e: Exception) {
        Log.e("Error", "MINH + error: $e")
        null
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (width, height) = options.outWidth to options.outHeight
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun getBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        } else {
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    } catch (e: IOException) {
        e.printStackTrace()
        null
    }
}

fun removeWhiteBackground(bitmap: Bitmap, tolerance: Int = 10): Bitmap {
    val width = bitmap.width
    val height = bitmap.height

    val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

    val pixels = IntArray(width * height)
    result.getPixels(pixels, 0, width, 0, 0, width, height)

    for (i in pixels.indices) {
        val color = pixels[i]

        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF

        if (r >= 255 - tolerance && g >= 255 - tolerance && b >= 255 - tolerance) {
            pixels[i] = color and 0x00FFFFFF
        }
    }

    result.setPixels(pixels, 0, width, 0, 0, width, height)
    return result
}

fun View.setSingleClick(
    clickSpendTime: Long = 500L,
    execution: () -> Unit
) {
    setOnClickListener(object : View.OnClickListener {
        var lastClickTime: Long = 0
        override fun onClick(p0: View?) {
            if (SystemClock.elapsedRealtime() - lastClickTime < clickSpendTime) {
                return
            }
            lastClickTime = SystemClock.elapsedRealtime()
            execution.invoke()
        }
    })
}

val Int.dp: Int
    get() = (this * Resources.getSystem().displayMetrics.density + 0.5f).toInt()

fun getJsonFromAssets(context: Context, fileDirectory: String): String? {
    var jsonString: String? = null
    try {
        val inputStream = context.assets.open(fileDirectory)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()
        jsonString = String(buffer, StandardCharsets.UTF_8)
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return jsonString
}

fun loadImageFromAsset(context: Context, directory: String, intoView: ImageView) {
    try {
        Glide.with(context).load(
            "file:///android_asset/$directory"
        ).into(intoView)
    } catch (e: Exception) {
        Log.e("Error", "Error: $e")
    }

}

fun loadImageFromAssetFixed(context: Context, directory: String, intoView: ImageView, width: Int, height: Int) {
    try {
        Glide.with(context).load(
            "file:///android_asset/$directory"
        ).override(width, height).diskCacheStrategy(DiskCacheStrategy.AUTOMATIC).into(intoView)
    } catch (e: Exception) {
        Log.e("Error", "Error: $e")
    }

}

fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
    return try {
        val inputStream = context.assets.open(fileName)
        BitmapFactory.decodeStream(inputStream)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun resizeForSketch(bm: Bitmap, context: Context): Bitmap {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val heapLimit = am.memoryClass * 1024 * 1024L
    val bytesPerPixel = 24.0
    val safety = 0.35
    val maxPixels = (heapLimit * safety / bytesPerPixel).toLong()
    val curPixels = bm.width.toLong() * bm.height.toLong()
    if (curPixels <= maxPixels) return bm

    val scale = sqrt(maxPixels.toDouble() / curPixels.toDouble())
    val newW = maxOf(16, (bm.width * scale).toInt())
    val newH = maxOf(16, (bm.height * scale).toInt())
    return bm.scale(newW, newH)
}

@SuppressLint("DiscouragedApi")
fun Context.getLinkFromRaw(name: String): String? {
    return try {
        val resourceId =
            resources.getIdentifier(name, "raw", packageName)
        "android.resource://${packageName}/$resourceId".toUri().toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


fun loadBitmapFromUri(context: Context, uri: Uri): Bitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            val bm = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            val orientation = readExifOrientation(context, uri)
            applyExifToBitmap(bm, orientation)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}


private fun readExifOrientation(ctx: Context, uri: Uri): Int {
    try {
        ctx.contentResolver.openInputStream(uri)?.use { ins ->
            return ExifInterface(ins).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    } catch (_: Exception) {}

    try {
        val p = arrayOf(MediaStore.Images.ImageColumns.ORIENTATION)
        ctx.contentResolver.query(uri, p, null, null, null)?.use { c ->
            val idx = c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION)
            if (c.moveToFirst()) {
                return when (c.getInt(idx)) {
                    90  -> ExifInterface.ORIENTATION_ROTATE_90
                    180 -> ExifInterface.ORIENTATION_ROTATE_180
                    270 -> ExifInterface.ORIENTATION_ROTATE_270
                    else -> ExifInterface.ORIENTATION_NORMAL
                }
            }
        }
    } catch (_: Exception) {}

    return ExifInterface.ORIENTATION_NORMAL
}

private fun applyExifToBitmap(src: Bitmap, exifOrientation: Int): Bitmap {
    val m = Matrix()
    when (exifOrientation) {
        ExifInterface.ORIENTATION_ROTATE_90        -> m.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180       -> m.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270       -> m.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL  -> m.postScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL    -> m.postScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE        -> { m.postRotate(90f);  m.postScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE       -> { m.postRotate(270f); m.postScale(-1f, 1f) }
        else -> return src
    }
    return try {
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out !== src) src.recycle()
        out
    } catch (_: OutOfMemoryError) {
        src
    }
}





