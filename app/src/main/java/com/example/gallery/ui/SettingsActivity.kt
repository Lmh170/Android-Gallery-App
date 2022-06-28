package com.example.gallery.ui

import android.os.Bundle
import android.provider.SearchRecentSuggestions
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.example.gallery.MySuggestionProvider
import com.example.gallery.R

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            val btnClearHistory: Preference? = findPreference<Preference>("clear_search_history")
            btnClearHistory?.setOnPreferenceClickListener {
                SearchRecentSuggestions(context, MySuggestionProvider.AUTHORITY, MySuggestionProvider.MODE)
                    .clearHistory()
                return@setOnPreferenceClickListener true
            }
        }
    }
}