package com.example.iotapp

import android.content.Context
import android.content.IntentFilter
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.StrictMode
import android.util.Log
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.iotapp.base.BaseActivity
import com.example.iotapp.base.PreferenceHelper
import com.example.iotapp.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import java.util.Locale

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    lateinit var binding: ActivityMainBinding
    private var handler: Handler? = null
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initLanguage()
        
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom)
            insets
        }
        handler = Handler(Looper.getMainLooper())
        initObserver()
        initListener()
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
        )

    }

    private fun initLanguage() {
        val prefs = getSharedPreferences("iot_prefs", Context.MODE_PRIVATE)
        val savedLanguage = prefs.getString("pref_language", null)
        
        val languageCode = if (savedLanguage.isNullOrBlank()) {
            Log.d(TAG, "No language preference found, setting default to 'en'")
            PreferenceHelper.saveLanguage(this, "en")
            "en"
        } else {
            Log.d(TAG, "Language preference found: $savedLanguage")
            savedLanguage
        }
        
        applyLocale(languageCode)
    }

    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val resources = resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    private fun initListener() {
    }

    private fun initObserver() {
    }


    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onDestroy() {
        handler?.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}