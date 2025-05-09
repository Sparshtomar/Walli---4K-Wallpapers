package com.sparsh.walli_4kwallpapers.Views.Fragments

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sparsh.walli_4kwallpapers.databinding.FragmentCategoriesBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.sparsh.walli_4kwallpapers.Views.Activities.ListWallpaperActivity
import com.sparsh.walli_4kwallpapers.Views.Adapters.CategoryAdapter

@Suppress("DEPRECATION")
class CategoriesFragment : Fragment(), RetryListener {

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var binding: FragmentCategoriesBinding
    private var categoryList: MutableList<Pair<String, String>> = mutableListOf()

    private lateinit var db: FirebaseFirestore
    private lateinit var adView: AdView
    private val AD_UNIT_ID = "ca-app-pub-7713317467402311/4402876563"
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentCategoriesBinding.inflate(inflater, container, false)

        drawLayout()
        binding.tryAgainButton.setOnClickListener {
            drawLayout()
            loadCategories()
        }
        loadAd()

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        // Initialize RecyclerView and Adapter
        binding.recyclerViewCategories.layoutManager = GridLayoutManager(activity, 2)
        categoryAdapter = CategoryAdapter(categoryList) { category ->
            navigateToCategoryDetail(category)
        }
        binding.recyclerViewCategories.adapter = categoryAdapter

        // Load categories from Firestore
        loadCategories()
        return binding.root
    }

    private fun loadCategories() {
        binding.progressBar.visibility = View.VISIBLE  // Show ProgressBar

        db.collection("categories")
            .get()
            .addOnSuccessListener { querySnapshot ->
                categoryList.clear()

                for (document in querySnapshot.documents) {
                    val categoryName = document.getString("categoryName")
                    val imageUrl = document.getString("imageUrl")

                    if (categoryName != null && imageUrl != null) {
                        categoryList.add(Pair(categoryName, imageUrl))
                    }
                }

                categoryAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE  // Hide ProgressBar
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching categories", e)
                scheduleRetry { retryLoadCategories() }
                binding.progressBar.visibility = View.GONE  // Hide ProgressBar on failure
            }
    }

    private fun navigateToCategoryDetail(category: String) {
        val intent = Intent(activity, ListWallpaperActivity::class.java)
        intent.putExtra("categoryName", category)
        startActivity(intent)
    }

    private fun scheduleRetry(retryFunction: () -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            if (isInternetAvailable(requireContext())) {
                retryFunction()
            } else {
                drawLayout()
            }
        }, 3000) // Retry after 3 seconds
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
        loadCategories()
    }

    override fun retryLoadCategoryWallpapers(categoryName: String) {
        TODO("Not yet implemented")
    }

    override fun retryLoadMoreCategoryWallpapers(categoryName: String) {
        TODO("Not yet implemented")
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
        if (isInternetAvailable(requireContext())) {
            binding.InternetLayout.visibility = View.VISIBLE
            binding.noInternetLayout.visibility = View.GONE

        } else {
            binding.noInternetLayout.visibility = View.VISIBLE
            binding.InternetLayout.visibility = View.GONE

        }
    }
    private fun loadAd() {
        // Create a new AdView instance
        adView = AdView(this.requireContext())
        adView.adUnitId = AD_UNIT_ID
        adView.setAdSize(getAdSize(this.requireContext())) // Use setAdSize() to set the ad size

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
}
