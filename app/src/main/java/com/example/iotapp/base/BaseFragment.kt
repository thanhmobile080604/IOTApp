package com.example.iotapp.base

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import com.example.iotapp.MainViewModel
import com.example.iotapp.R

abstract class BaseFragment<T : ViewBinding>(private val bindingInflater: (LayoutInflater, ViewGroup?, Boolean) -> T) :
    Fragment() {
    var isDestroyView = false
    val TAG = javaClass.name
    private val screenName =
        this::class.simpleName?.replace("Fragment", "")?.replace(Regex("([a-z])([A-Z])"), "$1_$2")
            ?.lowercase()?.plus("_screen") ?: "unknown_screen"
    protected lateinit var binding: T
        private set
    private val navController by lazy { findNavController() }
    val mainViewModel: MainViewModel by activityViewModels()
    private var screenPlayTime: Long = 0L
    private var isBackDisable = false

    protected open val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            onBack()
        }
    }

    protected fun splitList(listString: String): List<String> {
        if (listString.isBlank()) return emptyList()
        val listCircle = listString.split(";").filter { it.isNotBlank() }
        return listCircle
    }

    private var permissionListener: IPermissionListener? = null
    private var lastRequestedPerms: Set<String> = emptySet()

    private val storagePerms = setOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private val launcher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            val critical = lastRequestedPerms.filterNot { it in storagePerms }.toSet()
            val deniedCritical = critical.filter { results[it] != true }

            if (deniedCritical.isEmpty()) {
                permissionListener?.onAllow()
                return@registerForActivityResult
            }

            val neverAsk = deniedCritical.firstOrNull { !shouldShowRequestPermissionRationale(it) }
            if (neverAsk != null) {
                permissionListener?.onNeverAskAgain(neverAsk)
            } else {
                permissionListener?.onDenied()
            }
        }


    private val navOptions = NavOptions.Builder().setEnterAnim(R.anim.enter_from_right)
        .setExitAnim(R.anim.exit_to_left)
        .setPopEnterAnim(R.anim.enter_from_left)
        .setPopExitAnim(R.anim.exit_to_right)
        .build()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = bindingInflater.invoke(inflater, container, false)
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
        binding.initView()
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.initListener()
        initObserver()
    }

    protected fun disableBackPress(isDisable: Boolean) {
        isBackDisable = isDisable
    }

    abstract fun T.initView()

    abstract fun T.initListener()

    abstract fun initObserver()

    open fun onBack() {
        if (!isBackDisable) {
            checkIfFragmentAttached { findNavController().navigateUp() }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        isDestroyView = true
        super.onDestroyView()
        backPressedCallback.remove()
    }

    protected fun navigateUpScreen() {
        checkIfFragmentAttached {
            navController.navigateUp()
        }
    }

//    fun doRequestPermission(
//        permissions: Array<String>,
//        listener: IPermissionListener
//    ) {
//        permissionListener = listener
//        launcher.launch(permissions)
//    }

    fun doRequestPermission(perms: Array<String>, listener: IPermissionListener) {
        lastRequestedPerms = perms.toSet()
        permissionListener = listener
        launcher.launch(perms)
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

    interface IPermissionListener {
        fun onAllow()
        fun onDenied() {}
        fun onNeverAskAgain(permission: String) {}
    }

    protected fun navigateTo(
        id: Int, inclusive: Boolean = false, noAds: Boolean = false, complete: () -> Unit = {}
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
}