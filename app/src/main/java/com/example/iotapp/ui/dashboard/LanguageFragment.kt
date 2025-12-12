package com.example.iotapp.ui.dashboard

import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.PreferenceHelper
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentLanguageBinding
import com.example.iotapp.model.LanguageOption
import java.util.Locale

class LanguageFragment : BaseFragment<FragmentLanguageBinding>(FragmentLanguageBinding::inflate) {

    private lateinit var adapter: LanguageAdapter

    override fun FragmentLanguageBinding.initView() {
        adapter = LanguageAdapter { option ->
            Log.d(TAG, "Language item clicked ${option.code}, only update UI")
        }
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        adapter.submit(languageOptions(), PreferenceHelper.getLanguage(requireContext()))
    }

    override fun FragmentLanguageBinding.initListener() {
        icTick.setSingleClick {
            val selectedCode = adapter.getSelectedCode()
            if (selectedCode != null) {
                Log.d(TAG, "Save language: $selectedCode")
                PreferenceHelper.saveLanguage(requireContext(), selectedCode)
                applyLocale(selectedCode)
                onBack()
            } else {
                Log.w(TAG, "No language selected")
            }
        }
        icBack.setSingleClick {
            onBack()
        }
    }

    override fun initObserver() = Unit

    private fun languageOptions(): List<LanguageOption> = listOf(
        LanguageOption("en", getString(R.string.english_label), R.drawable.ic_english),
        LanguageOption("vi", getString(R.string.vietnamese_label), R.drawable.ic_vietnam)
    )

    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val resources = requireContext().resources
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        requireActivity().recreate()
    }
}
