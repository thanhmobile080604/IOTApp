package com.example.iotapp.base

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.iotapp.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


abstract class BaseDialogFragment<DialogBinding : ViewBinding>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> DialogBinding) :
    DialogFragment() {

    protected lateinit var binding: DialogBinding
    protected val navController by lazy { findNavController() }
    protected open val isFullscreen: Boolean = true
    private val screenName =
        this::class.simpleName?.replace("Fragment", "")?.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            ?.lowercase()?.plus("_screen") ?: "unknown_screen"

    protected open val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBackPressed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater.invoke(inflater, container, false)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
        binding.initView()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initListener()
        initObserver()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        isCancelable = false
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setWindowAnimations(R.style.FadeTransition)
        return dialog
    }

    override fun getTheme(): Int {
        return if (isFullscreen) R.style.DialogFullScreen else R.style.DialogModal
    }

    private val navOptions = NavOptions.Builder().setEnterAnim(R.anim.enter_from_right)
        .setExitAnim(R.anim.exit_to_left)
        .setPopEnterAnim(R.anim.enter_from_left)
        .setPopExitAnim(R.anim.exit_to_right)
        .build()

    protected fun navigateTo(
        id: Int, inclusive: Boolean = false, noAds: Boolean = false, complete: () -> Unit = {},
    ) {
        val action: () -> Unit = {
            complete.invoke()
            try {
                val navOptions = buildNavOptions(inclusive)
                findNavController().navigate(id, null, navOptions)
            } catch (e: Exception) {
                Log.e("NavigationError", "Navigation failed: $e")
                findNavController().navigate(id, null, navOptions)
            }
        }
        checkIfFragmentAttached { action() }
    }

    private fun buildNavOptions(inclusive: Boolean): NavOptions {
        try {
            return NavOptions.Builder().apply {
                val currentDestination = findNavController().currentDestination?.id
                if (inclusive && currentDestination != null) {
                    setPopUpTo(currentDestination, true)
                }
                setEnterAnim(R.anim.enter_from_right)
                setExitAnim(R.anim.exit_to_left)
                setPopEnterAnim(R.anim.enter_from_left)
                setPopExitAnim(R.anim.exit_to_right)
            }.build()
        } catch (e: Exception) {
            Log.e("NavigationError", "Navigation failed: $e")
            return navOptions
        }
    }


    abstract fun DialogBinding.initView()

    abstract fun DialogBinding.initListener()

    abstract fun initObserver()


    override fun onDestroyView() {
        super.onDestroyView()
        backPressedCallback.remove()
    }

    open fun onBackPressed() {
        checkIfFragmentAttached {
            navController.navigateUp()
        }
    }

    fun doRequestPermission(
        permissions: Array<String>,
        listener: IPermissionListener
    ) {
        permissionListener = listener
        launcher.launch(permissions)
    }

    fun openAppSettings() {
        try {
            if (context == null) return
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context?.packageName, null)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var permissionListener: IPermissionListener? = null
    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            val allowList = mutableListOf<String>()
            it.forEach { (k, v) ->
                if (!v) {
                    if (!shouldShowRequestPermissionRationale(k)) {
                        permissionListener?.onNeverAskAgain(k)
                        return@registerForActivityResult
                    }
                } else {
                    allowList.add(k)
                }
            }
            if (allowList.isNotEmpty() && allowList.size == it.size) {
                permissionListener?.onAllow()
            } else {
                permissionListener?.onDenied()
            }
        }

    interface IPermissionListener {
        fun onAllow()
        fun onDenied() {}
        fun onNeverAskAgain(permission: String) {}
    }

    override fun onResume() {
        super.onResume()
    }
}