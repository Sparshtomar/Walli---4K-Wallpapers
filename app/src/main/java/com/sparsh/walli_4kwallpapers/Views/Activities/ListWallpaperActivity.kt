package com.sparsh.walli_4kwallpapers.Views.Activities

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sparsh.walli_4kwallpapers.Data.Wallpaper
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.Views.Adapters.ListWallpaperAdapter
import com.sparsh.walli_4kwallpapers.Views.Fragments.RetryListener
import com.sparsh.walli_4kwallpapers.databinding.ActivityListWallpaperBinding
import kotlinx.coroutines.*

@Suppress("DEPRECATION")
class ListWallpaperActivity : AppCompatActivity(), RetryListener {

    private lateinit var binding: ActivityListWallpaperBinding
    private lateinit var ListwallpaperAdapter: ListWallpaperAdapter
    private var ListWallpaperList: MutableList<Wallpaper> = mutableListOf()

    private lateinit var db: FirebaseFirestore
    private var lastVisible: DocumentSnapshot? = null

    private var currentPage = 1
    private val pageSize = 20 // Number of wallpapers to load per page
    private var isLastPage = false
    private var isLoading = false
    private var lastVisibleDocument: DocumentSnapshot? = null

    private lateinit var adView: AdView
    private val AD_UNIT_ID = "ca-app-pub-7713317467402311/8542437711"
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListWallpaperBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBarColorBasedOnTheme()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        loadAd()

        val categoryName = intent.getStringExtra("categoryName")

        drawLayout()
        binding.tryAgainButton.setOnClickListener {
            drawLayout()
            loadWallpapers(categoryName!!)
        }

        binding.tryAgainButtonMore.setOnClickListener {
            drawLayoutMore()
            if(isInternetAvailable(this)){
                loadMoreWallpapers(categoryName!!)
            }

        }


        if (categoryName.isNullOrEmpty()) {
            Log.e("ListWallpaperActivity", "Category name is null or empty")
            finish()
            return
        }

        db = FirebaseFirestore.getInstance()

        binding.recyclerViewWallpapers.layoutManager = GridLayoutManager(this, 3)
        ListwallpaperAdapter = ListWallpaperAdapter(ListWallpaperList)
        binding.recyclerViewWallpapers.adapter = ListwallpaperAdapter

        binding.categoryname.text = categoryName
        binding.categoriesbackbtn.setOnClickListener {
            onBackPressed()
        }

        // Initially show the middle progress bar
        binding.progressBar2.visibility = View.VISIBLE



        loadWallpapers(categoryName)

        setupScrollListener(categoryName)
    }


    private fun loadWallpapers(categoryName: String) {

        if (!isInternetAvailable(this)) {
            drawLayout()
            return
        }
        binding.progressBar2.visibility = View.VISIBLE

        db.collection("wallpapers")
            .whereEqualTo("category", categoryName)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val newWallpapers = mutableListOf<Wallpaper>()
                for (document in querySnapshot.documents) {
                    val wallpaper = document.toObject(Wallpaper::class.java)
                    wallpaper?.let {
                        newWallpapers.add(it)
                    }
                }

                // Replace the entire list with new data
                ListWallpaperList.clear()
                ListWallpaperList.addAll(newWallpapers)

                ListwallpaperAdapter.notifyDataSetChanged()
                binding.progressBar2.visibility = View.GONE

                binding.recyclerViewWallpapers.adapter = ListwallpaperAdapter

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
            }
            .addOnFailureListener {
                scheduleRetry { retryLoadCategoryWallpapers(categoryName) }
                binding.progressBar2.visibility = View.GONE
            }
    }

    private fun loadMoreWallpapers(categoryName: String) {

        val newcatWallpapers = mutableListOf<Wallpaper>()

        if (isLoading || isLastPage) return

        isLoading = true
        binding.progressBarPagination.visibility = View.VISIBLE
        db.collection("wallpapers")
            .whereEqualTo("category",categoryName)
            .startAfter(lastVisible!!)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->

                for (document in querySnapshot.documents) {
                    val catwallpaper = document.toObject(Wallpaper::class.java)
                    catwallpaper?.let {
                        newcatWallpapers.add(it)
                    }
                }

                // Add new wallpapers to the existing list
                ListWallpaperList.addAll(newcatWallpapers)
                ListwallpaperAdapter.notifyDataSetChanged()

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
                isLoading = false
                binding.progressBarPagination.visibility = View.GONE
                Log.d("HomeFragment", "Loaded ${newcatWallpapers.size} more wallpapers by category '$categoryName' successfully")
            }
            .addOnFailureListener { e ->

                isLoading = false
                scheduleRetry { retryLoadMoreCategoryWallpapers(categoryName) }
                Log.e("HomeFragment", "Error fetching more wallpapers by tag", e)
                binding.progressBarPagination.visibility = View.GONE
            }
    }


    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel() // Cancel all coroutines when activity is destroyed
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    override fun retryLoadWallpapers() {
        TODO("Not yet implemented")
    }

    override fun retryLoadMoreWallpapers() {
        TODO("Not yet implemented")
    }

    override fun retryLoadWallpapersByTag(tag: String) {
        TODO("Not yet implemented")
    }

    override fun retryLoadMoreWallpapersByTag(tag: String) {
        TODO("Not yet implemented")
    }

    override fun retryLoadCategories() {
        TODO("Not yet implemented")
    }

    override fun retryLoadCategoryWallpapers(categoryName: String) {
        loadWallpapers(categoryName)
    }

    override fun retryLoadMoreCategoryWallpapers(categoryName: String) {
        loadMoreWallpapers(categoryName)
    }
    private fun scheduleRetry(retryFunction: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isInternetAvailable(this)) {
                retryFunction()
            } else {
                drawLayout()
            }
        }, 3000) // Retry after 3 seconds
    }

    private fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            // For older devices
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
        }
    }


    private fun drawLayout() {
        if (isInternetAvailable(this)) {
            binding.InternetLayout.visibility = View.VISIBLE
            binding.noInternetLayout.visibility = View.GONE

        } else {
            binding.noInternetLayout.visibility = View.VISIBLE
            binding.InternetLayout.visibility = View.GONE

        }
    }


    private fun drawLayoutMore() {
        if (isInternetAvailable(this)) {
            binding.InternetLayout.visibility = View.VISIBLE
            binding.noInternetLayoutMore.visibility = View.GONE

        } else {
            binding.noInternetLayoutMore.visibility = View.VISIBLE
            binding.InternetLayout.visibility = View.GONE

        }
    }

    private fun setupScrollListener(categoryName: String) {
        binding.recyclerViewWallpapers.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if (visibleItemCount + firstVisibleItemPosition >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= pageSize
                    ) {
                        currentPage++
                        drawLayoutMore()
                        if(isInternetAvailable(this@ListWallpaperActivity)) {
                            loadMoreWallpapers(categoryName)
                        }

                    }
                }
            }
        })
    }

    private fun loadAd() {
        // Create a new AdView instance
        adView = AdView(this)
        adView.adUnitId = AD_UNIT_ID
        adView.setAdSize(getAdSize(this)) // Use setAdSize() to set the ad size

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
