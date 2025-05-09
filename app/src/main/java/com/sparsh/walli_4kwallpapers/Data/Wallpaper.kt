package com.sparsh.walli_4kwallpapers.Data

import com.google.firebase.firestore.PropertyName
import java.util.Arrays

data class Wallpaper(
    @get:PropertyName("author") @set:PropertyName("author") var author: String? = null,
    @get:PropertyName("category") @set:PropertyName("category") var category: String? = null,
    @get:PropertyName("source") @set:PropertyName("source") var source: String? = null,
    @get:PropertyName("tag") @set:PropertyName("tag") var tag: String? = null,
    @get:PropertyName("tags") @set:PropertyName("tags") var tags: List<String>? = null,
    @get:PropertyName("url") @set:PropertyName("url") var url: String? = null,
    // Add other fields as needed
) {
    // Default no-argument constructor required by Firestore
    constructor() : this("", "", "", "", listOf(),"")
}

