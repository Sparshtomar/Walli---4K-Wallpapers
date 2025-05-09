package com.sparsh.walli_4kwallpapers.Views.Fragments



import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.sparsh.walli_4kwallpapers.databinding.FragmentFavoritesBinding
import com.sparsh.walli_4kwallpapers.Database.WallpaperDatabaseHelper
import com.sparsh.walli_4kwallpapers.Views.Activities.wallpaperSetActivity
import com.sparsh.walli_4kwallpapers.Views.Adapters.FavoritesAdapter
import com.sparsh.walli_4kwallpapers.Views.Adapters.OnItemClickListener3

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@Suppress("DEPRECATION")
class FavoritesFragment : Fragment(), OnItemClickListener3 {
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentFavoritesBinding? = null
    private val binding get() = _binding!!
    private lateinit var favoritesAdapter: FavoritesAdapter
    private lateinit var dbHelper: WallpaperDatabaseHelper
    private var favoriteImageUrls: MutableList<String> = mutableListOf()

    private lateinit var adView: AdView
    private val AD_UNIT_ID = "ca-app-pub-7713317467402311/4399007134"
    private val TEST_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    companion object {
        private const val REQUEST_CODE_WALLPAPER_SET = 1001

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            FavoritesFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFavoritesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        dbHelper = WallpaperDatabaseHelper(requireContext())

        val recyclerView: RecyclerView = binding.recyclerViewFavorites
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 3)

        updateFavorites()
        loadAd()

        return root
    }

    override fun onResume() {
        super.onResume()
        updateFavorites()
    }

    private fun updateFavorites() {
        favoriteImageUrls = dbHelper.getAllFavoriteWallpapers().toMutableList()
        if (favoriteImageUrls.isEmpty()) {
            binding.emptyTextView.visibility = View.VISIBLE
            binding.recyclerViewFavorites.visibility = View.GONE
        } else {
            binding.emptyTextView.visibility = View.GONE
            binding.recyclerViewFavorites.visibility = View.VISIBLE

            if (!::favoritesAdapter.isInitialized) {
                favoritesAdapter = FavoritesAdapter(requireContext(), favoriteImageUrls, this)
                binding.recyclerViewFavorites.adapter = favoritesAdapter
            } else {
                favoritesAdapter.updateData(favoriteImageUrls)
            }
        }
    }

    override fun onImageClick3(imageUrl: String) {
        val intent = Intent(requireContext(), wallpaperSetActivity::class.java).apply {
            putExtra("imageUrl", imageUrl)
        }
        startActivity(intent)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
