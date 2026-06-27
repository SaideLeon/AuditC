package com.example.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("secur_audit_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_GITHUB_TOKEN = "github_token"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_SELECTED_MODEL = "selected_model"
    }

    var gitHubToken: String?
        get() = prefs.getString(KEY_GITHUB_TOKEN, "")
        set(value) = prefs.edit().putString(KEY_GITHUB_TOKEN, value).apply()

    var geminiApiKey: String?
        get() = prefs.getString(KEY_GEMINI_API_KEY, "")
        set(value) = prefs.edit().putString(KEY_GEMINI_API_KEY, value).apply()

    var selectedModel: String
        get() = prefs.getString(KEY_SELECTED_MODEL, "gemini-3.5-flash") ?: "gemini-3.5-flash"
        set(value) = prefs.edit().putString(KEY_SELECTED_MODEL, value).apply()
}
