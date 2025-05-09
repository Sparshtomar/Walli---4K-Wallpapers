package com.sparsh.walli_4kwallpapers.Database

import android.provider.BaseColumns

object WallpaperContract {

    // Define table contents
    object FavoriteEntry : BaseColumns {
        const val TABLE_NAME = "favorites"
        const val COLUMN_NAME_ID = "id"
        const val COLUMN_NAME_URL = "url"
    }
}
