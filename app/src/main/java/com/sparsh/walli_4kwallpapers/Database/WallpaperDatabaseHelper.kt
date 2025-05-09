package com.sparsh.walli_4kwallpapers.Database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WallpaperDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "WallpaperFavorites.db"

        private const val SQL_CREATE_ENTRIES =
            "CREATE TABLE ${WallpaperContract.FavoriteEntry.TABLE_NAME} (" +
                    "${WallpaperContract.FavoriteEntry.COLUMN_NAME_ID} INTEGER PRIMARY KEY," +
                    "${WallpaperContract.FavoriteEntry.COLUMN_NAME_URL} TEXT)"

        private const val SQL_DELETE_ENTRIES = "DROP TABLE IF EXISTS ${WallpaperContract.FavoriteEntry.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_ENTRIES)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_ENTRIES)
        onCreate(db)
    }

    fun addFavorite(url: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(WallpaperContract.FavoriteEntry.COLUMN_NAME_URL, url)
        }
        val newRowId = db.insert(WallpaperContract.FavoriteEntry.TABLE_NAME, null, values)
        db.close()
        return newRowId
    }

    fun removeFavorite(url: String): Int {
        val db = this.writableDatabase
        val selection = "${WallpaperContract.FavoriteEntry.COLUMN_NAME_URL} = ?"
        val selectionArgs = arrayOf(url)
        val deletedRows = db.delete(WallpaperContract.FavoriteEntry.TABLE_NAME, selection, selectionArgs)
        db.close()
        return deletedRows
    }

    fun checkIfWallpaperIsFavorite(url: String): Boolean {
        val db = this.readableDatabase
        val selection = "${WallpaperContract.FavoriteEntry.COLUMN_NAME_URL} = ?"
        val selectionArgs = arrayOf(url)
        val cursor: Cursor = db.query(
            WallpaperContract.FavoriteEntry.TABLE_NAME,
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        )
        val isFavorite = cursor.count > 0
        cursor.close()
        db.close()
        return isFavorite
    }
    fun removeWallpaperFromFavorites(imageUrl: String) {
        val db = this.writableDatabase
        val selection = "${WallpaperContract.FavoriteEntry.COLUMN_NAME_URL} = ?"
        val selectionArgs = arrayOf(imageUrl)
        db.delete(WallpaperContract.FavoriteEntry.TABLE_NAME, selection, selectionArgs)
    }

    fun addWallpaperToFavorites(imageUrl: String) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(WallpaperContract.FavoriteEntry.COLUMN_NAME_URL, imageUrl)
        }
        db.insert(WallpaperContract.FavoriteEntry.TABLE_NAME, null, values)
    }
    fun getAllFavoriteWallpapers(): List<String> {
        val favoriteWallpapers: MutableList<String> = mutableListOf()
        val db = this.readableDatabase
        val selectQuery = "SELECT * FROM ${WallpaperContract.FavoriteEntry.TABLE_NAME}"
        val cursor = db.rawQuery(selectQuery, null)

        cursor.use { cursor ->
            while (cursor.moveToNext()) {
                val url = cursor.getString(cursor.getColumnIndex(WallpaperContract.FavoriteEntry.COLUMN_NAME_URL))
                favoriteWallpapers.add(url)
            }
        }

        return favoriteWallpapers
    }


}
