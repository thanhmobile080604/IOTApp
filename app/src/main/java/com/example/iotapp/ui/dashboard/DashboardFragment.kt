package com.example.iotapp.ui.dashboard

import android.widget.Toast
import androidx.fragment.app.viewModels
import com.example.iotapp.MainViewModel
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.databinding.FragmentDashboardBinding

class DashboardFragment : BaseFragment<FragmentDashboardBinding>(FragmentDashboardBinding::inflate) {

    override fun FragmentDashboardBinding.initView() {
        tvHRValue.text = "70"
        tvHRVValue.text = "60"
    }

    override fun FragmentDashboardBinding.initListener() {

        // Nút Measure Again
        btnMeasure.setOnClickListener {
            Toast.makeText(requireContext(), "Measuring again...", Toast.LENGTH_SHORT).show()
        }

        // Chip chọn Heart Rate
        chipHeartRate.setOnClickListener {
            // TODO: update chart / data
            Toast.makeText(requireContext(), "Heart Rate selected", Toast.LENGTH_SHORT).show()
        }

        chipHRV.setOnClickListener {
            // TODO: update chart / data
            Toast.makeText(requireContext(), "HRV selected", Toast.LENGTH_SHORT).show()
        }

        // SeekBar
        seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                tvHRValue.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        // Tab Week / Month / Year
        tabRange.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                Toast.makeText(requireContext(), "Selected ${tab?.text}", Toast.LENGTH_SHORT).show()
                // TODO: update chart theo tab
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
    }

    override fun initObserver() {
    }
}
