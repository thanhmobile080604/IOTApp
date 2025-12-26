package com.example.iotapp.ui.dashboard

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.iotapp.R
import com.example.iotapp.base.BaseFragment
import com.example.iotapp.base.setSingleClick
import com.example.iotapp.databinding.FragmentHomeBinding
import kotlin.math.abs

class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private lateinit var pagerAdapter: HomePagerAdapter
    private var xDelta = 0f
    private var yDelta = 0f

    private var actionDownTime: Long = 0
    private var isActionMove: Boolean = false
    private var lastActionDownX = 0f
    private var lastActionDownY = 0f
    private val CLICK_THRESHOLD = 10f
    private val CLICK_TIME_THRESHOLD = 200L

    @SuppressLint("ClickableViewAccessibility")
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

        icChatbot.setOnTouchListener { view, event ->
            val screenWidth = resources.displayMetrics.widthPixels
            val screenHeight = resources.displayMetrics.heightPixels
            val viewWidth = view.width
            val viewHeight = view.height

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xDelta = event.rawX - view.x
                    yDelta = event.rawY - view.y
                    actionDownTime = System.currentTimeMillis()
                    lastActionDownX = event.rawX
                    lastActionDownY = event.rawY
                    isActionMove = false
                    return@setOnTouchListener true
                }

                MotionEvent.ACTION_MOVE -> {
                    val moveX = abs(event.rawX - lastActionDownX)
                    val moveY = abs(event.rawY - lastActionDownY)

                    if (moveX > CLICK_THRESHOLD || moveY > CLICK_THRESHOLD) {
                        isActionMove = true

                        var newX = event.rawX - xDelta
                        var newY = event.rawY - yDelta

                        newX = maxOf(0f, minOf(newX, screenWidth - viewWidth.toFloat()))
                        newY = maxOf(0f, minOf(newY, screenHeight - viewHeight.toFloat()))

                        view.animate()
                            .x(newX)
                            .y(newY)
                            .setDuration(0)
                            .start()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    val actionDuration = System.currentTimeMillis() - actionDownTime
                    val moveX = abs(event.rawX - lastActionDownX)
                    val moveY = abs(event.rawY - lastActionDownY)

                    if (!isActionMove &&
                        actionDuration < CLICK_TIME_THRESHOLD &&
                        moveX < CLICK_THRESHOLD &&
                        moveY < CLICK_THRESHOLD) {

                        Log.d(TAG, "Click detected - Navigate to ChatbotFragment")
                        navigateTo(R.id.chatbotFragment)
                        view.performClick()
                    }
                }

                else -> return@setOnTouchListener false
            }
            true
        }
    }

    override fun initObserver() {
        mainViewModel.isNetworkAvailable.observe(viewLifecycleOwner) { isConnected ->
            binding.loadingOverlay.visibility =
                if (isConnected) View.GONE else View.VISIBLE
        }
    }

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

    override fun onBack() {

    }


    override fun onDestroyView() {
        binding.viewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        super.onDestroyView()
    }
}
