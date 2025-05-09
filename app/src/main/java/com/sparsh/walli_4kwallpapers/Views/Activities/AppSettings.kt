package com.sparsh.walli_4kwallpapers.Views.Activities

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.FirebaseFirestore
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.databinding.ActivityAppSettingsBinding


@Suppress("DEPRECATION")
class AppSettings : AppCompatActivity() {
    private lateinit var binding: ActivityAppSettingsBinding
    private lateinit var adView: AdView
    private val AD_UNIT_ID = "ca-app-pub-7713317467402311/4160339579"
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    val playStoreUrl = "https://play.google.com/store/apps/details?id=com.sparsh.walli_4kwallpapers"
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityAppSettingsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setStatusBarColorBasedOnTheme()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        loadAd()

        //fetchUniqueTagsFromFirestore()

        binding.settingsbackbtn.setOnClickListener {
            super.onBackPressed()
        }
        binding.PrivacyPolicyCard.setOnClickListener {
            showPrivacyPolicyDialog(this)
        }
        binding.CopyrightCard.setOnClickListener {
            showCopyrightDialog(this)
        }
        binding.termscard.setOnClickListener {
            showtermsDialog(this)
        }
        binding.ContactCard.setOnClickListener {
            sendEmail()
        }
        binding.RateAppCard.setOnClickListener {

                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
                startActivity(intent)
        }
    }


    private fun showCopyrightDialog(context: Context) {
        val privacyPolicyText = context.getString(R.string.copyright)

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(R.string.copyright_title)
            .setMessage(privacyPolicyText)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }


        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

    }

    private fun showtermsDialog(context: Context) {
        val privacyPolicyText = context.getString(R.string.terms)

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(R.string.Terms_title)
            .setMessage(privacyPolicyText)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

    }


    fun showPrivacyPolicyDialog(context: Context) {
        val privacyPolicyText = context.getString(R.string.privacy_policy)

        val dialogBuilder = AlertDialog.Builder(context)
        dialogBuilder
            .setTitle(R.string.privacy_policy_title)
            .setMessage(privacyPolicyText)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                dialog.dismiss()
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(context, R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    private fun sendEmail() {
        val email = "sparshtomar.res@gmail.com"
        val subject = "Regarding Walli - HD, 4K Wallpapers "
        val body = ""

        val uri = Uri.parse("mailto:").buildUpon()
            .appendQueryParameter("to", email)
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()

        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            setPackage("com.google.android.gm")
        }

        try {
            startActivity(intent)
        } catch (ex: ActivityNotFoundException) {
            showAlternativeContactMethod()
        }
    }





    private fun showAlternativeContactMethod() {
        AlertDialog.Builder(this)
            .setTitle("Contact Support")
            .setMessage("No email app is available. You can contact us via email at sparshtomar.res@gmail.com. Would you like to copy the email address?")
            .setPositiveButton("Copy Email") { dialog, _ ->
                // Copy the email address to clipboard
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Email", "sparshtomar.res@gmail.com")
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "Email copied to clipboard", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    private fun loadAd() {
        // Create a new AdView instance
        adView = AdView(this)
        adView.adUnitId = AD_UNIT_ID
        adView.setAdSize(getAdSize(this)) // Use setAdSize() to set the ad size
       // adView.setAdSize(Adapt)

        // Replace ad container with new ad view
        binding.adViewContainer.removeAllViews()
        binding.adViewContainer.addView(adView)

        // Start loading the ad
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }


    private fun getAdSize(context: Context): AdSize {
        val displayMetrics = context.resources.displayMetrics
        var adWidthPixels = displayMetrics.widthPixels

        // Check for Android R (API 30) or later to use WindowMetrics
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = context.getSystemService<WindowManager>()?.currentWindowMetrics
            adWidthPixels = windowMetrics?.bounds?.width() ?: displayMetrics.widthPixels
        }

        val density = displayMetrics.density
        val adWidth = (adWidthPixels / density).toInt()

        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    private fun setStatusBarColorBasedOnTheme() {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        when (currentNightMode) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> {
                // Dark Theme: Set the status bar color to black
                setStatusBarColor(R.color.status_bar_color_dark)
            }
            android.content.res.Configuration.UI_MODE_NIGHT_NO -> {
                // Light Theme: Set the status bar color to white
                setStatusBarColor(R.color.status_bar_color_light)
            }
        }
    }

    private fun setStatusBarColor(colorResId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = ContextCompat.getColor(this, colorResId)
        }

        // For devices running Android 6.0 and above, change the status bar text color to dark for better visibility in light theme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.decorView.systemUiVisibility = if (colorResId == R.color.status_bar_color_light) {
                // Light theme: Make status bar icons dark
                window.decorView.systemUiVisibility or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                // Dark theme: Remove dark status bar icons
                window.decorView.systemUiVisibility and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }



}