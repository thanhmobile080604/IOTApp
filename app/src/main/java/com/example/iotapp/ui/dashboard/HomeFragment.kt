package com.example.iotapp.ui.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentHomeBinding

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var pagerAdapter: HomePagerAdapter

    override fun FragmentHomeBinding.initView() {
        pagerAdapter = HomePagerAdapter(this@HomeFragment)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = pagerAdapter.itemCount
        viewPager.registerOnPageChangeCallback(pageChangeCallback)
        updateTabState(0)
    }

    override fun FragmentHomeBinding.initListener() {
        homeButton.setSingleClick {
            Log.d(TAG, "Home tab clicked")
            viewPager.setCurrentItem(0, true)
        }
        infoButton.setSingleClick {
            Log.d(TAG, "Info tab clicked")
            viewPager.setCurrentItem(1, true)
        }
    }

    override fun initObserver() = Unit

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateTabState(position)
        }
    }

    private fun updateTabState(position: Int) {

        val activeBg = R.drawable.bg_6adf9b_12
        val inactiveBg = R.drawable.bg_346348_12

        binding.homeButton.apply {
            background = ContextCompat.getDrawable(context, if (position == 0) activeBg else inactiveBg)
            imageTintList = if (position == 0)
                ColorStateList.valueOf(Color.BLACK)
            else
                ColorStateList.valueOf(Color.WHITE)
        }

        binding.infoButton.apply {
            background = ContextCompat.getDrawable(context, if (position == 1) activeBg else inactiveBg)
            imageTintList = if (position == 1)
                ColorStateList.valueOf(Color.BLACK)
            else
                ColorStateList.valueOf(Color.WHITE)
        }
    }


    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }
}
