package com.example.iotapp.ui.dashboard

import android.util.Log
import android.widget.PopupMenu
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.PreferenceHelper
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentInfoBinding
import com.example.iotapp.repository.AuthRepository
import java.text.DecimalFormat
import java.util.Locale

class InfoFragment : BaseFragment<FragmentInfoBinding>(FragmentInfoBinding::inflate) {

    private var tempUnit: String = "C"
    private var cachedTempC: Double? = null
    private val authRepository = AuthRepository()

    override fun FragmentInfoBinding.initView() {
        tempUnit = PreferenceHelper.getTempUnit(requireContext())
        tvLanguageReal.text = currentLanguageLabel()
        tvTemperatureReal.text = getTempUnitLabel()
    }

    override fun FragmentInfoBinding.initListener() {
        icLanguage.setSingleClick {
            Log.d(TAG, "Navigate to LanguageFragment")
            navigateTo(R.id.languageFragment)
        }
        tvLanguageReal.setSingleClick {
            Log.d(TAG, "Navigate to LanguageFragment")
            navigateTo(R.id.languageFragment)
        }
        tvLanguage.setSingleClick {
            Log.d(TAG, "Navigate to LanguageFragment")
            navigateTo(R.id.languageFragment)
        }
        icMore.setSingleClick {
            Log.d(TAG, "Navigate to LanguageFragment")
            navigateTo(R.id.languageFragment)
        }
        icTemperatureMeter.setSingleClick {
            Log.d(TAG, "Open temperature unit dropdown")
            showTempMenu()
        }

        tvTemperatureReal.setSingleClick {
            Log.d(TAG, "Open temperature unit dropdown")
            showTempMenu()
        }

        tvTemperature.setSingleClick {
            Log.d(TAG, "Open temperature unit dropdown")
            showTempMenu()
        }
        icMore2.setSingleClick {
            Log.d(TAG, "Open temperature unit dropdown")
            showTempMenu()
        }
        logOut.setSingleClick {
            Log.d(TAG, "Log out clicked")
            authRepository.signOut()
            navigateTo(R.id.signInFragment)
        }
    }

    override fun initObserver() = Unit

    override fun onResume() {
        super.onResume()
        tempUnit = PreferenceHelper.getTempUnit(requireContext())
        binding.tvLanguageReal.text = currentLanguageLabel()
        binding.tvTemperatureReal.text = getTempUnitLabel()
    }

    private fun formatTemperature(tempC: Double?): String {
        tempC ?: return "--"
        val df = DecimalFormat("#.##")
        return if (tempUnit.equals("F", true)) {
            val f = tempC * 9 / 5 + 32
            "${df.format(f)} °F"
        } else {
            "${df.format(tempC)} °C"
        }
    }

    private fun showTempMenu() {
        val popup = PopupMenu(requireContext(), binding.icMore2)
        popup.menu.add(0, 0, 0, getString(R.string.celsius_label))
        popup.menu.add(0, 1, 1, getString(R.string.fahrenheit_label))
        popup.setOnMenuItemClickListener { item ->
            tempUnit = if (item.itemId == 1) "F" else "C"
            PreferenceHelper.saveTempUnit(requireContext(), tempUnit)
            binding.tvTemperatureReal.text = getTempUnitLabel()
            true
        }
        popup.show()
    }

    private fun getTempUnitLabel(): String {
        return if (tempUnit.equals("F", true)) {
            getString(R.string.fahrenheit_label)
        } else {
            getString(R.string.celsius_label)
        }
    }

    private fun currentLanguageLabel(): String {
        return when (PreferenceHelper.getLanguage(requireContext())
            .lowercase(Locale.getDefault())) {
            "vi" -> "Tiếng Việt"
            else -> "English"
        }
    }
}
