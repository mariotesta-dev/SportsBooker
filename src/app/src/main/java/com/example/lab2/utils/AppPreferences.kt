package com.example.lab2.utils

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val PREF_NAME = "AppPreferences"
    private val PREF_KEY_SHOW_RATING_DIALOG = "ShowRatingDialog"

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    var shouldShowRatingDialog: Boolean
        get() = sharedPreferences.getBoolean(PREF_KEY_SHOW_RATING_DIALOG, true)
        set(show) {
            val editor = sharedPreferences.edit()
            editor.putBoolean(PREF_KEY_SHOW_RATING_DIALOG, show)
            editor.apply()
        }
}