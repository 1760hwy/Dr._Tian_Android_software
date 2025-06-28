// VideoPlaybackFragment.kt
package com.example.myapplication

import android.Manifest // Video.kt 中有，fragment 中如果直接用就需要
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.tplink.render.common.ScaleMode
import com.tplink.smbdeveplatsdk.SMBCloudSDKConstants
import com.tplink.smbdeveplatsdk.SMBCloudSDKContext
import com.tplink.smbdeveplatsdk.bean.PlayerAllStatus
import com.tplink.smbdeveplatsdk.bean.SMBCloudSDKDeviceBean
import com.tplink.smbdeveplatsdk.constant.Resolution
import com.tplink.smbdeveplatsdk.constant.StreamType
import com.tplink.smbdeveplatsdk.manager.VMSPlayerManager

class VideoPlaybackFragment : Fragment() {

    interface VideoPlaybackListener {
        fun onStatusUpdate(statusMessage: String)
        fun onError(errorMessage: String)
    }

    private var playbackListener: VideoPlaybackListener? = null
    private lateinit var previewContainerInFragment: FrameLayout
    private var playerManager: VMSPlayerManager? = null
    private var currentDeviceBean: SMBCloudSDKDeviceBean? = null
    @Volatile
    private var currentPlayerStatus: Int = SMBCloudSDKConstants.PlayerStatus.stopped
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "VideoPlaybackFragment"
        private const val YOUR_APP_KEY = "34b45db265eb4ecba48494cda4497369"
        private const val YOUR_APP_SECRET = "47646a74e43b4f3eb498a92bbdd47e1e"
        private const val YOUR_CAMERA_QRCODE_ID = "3571839A8604023A1"
        private const val YOUR_CAMERA_MAC_ADDRESS = "4C-10-D5-98-69-4E"

        @JvmStatic
        fun newInstance(): VideoPlaybackFragment {
            return VideoPlaybackFragment()
        }
    }

    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var allGranted = true
            permissions.entries.forEach {
                Log.d(TAG, "Permission: ${it.key}, Granted: ${it.value}")
                if (!it.value) {
                    allGranted = false
                    playbackListener?.onError("${it.key} 权限被拒绝")
                }
            }
            if (allGranted) {
                Log.d(TAG, "所有权限已通过启动器授予。")
                handler.postDelayed({ autoConnect() }, 100)
            } else {
                playbackListener?.onError("部分必要权限被拒绝，无法播放视频")
            }
        }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is VideoPlaybackListener) {
            playbackListener = context
        } else {
            Log.w(TAG, "$context 必须实现 VideoPlaybackListener 接口")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_minimal_video, container, false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewContainerInFragment = view.findViewById(R.id.preview_container_in_fragment)
        Log.d(TAG, "Fragment onViewCreated (Minimal)")
        Log.i(TAG, "假设 SDK 已在 Application 中初始化。")
        checkAndRequestPermissions()
    }

    private fun autoConnect() {
        Log.d(TAG, "开始自动连接 (Minimal) - 假设 AK/SK 已全局配置")
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            playbackListener?.onError("缺少 INTERNET 权限，无法连接摄像头")
            return
        }

        // **不再需要在这里获取 YOUR_APP_KEY, YOUR_APP_SECRET 和调用 SMBCloudSDKContext.setAkSk()**
        // 这些已在 MyApplication 中处理

        val qrCode = YOUR_CAMERA_QRCODE_ID // 设备特定的信息仍然从常量获取
        val mac = YOUR_CAMERA_MAC_ADDRESS.uppercase()

        if (qrCode.isEmpty() || mac.isEmpty()) {
            playbackListener?.onError("摄像头 QRCode 或 MAC 地址缺失")
            return
        }
        if (!isValidMacAddress(mac)) {
            playbackListener?.onError("MAC 地址格式错误")
            return
        }

        // **不需要在这里的 try-catch 中调用 SMBCloudSDKContext.setAkSk()**
        // **也不需要在这里通过 playbackListener 回调 "状态：AK/SK 已设置"**
        // 这个状态的确认现在是应用层面的了

        Log.i(TAG, "autoConnect: proceeding with device-specific connection as AK/SK should be globally set.")

        currentDeviceBean = SMBCloudSDKDeviceBean().apply {
            this.qrCode = qrCode
            this.mac = mac
            this.channelId = 1
        }
        initializeAndStartPreview(currentDeviceBean!!)
    }


    private fun initializeAndStartPreview(deviceBean: SMBCloudSDKDeviceBean) {
        playbackListener?.onStatusUpdate("状态：初始化播放器...")
        try {
            // 与 Video.kt 行为一致，先释放旧的 playerManager (如果存在)
            playerManager?.release()
            playerManager = null

            if (!this::previewContainerInFragment.isInitialized) {
                Log.e(TAG, "previewContainerInFragment 未在 initializeAndStartPreview 之前初始化！")
                playbackListener?.onError("内部视图错误，无法加载视频")
                return
            }
            previewContainerInFragment.removeAllViews() // 清理旧视图

            playerManager = VMSPlayerManager(viewLifecycleOwner.lifecycleScope)
            playerManager?.init(deviceBean)

            playerManager?.videoViewCallback = object : VMSPlayerManager.VideoViewCallback {
                override fun onVideoViewAdd() {
                    activity?.runOnUiThread {
                        if (!isAdded || context == null || !this@VideoPlaybackFragment::previewContainerInFragment.isInitialized) return@runOnUiThread
                        previewContainerInFragment.removeAllViews()
                        playerManager?.addVideoViewSafely(previewContainerInFragment)
                        playbackListener?.onStatusUpdate("状态：视频加载成功")
                    }
                }
                override fun onGetScaleMode(): ScaleMode = ScaleMode.AspectFit
                override fun isHandleTouchEvent(): Boolean = true
            }

            playerManager?.playStatusCallback = object : VMSPlayerManager.PlayStatusCallback {
                override fun onStatusChange(playerAllStatus: PlayerAllStatus) {
                    val statusCode = playerAllStatus.channelStatus
                    val reasonCode = playerAllStatus.channelFinishReason
                    currentPlayerStatus = statusCode
                    val statusText = getStatusText(statusCode, reasonCode)
                    activity?.runOnUiThread {
                        if (!isAdded || context == null) return@runOnUiThread
                        playbackListener?.onStatusUpdate("状态：$statusText")
                    }
                    if (statusCode == SMBCloudSDKConstants.PlayerStatus.fialure) {
                        val errorMsg = playerManager?.getPlayerFinishMessage(reasonCode) ?: "未知错误 ($reasonCode)"
                        activity?.runOnUiThread {
                            if (!isAdded || context == null) return@runOnUiThread
                            playbackListener?.onError("播放失败：$errorMsg")
                        }
                    }
                }
                override fun onVolumeChange(enable: Boolean) {}
                override fun onGetDeviceInfo(errorCode: Int) {}
                override fun onPlayTimeUpdate(progressTime: Long) {}
                override fun onQualityChange(quality: Int) {}
            }
            startCameraPreviewInternal()
        } catch (t: Throwable) {
            Log.e(TAG, "播放器初始化或预览启动时出错", t)
            activity?.runOnUiThread {
                if (!isAdded || context == null) return@runOnUiThread
                playbackListener?.onError("播放器故障：${t.message}")
            }
        }
    }

    private fun startCameraPreviewInternal(resolution: Resolution = Resolution.HD) {
        val currentPlayerManager = playerManager
        if (currentPlayerManager == null) {
            activity?.runOnUiThread { if (isAdded) playbackListener?.onError("播放器未准备好 (startInternal)") }
            return
        }
        activity?.runOnUiThread { if (isAdded) playbackListener?.onStatusUpdate("状态：请求流信息...")}
        try {
            currentPlayerManager.reqGetPlayInfo(
                viewLifecycleOwner.lifecycleScope,
                SMBCloudSDKConstants.ClientType.CLIENT_TYPE_ANDROID,
                StreamType.StreamTypeVideo,
                resolution.value
            ) { errorCode, _, _ -> // moduleSpec, streamUrl 未使用
                if (SMBCloudSDKContext.isRequestSuccess(errorCode)) {
                    // Video.kt 中的逻辑是直接调用 startPreview
                    currentPlayerManager.startPreview(resolution) // !! 确保 VMSPlayerManager 有 startPreview 方法
                } else {
                    val errorMsg = SMBCloudSDKContext.getErrorCodeMessage(errorCode)
                    activity?.runOnUiThread {
                        if (!isAdded || context == null) return@runOnUiThread
                        playbackListener?.onError("获取流信息失败：$errorMsg")
                    }
                }
            }
        } catch (e: Exception) {
            activity?.runOnUiThread {
                if (!isAdded || context == null) return@runOnUiThread
                playbackListener?.onError("请求流信息出错：${e.message}")
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!this::previewContainerInFragment.isInitialized) return
        Log.d(TAG, "屏幕配置变更 (Minimal)")
        previewContainerInFragment.post {
            if (playerManager != null && ::previewContainerInFragment.isInitialized &&
                (currentPlayerStatus == SMBCloudSDKConstants.PlayerStatus.playing ||
                        currentPlayerStatus == SMBCloudSDKConstants.PlayerStatus.paused)) {
                previewContainerInFragment.removeAllViews()
                playerManager?.addVideoViewSafely(previewContainerInFragment)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "Fragment onStop - 通常不在此处 release 播放器，除非App设计如此")
        // 在 Video.kt 中，onStop 时并没有操作 playerManager。
        // 如果你希望在 fragment 不可见时停止视频流（节省资源），
        // 你需要确定 VMSPlayerManager 是否有 pausePreview() 或 stopStream() 之类的方法。
        // 如果没有，并且你调用 release()，那么下次 fragment 可见时需要完整重新初始化。
        // 为简单起见，并且与 Video.kt 行为（依赖onDestroy释放）保持部分一致，
        // 这里暂时不调用 playerManager 的方法。
        // 如果确实需要停止，并且 SDK 没有提供轻量级停止方法，则：
        // playerManager?.release() // 但这意味着下次需要重新初始化
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.w(TAG, "Fragment onDestroyView (Minimal) - 清理视图")
        // 确保视图从容器中移除，防止泄露
        if (this::previewContainerInFragment.isInitialized) {
            previewContainerInFragment.removeAllViews()
        }
        // 在 Video.kt 中，onDestroyView 阶段也没有显式操作 playerManager。
        // playerManager?.stopPreview()  // 如果SDK有此方法，在这里调用是合适的
        // playerManager?.releaseVideoView() // 如果SDK有此方法
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "Fragment onDestroy (Minimal) - 释放所有播放器资源")
        playerManager?.release() // 这是与 Video.kt 一致的主要清理步骤
        playerManager = null
        handler.removeCallbacksAndMessages(null)
    }

    override fun onDetach() {
        super.onDetach()
        playbackListener = null
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.INTERNET)
        }
        // 按需添加其他权限，例如 RECORD_AUDIO
        // if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
        // permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        // }

        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            handler.postDelayed({ autoConnect() }, 100)
        }
    }

    private fun isValidMacAddress(mac: String?): Boolean {
        return mac?.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()) ?: false
    }

    private fun getStatusText(statusCode: Int, reasonCode: Int): String {
        return when (statusCode) {
            SMBCloudSDKConstants.PlayerStatus.loading -> "加载中..."
            SMBCloudSDKConstants.PlayerStatus.playing -> "播放中"
            SMBCloudSDKConstants.PlayerStatus.paused -> "已暂停"
            SMBCloudSDKConstants.PlayerStatus.stopped -> "已停止"
            SMBCloudSDKConstants.PlayerStatus.fialure -> "失败（原因：${getFailureReasonText(reasonCode)}）"
            SMBCloudSDKConstants.PlayerStatus.noStream -> "无流"
            SMBCloudSDKConstants.PlayerStatus.invalid -> "无效"
            else -> "状态未知($statusCode)"
        }
    }

    private fun getFailureReasonText(reasonCode: Int): String {
        val sdkMessage = playerManager?.getPlayerFinishMessage(reasonCode)
        return if (!sdkMessage.isNullOrEmpty() && sdkMessage != reasonCode.toString()) {
            sdkMessage
        } else {
            when (reasonCode) {
                BizMediaFinishReason1.DeviceOffline -> "设备离线"
                BizMediaFinishReason1.AuthFailed -> "认证失败"
                BizMediaFinishReason1.TooManyClient -> "连接数过多"
                // ... 其他 BizMediaFinishReason 常量对应的文本
                else -> "代码:$reasonCode"
            }
        }
    }
}

// !! 再次确认：BizMediaFinishReason 在整个项目中只定义一次 !!
// 如果你的 Video.kt 文件还存在并且其中定义了 BizMediaFinishReason，请删除其中一个。
object BizMediaFinishReason1 {
    const val DeviceOffline = 14
    const val AuthFailed = 1
    const val TooManyClient = 8
    const val Timeout = 0
    const val NoInternetConnection = 16
    const val DeviceUnbind = 15
    const val ServiceUnavailable = 6
}