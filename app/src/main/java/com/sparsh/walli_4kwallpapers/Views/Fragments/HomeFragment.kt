package com.sparsh.walli_4kwallpapers.Views.Fragments

import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.getSystemService
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.sparsh.walli_4kwallpapers.Data.Wallpaper
import com.sparsh.walli_4kwallpapers.R
import com.sparsh.walli_4kwallpapers.Views.Adapters.TagWallpaperAdapter
import com.sparsh.walli_4kwallpapers.Views.Adapters.WallpaperAdapter
import com.sparsh.walli_4kwallpapers.databinding.FragmentHomeBinding


@Suppress("DEPRECATION")
class HomeFragment : Fragment(), RetryListener {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var db: FirebaseFirestore
    private var wallpaperList: MutableList<Wallpaper> = mutableListOf()
    private var tagwallpaperList: MutableList<Wallpaper> = mutableListOf()

    private var lastVisible: DocumentSnapshot? = null
    private val pageSize = 20
    private var isLastPage = false
    private var isLoading = false
    private var currentTag: String? = null
    private var tagSuggestionsAdapter: ArrayAdapter<String>? = null

    private var allTags: MutableList<String> = mutableListOf()

    private lateinit var wallpaperAdapter: WallpaperAdapter
    private lateinit var tagWallpaperAdapter: TagWallpaperAdapter

    private lateinit var adView: AdView
    private val AD_UNIT_ID = "ca-app-pub-7713317467402311/5037301386"
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        drawLayout()
        binding.tryAgainButton.setOnClickListener {
            drawLayout()
            loadWallpapers()
        }

        binding.tryAgainButtontag.setOnClickListener {
            drawLayoutTag() // Reset UI state if necessary
            currentTag?.let { tag ->
                loadMoreWallpapersByTag(tag) // Load wallpapers by the current tag
            } ?: run {
                // Handle case where currentTag is null (possibly set a default behavior or show an error)
                Log.e("HomeFragment", "currentTag is null when tryAgainButtontag is clicked")
            }
        }


        binding.tryAgainButtonMore.setOnClickListener {
            drawLayoutMore()
            if(isInternetAvailable(requireContext())) {
                loadMoreWallpapers()
            }

        }
        loadAd()



        // Initialize Firestore
        db = FirebaseFirestore.getInstance()

        binding.homerecyclerView.layoutManager = GridLayoutManager(activity, 3)
        wallpaperAdapter = WallpaperAdapter(wallpaperList)
        tagWallpaperAdapter = TagWallpaperAdapter(tagwallpaperList) // Initialize with same list initially

        // Initially set all wallpapers adapter
        binding.homerecyclerView.adapter = wallpaperAdapter



        binding.searchView.setOnCloseListener {
            resetSearchView() // Reset search view state
            false // Return false to allow normal closing behavior
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                showTagSuggestions(newText)
                return true
            }
        })


        // Click listener for the SearchView
        binding.searchView.setOnClickListener {
                expandSearchView()
        }

        // Focus change listener for the SearchView
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                expandSearchView()
                showTagSuggestions(binding.searchView.query.toString())
                binding.searchView.queryHint = "Search Wallpaper"
            } else {
                binding.searchView.queryHint = "Search Wallpaper"
                hideTagSuggestions()
            }
        }



        tagSuggestionsAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1)
        binding.tagSuggestionsListView.adapter = tagSuggestionsAdapter

        binding.tagSuggestionsListView.setOnItemClickListener { parent, view, position, id ->
            val tag = parent.getItemAtPosition(position) as String
            binding.searchView.setQuery(tag, false) // Set query without submitting
            hideTagSuggestions() // Hide suggestions list after selecting a tag
            currentTag = tag
            loadWallpapersByTag(tag) // Load wallpapers for the selected tag
        }

        binding.tagSuggestionsListView.visibility = View.GONE

        // Load initial wallpapers from Firestore
        loadWallpapers()

        // Setup scroll listener for pagination
        setupScrollListener()

        // Fetch tags from Firestore
        fetchTagsFromFirestore()
    }

    override fun retryLoadWallpapers() {
        loadWallpapers()
    }

    override fun retryLoadMoreWallpapers() {
        loadMoreWallpapers()
    }

    override fun retryLoadWallpapersByTag(tag: String) {
        loadWallpapersByTag(tag)
    }

    override fun retryLoadMoreWallpapersByTag(tag: String) {
        loadMoreWallpapersByTag(tag)
    }

    override fun retryLoadCategories() {
        TODO("Not yet implemented")
    }

    override fun retryLoadCategoryWallpapers(categoryName: String) {
        TODO("Not yet implemented")
    }

    override fun retryLoadMoreCategoryWallpapers(categoryName: String) {
        TODO("Not yet implemented")
    }

    private fun expandSearchView() {
        binding.titleTextView.visibility = View.GONE
//        val params = binding.searchView.layoutParams as ViewGroup.MarginLayoutParams
//        params.marginStart = dpToPx(0)
//        params.marginEnd = dpToPx(0)
//        binding.searchView.layoutParams = params
        binding.searchView.isIconified = false // Expand the SearchView
        binding.searchView.requestFocus()
        binding.searchView.requestFocusFromTouch()
        binding.searchView.requestLayout()
    }

    private fun resetSearchView() {
        binding.titleTextView.visibility = View.VISIBLE
//        val params = binding.searchView.layoutParams as ViewGroup.MarginLayoutParams
//        params.marginStart = dpToPx(20)
//        params.marginEnd = dpToPx(16)
//        binding.searchView.layoutParams = params
        binding.searchView.clearFocus()
        binding.searchView.requestLayout()
        hideTagSuggestions()
        currentTag = null
        loadWallpapers()
    }


    private fun showTagSuggestions(query: String?) {
        if (query.isNullOrBlank()) {
            binding.tagSuggestionsListView.visibility = View.GONE
        } else {
            val filteredTags = allTags.filter { it.contains(query, ignoreCase = true) }
            tagSuggestionsAdapter?.clear()
            tagSuggestionsAdapter?.addAll(filteredTags)
            binding.tagSuggestionsListView.visibility = View.VISIBLE
        }
    }

    private fun hideTagSuggestions() {
        binding.tagSuggestionsListView.visibility = View.GONE
    }

    private fun fetchTagsFromFirestore() {
        db.collection("tags").document("jtfuHaE6YbX76PrINzL3")
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val tagsString = document.getString("tag")
                    tagsString?.let {
                        allTags.clear()
                        allTags.addAll(it.split(",").map { tag -> tag.trim() })
                        allTags.sort()
                        updateTagSuggestions()
                    }
                }
            }
            .addOnFailureListener { exception ->
                // Handle the error
            }
    }

    private fun updateTagSuggestions() {
        tagSuggestionsAdapter?.clear()
        tagSuggestionsAdapter?.addAll(allTags)
    }

    private fun loadWallpapers() {


        if (!isInternetAvailable(requireContext())) {
            drawLayout()
            return
        }
        binding.progressBar.visibility = View.VISIBLE

        db = FirebaseFirestore.getInstance()
        db.collection("wallpapers")
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                wallpaperList.clear()
                for (document in querySnapshot.documents) {
                    val wallpaper = document.toObject(Wallpaper::class.java)
                    wallpaper?.let {
                        wallpaperList.add(it)
                    }
                }
                wallpaperAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE
                binding.homerecyclerView.adapter = wallpaperAdapter

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
            }
            .addOnFailureListener {
                isLoading = false
                scheduleRetry(this::retryLoadWallpapers)
                binding.progressBar.visibility = View.GONE
            }
    }


    private fun loadWallpapersByTag(tag: String) {

        if (!isInternetAvailable(requireContext())) {
            drawLayoutTag()
            return
        }
        binding.progressBar.visibility = View.VISIBLE

        db.collection("wallpapers")
            .whereArrayContains("tags", tag)
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
                tagwallpaperList.clear()
                tagwallpaperList.addAll(newWallpapers)

                tagWallpaperAdapter.notifyDataSetChanged()
                binding.progressBar.visibility = View.GONE

                binding.homerecyclerView.adapter = tagWallpaperAdapter

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
            }
            .addOnFailureListener {
                scheduleRetry { retryLoadWallpapersByTag(tag) }
                binding.progressBar.visibility = View.GONE
            }
    }


    private fun loadMoreWallpapers() {
        if (isLoading || isLastPage) return

        isLoading = true
        binding.progressBarPagination.visibility = View.VISIBLE
        db.collection("wallpapers")
            .startAfter(lastVisible!!)
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
                wallpaperList.addAll(newWallpapers)
                wallpaperAdapter.notifyDataSetChanged()
                isLoading = false

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
                binding.progressBarPagination.visibility = View.GONE
                Log.d("HomeFragment", "Loaded ${newWallpapers.size} more wallpapers successfully")
            }
            .addOnFailureListener { e ->
                isLoading = false
                scheduleRetry(this::retryLoadMoreWallpapers)
                binding.progressBarPagination.visibility = View.GONE
                Log.e("HomeFragment","Error fetching more wallpapers", e)
            }
    }

    private fun loadMoreWallpapersByTag(tag: String) {

//        if (!isInternetAvailable(requireContext())) {
//            drawLayoutTag()
//            return
//        }
        if (isLoading || isLastPage) return

        isLoading = true
        binding.progressBarPagination.visibility = View.VISIBLE
        db.collection("wallpapers")
            .whereArrayContains("tags", tag)
            .startAfter(lastVisible!!)
            .limit(pageSize.toLong())
            .get()
            .addOnSuccessListener { querySnapshot ->
                val newtagWallpapers = mutableListOf<Wallpaper>()
                for (document in querySnapshot.documents) {
                    val tagwallpaper = document.toObject(Wallpaper::class.java)
                    tagwallpaper?.let {
                        newtagWallpapers.add(it)
                    }
                }

                // Add new wallpapers to the existing list
                tagwallpaperList.addAll(newtagWallpapers)
                tagWallpaperAdapter.notifyDataSetChanged()

                // Update pagination state
                if (querySnapshot.size() < pageSize) {
                    isLastPage = true
                } else {
                    lastVisible = querySnapshot.documents[querySnapshot.size() - 1]
                    isLastPage = false
                }
                isLoading = false
                binding.progressBarPagination.visibility = View.GONE
                Log.d("HomeFragment", "Loaded ${newtagWallpapers.size} more wallpapers by tag '$tag' successfully")
            }
            .addOnFailureListener { e ->

                isLoading = false
                scheduleRetry { retryLoadMoreWallpapersByTag(tag) }
                Log.e("HomeFragment", "Error fetching more wallpapers by tag", e)
                binding.progressBarPagination.visibility = View.GONE
            }
    }

    private fun setupScrollListener() {
        binding.homerecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
                        if (currentTag != null) {
                                drawLayoutTag()
                            if(isInternetAvailable(requireContext())) {
                                loadMoreWallpapersByTag(currentTag!!)
                            }
                            Log.d("HomeFragment","loadMoreWallpapersByTag fun called")
                        } else {
                            drawLayoutMore()
                            if(isInternetAvailable(requireContext())) {
                                loadMoreWallpapers()
                            }
                            Log.d("HomeFragment","loadMoreWallpapers fun called")
                        }
                    }
                }
            }
        })
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


    private fun drawLayoutTag() {
        if (isInternetAvailable(requireContext())) {
            binding.InternetLayout.visibility = View.VISIBLE
            binding.noInternetLayoutTag.visibility = View.GONE

        } else {
            binding.noInternetLayoutTag.visibility = View.VISIBLE
            binding.InternetLayout.visibility = View.GONE

        }
    }

    private fun drawLayoutMore() {
        if (isInternetAvailable(requireContext())) {
            binding.InternetLayout.visibility = View.VISIBLE
            binding.noInternetLayoutMore.visibility = View.GONE

        } else {
            binding.noInternetLayoutMore.visibility = View.VISIBLE
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

