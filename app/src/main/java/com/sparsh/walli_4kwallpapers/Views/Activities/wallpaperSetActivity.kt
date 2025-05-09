package com.sparsh.walli_4kwallpapers.Views.Activities

import android.Manifest
import android.app.Dialog
import android.app.WallpaperManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.databinding.ActivityWallpaperSetBinding
import com.sparsh.walli_4kwallpapers.Database.WallpaperDatabaseHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
class wallpaperSetActivity : AppCompatActivity() {
    lateinit var binding: ActivityWallpaperSetBinding
    private var isFavorite: Boolean = false
    private lateinit var dbHelper: WallpaperDatabaseHelper
    private lateinit var url: String
    private var bitmap: Bitmap? = null
    private val playStoreUrl = "https://play.google.com/store/apps/details?id=com.sparsh.walli_4kwallpapers"
    private val STORAGE_PERMISSION_CODE = 100
    private var interstitialAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityWallpaperSetBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@wallpaperSetActivity) {}
        }
        loadInterstitialAd()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        url = intent.getStringExtra("imageUrl") ?: ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val statusBarColor = if (isDarkTheme()) {
                ContextCompat.getColor(this, R.color.status_bar_color_dark)
            } else {
                ContextCompat.getColor(this, R.color.status_bar_color_light)
            }
            window.statusBarColor = statusBarColor
        }

        window?.decorView?.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
        window.statusBarColor = Color.TRANSPARENT

        binding.Favoritebutton.setOnClickListener {
            // Toggle favorite state
            isFavorite = !isFavorite

            // Update UI
            updateFavoriteButtonState()

            // Update database
            lifecycleScope.launch(Dispatchers.IO) {
                if (isFavorite) {
                    dbHelper.addWallpaperToFavorites(url)
                } else {
                    dbHelper.removeWallpaperFromFavorites(url)
                }
            }
            showInterstitialAd()
        }

        binding.applybutton.setOnClickListener {
            showProgressDialog("Applying Wallpaper...") {
                val wallpaperManager = WallpaperManager.getInstance(applicationContext)
                val success = try {
                    wallpaperManager.setBitmap(bitmap)
                    true
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                showToastForWallpaperSetting(success)
            }
            showInterstitialAd()
        }

        binding.savebutton.setOnClickListener {
            checkStoragePermission()
            showInterstitialAd()
        }

        binding.sharebutton.setOnClickListener {
            shareWallpaper()
            showInterstitialAd()
        }

        dbHelper = WallpaperDatabaseHelper(this)
        loadImage()
        isFavorite = dbHelper.checkIfWallpaperIsFavorite(url)
        updateFavoriteButtonState()
    }

    private fun updateFavoriteButtonState() {
        val drawableId = if (isFavorite) R.drawable.icon_favorite_red else R.drawable.icon_favorite_outline
        binding.buttonFavorite.setImageResource(drawableId)
    }

    private fun loadImage() {
        Glide.with(this)
            .asBitmap()
            .load(url)
            .into(object : SimpleTarget<Bitmap>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    bitmap = resource
                    binding.imageViewWallpaper.setImageBitmap(bitmap)
                }
            })
    }

    private fun showProgressDialog(message: String, action: suspend () -> Unit) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_progress)
        dialog.setCancelable(false)
        dialog.findViewById<TextView>(R.id.textViewDialog).text = message
        dialog.show()

        lifecycleScope.launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) { action() }
            dialog.dismiss()
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No need to request permission on Android 10 (API level 29) and above
            saveImage(bitmap!!)
        } else {
            // Request WRITE_EXTERNAL_STORAGE permission for devices below Android 10
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
                saveImage(bitmap!!)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with saving the image
                    saveImage(bitmap!!)
                } else {
                    // Permission denied, show a toast or dialog indicating permission is required
                    showToast("Permission denied, cannot save image")
                }
            }
        }
    }

    private fun saveImage(image: Bitmap) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Use MediaStore for Android 10 (API level 29) and above
            saveImageUsingMediaStore(image, imageFileName)
        } else {
            // Use traditional file operations for Android 9 (API level 28) and below
            saveImageUsingFileOperations(image, imageFileName)
        }
    }

    private fun saveImageUsingMediaStore(image: Bitmap, imageFileName: String) {
        val resolver = contentResolver

        // ContentValues for the image metadata
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$imageFileName.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Walli-4k Wallpapers")
        }

        // Use MediaStore to insert the image
        val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            imageUri?.let { uri ->
                resolver.openOutputStream(uri)?.use { outputStream ->
                    if (!image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)) {
                        showToast("Failed to save image")
                    } else {
                        showToast("Wallpaper Saved")
                    }
                }
            } ?: showToast("Failed to insert image into MediaStore")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to save image")
        }
    }

    private fun saveImageUsingFileOperations(image: Bitmap, imageFileName: String) {
        val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Walli-4k Wallpapers")
        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "$imageFileName.jpg")

        try {
            val outputStream = FileOutputStream(file)
            image.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            showToast("Wallpaper Saved: ${file.absolutePath}")

            // Tell the media scanner to scan the saved image file
            MediaScannerConnection.scanFile(
                applicationContext,
                arrayOf(file.absolutePath),
                arrayOf("image/jpeg"),
                null
            )

        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Failed to save image")
        }
    }




    private fun showToastForWallpaperSetting(success: Boolean) {
        val message = if (success) "Wallpaper set successfully" else "Failed to set wallpaper"
        showToast(message)
    }

    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this@wallpaperSetActivity, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareWallpaper() {
        bitmap?.let { image ->
            try {
                val imagePath = saveBitmapToExternalStorage(image)

                // Prepare the text message to share
                val shareText = "Check out this awesome wallpaper from Walli-HD,4k Wallpapers!\nDownload the app from Play Store: $playStoreUrl"

                // Create an intent to share the image and text
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, FileProvider.getUriForFile(this@wallpaperSetActivity, "${packageName}.fileprovider", File(imagePath)))
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }

                // Grant read permission to the intent
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Start the chooser dialog with a custom title
                startActivity(Intent.createChooser(shareIntent, "Share Wallpaper via"))
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to share wallpaper")
            }
        } ?: showToast("No wallpaper to share")
    }

    private fun saveBitmapToExternalStorage(bitmap: Bitmap): String {
        val fileName = "wallpaper_share.jpg"
        val directory = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Walli-4k Wallpapers")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val file = File(directory, fileName)
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return file.absolutePath
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun loadInterstitialAd() {
        InterstitialAd.load(this, "ca-app-pub-7713317467402311/1377229725", AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d("TAG", "Ad loaded successfully")
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.d("TAG", "Ad failed to load: ${loadAdError.message}")
                    interstitialAd = null
                }
            })
    }


    private fun showInterstitialAd() {
        interstitialAd?.let { ad ->
            ad.show(this)
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    loadInterstitialAd() // Load another ad
                }
            }
        }
    }
}
