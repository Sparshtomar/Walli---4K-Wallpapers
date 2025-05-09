package com.sparsh.walli_4kwallpapers.Views.Activities


import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.databinding.ActivityMainBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import com.google.firebase.messaging.FirebaseMessaging
import com.ismaeldivita.chipnavigation.ChipNavigationBar
import com.sparsh.walli_4kwallpapers.Views.Fragments.CategoriesFragment
import com.sparsh.walli_4kwallpapers.Views.Fragments.FavoritesFragment
import com.sparsh.walli_4kwallpapers.Views.Fragments.HomeFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager: ViewPager2
    private lateinit var chipNavigationBar: ChipNavigationBar
    private var lastSelectedMenuItemId: Int = R.id.icon_home

    private lateinit var appUpdateManager: AppUpdateManager
    private val updateType = AppUpdateType.IMMEDIATE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.registerListener(installStateUpdatedlistner)
        }
        checkForAppUpdates()

        setStatusBarColorBasedOnTheme()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkNotificationPermission()
        }

        val backgroundScope = CoroutineScope(Dispatchers.IO)
        backgroundScope.launch {
            // Initialize the Google Mobile Ads SDK on a background thread.
            MobileAds.initialize(this@MainActivity) {}
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT


        viewPager = binding.viewPager
        chipNavigationBar = binding.chipnavbar

        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        viewPager.isUserInputEnabled = false

        // Add a listener to update lastSelectedMenuItemId when changing fragments manually
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                lastSelectedMenuItemId = when (position) {
                    0 -> R.id.icon_home
                    1 -> R.id.icon_categories
                    2 -> R.id.icon_favorites
                    else -> -1
                }
                chipNavigationBar.setItemSelected(lastSelectedMenuItemId)
            }
        })

        chipNavigationBar.setOnItemSelectedListener { itemId ->
            when (itemId) {
                R.id.icon_home -> viewPager.currentItem = 0
                R.id.icon_categories -> viewPager.currentItem = 1
                R.id.icon_favorites -> viewPager.currentItem = 2
                R.id.icon_menu -> showbottomSheetDialog()
            }
        }
        chipNavigationBar.setItemSelected(lastSelectedMenuItemId)
    }

    override fun onResume() {
        super.onResume()
        if (updateType == AppUpdateType.IMMEDIATE) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                    appUpdateManager.startUpdateFlowForResult(
                        info,
                        updateType,
                        this,
                        123
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedFragmentId", lastSelectedMenuItemId)
        super.onSaveInstanceState(outState)
    }

    // The rest of the code remains unchanged

    private fun showbottomSheetDialog() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.bottom_dialog, null)

        val rateAppLayout = view.findViewById<LinearLayout>(R.id.RateAppbtn)
        val favoritesLayout = view.findViewById<LinearLayout>(R.id.Favoritesbtn)
        val removeAdsLayout = view.findViewById<LinearLayout>(R.id.RemoveAdsbtn)
        val settingsLayout = view.findViewById<LinearLayout>(R.id.settingsbtn)

        val playStoreUrl = "https://play.google.com/store/apps/details?id=com.sparsh.walli_4kwallpapers"

        rateAppLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(playStoreUrl))
            startActivity(intent)
        }

        favoritesLayout.setOnClickListener {
            viewPager.currentItem = 2
            dialog.dismiss()
        }

        removeAdsLayout.setOnClickListener {
            Toast.makeText(this, "Coming Soon", Toast.LENGTH_SHORT).show()
        }

        settingsLayout.setOnClickListener {
            val intent = Intent(this@MainActivity, AppSettings::class.java)
            startActivity(intent)
        }

        dialog.setOnDismissListener {
            chipNavigationBar.setItemSelected(lastSelectedMenuItemId)
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> HomeFragment()
                1 -> CategoriesFragment()
                2 -> FavoritesFragment()
                else -> throw IllegalArgumentException("Invalid position: $position")
            }
        }
    }



    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123) {
            when (resultCode) {
                RESULT_OK -> {
                    // The update was successful, handle accordingly
                    Toast.makeText(this, "App updated successfully", Toast.LENGTH_SHORT).show()
                }
                RESULT_CANCELED -> {
                    // The user cancelled the update, force app closure or show a dialog
                    showForceCloseDialog()
                }
                else -> {
                    // The update failed, handle accordingly
                    Toast.makeText(this, "App update failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun checkForAppUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
            val isUpdateAllowed = when(updateType) {
                AppUpdateType.FLEXIBLE -> info.isFlexibleUpdateAllowed
                AppUpdateType.IMMEDIATE -> info.isImmediateUpdateAllowed
                else -> false
            }
            if (isUpdateAvailable && isUpdateAllowed) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    updateType,
                    this,
                    123
                )
            }

        }
    }

    private val installStateUpdatedlistner = InstallStateUpdatedListener { state ->
        if(state.installStatus() == InstallStatus.DOWNLOADED) {
            Toast.makeText(
                applicationContext,
                "Download successful. Restarting app in 5 seconds",
                Toast.LENGTH_LONG
            ).show()
            lifecycleScope.launch {
                delay(5.seconds)
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(updateType == AppUpdateType.FLEXIBLE) {
            appUpdateManager.unregisterListener(installStateUpdatedlistner)
        }
    }

    private fun showForceCloseDialog() {
        val dialogBuilder = AlertDialog.Builder(this) // Use `this` instead of `context`
        dialogBuilder
            .setTitle("Critical Update Required")
            .setMessage("To continue using the app, please update to the latest version.")
            .setCancelable(false)
            .setPositiveButton("Update Now") { dialog, _ ->
                // Optionally, you can restart the update flow here
                checkForAppUpdates()
                dialog.dismiss()
            }
            .setNegativeButton("Exit App") { dialog, _ ->
                // Exit the app if the user chooses not to update (for critical updates)
                dialog.dismiss()
                finish()
                finishAffinity() // Finish all activities in the task
                System.exit(0) // Ensure the app process is terminated
            }

        // Create the AlertDialog
        val alertDialog = dialogBuilder.create()

        // Show the AlertDialog
        alertDialog.show()

        // Set text color for positive button
        if(isDarkTheme()){
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ok_dark))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this,R.color.ok_dark))
        } else {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(ContextCompat.getColor(this, R.color.ok_light))
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(ContextCompat.getColor(this,R.color.ok_light))
        }

        // Set text size for message TextView
        val textView = alertDialog.findViewById<TextView>(android.R.id.message)
        textView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)

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

    private fun checkNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission is not granted, so request it
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Permission is already granted, proceed with subscribing to topic
            subscribeToNotifications()
        }
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with subscribing to topic
                subscribeToNotifications()
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Subscribe to Firebase topic for notifications
    private fun subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("notify")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to notifications"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("notify", msg)
            }
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

}
