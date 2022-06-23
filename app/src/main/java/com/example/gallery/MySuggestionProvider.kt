package com.example.gallery

import android.content.SearchRecentSuggestionsProvider

class MySuggestionProvider : SearchRecentSuggestionsProvider() {
    init {
        setupSuggestions(AUTHORITY, MODE)
    }

    companion object {
        const val AUTHORITY: String = "com.example.gallery.MySuggestionProvider"
        const val MODE: Int = DATABASE_MODE_QUERIES
    }
}