package com.example.flashcards.utils

import android.content.Context
import androidx.annotation.StringRes

object ContextUtils {
    var appContext: Context? = null

    fun getString(@StringRes id: Int): String {
        return appContext?.getString(id) ?: ""
    }

    fun getString(@StringRes id: Int, vararg formatArgs: Any): String {
        return appContext?.getString(id, *formatArgs) ?: ""
    }
}
