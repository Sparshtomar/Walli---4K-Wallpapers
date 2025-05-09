package com.sparsh.walli_4kwallpapers.Views.Fragments

interface RetryListener {
    fun retryLoadWallpapers()
    fun retryLoadMoreWallpapers()
    fun retryLoadWallpapersByTag(tag: String)
    fun retryLoadMoreWallpapersByTag(tag: String)
    fun retryLoadCategories()
    fun retryLoadCategoryWallpapers(categoryName: String)
    fun retryLoadMoreCategoryWallpapers(categoryName: String)
}