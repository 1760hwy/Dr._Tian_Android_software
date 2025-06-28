package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.tplink.render.common.ScaleMode
import com.example.myapplication.databinding.ActivityVideoBinding
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.tplink.smbdeveplatsdk.SMBCloudSDKConstants
import com.tplink.smbdeveplatsdk.SMBCloudSDKContext
import com.tplink.smbdeveplatsdk.bean.PlayerAllStatus
import com.tplink.smbdeveplatsdk.bean.SMBCloudSDKDeviceBean
import com.tplink.smbdeveplatsdk.constant.Resolution
import com.tplink.smbdeveplatsdk.constant.StreamType
import com.tplink.smbdeveplatsdk.manager.VMSPlayerManager

class Video : AppCompatActivity() {

    private lateinit var binding: ActivityVideoBinding
    private var playerManager: VMSPlayerManager? = null
    private var currentDeviceBean: SMBCloudSDKDeviceBean? = null
    @Volatile
    private var currentPlayerStatus: Int = SMBCloudSDKConstants.PlayerStatus.stopped
    private val handler = Handler(Looper.getMainLooper())

    //ML Kit
    private lateinit var faceDetector:FaceDetector
    private lateinit var faceOverlayView:FaceOverlayView

    companion object {
        private const val TAG = "VideoActivity"
        private const val PERMISSION_REQUEST_CODE = 101
        private const val YOUR_APP_KEY = "34b45db265eb4ecba48494cda4497369"
        private const val YOUR_APP_SECRET = "47646a74e43b4f3eb498a92bbdd47e1e"
        private const val YOUR_CAMERA_QRCODE_ID = "3571839A8604023A1"
        private const val YOUR_CAMERA_MAC_ADDRESS = "4C-10-D5-98-69-4E"
        private const val SPEED_DEFAULT = 7 // 云台默认速度：取值范围1~7
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityVideoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "Video Activity 创建成功")
        } catch (e: Exception) {
            Log.e(TAG, "ViewBinding 初始化失败", e)
            showToast("界面加载失败：${e.message}")
            finish()
            return
        }

        val faceDetectorOptions=FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)//精度优先
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()

        faceDetector=FaceDetection.getClient(faceDetectorOptions)

        //初始化FaJAVA文件
        faceOverlayView=binding.faceOverlay

        // 设置 UI（包括云台控制按钮）
        setupUI()

        // 初始化 SDK
        try {
            SMBCloudSDKContext.init(applicationContext)
            Log.i(TAG, "SDK 初始化成功")
        } catch (e: Exception) {
            Log.e(TAG, "SDK 初始化失败", e)
            showToast("SDK 初始化失败：${e.message}")
            return
        }

        // 检查权限并延迟自动连接
        checkAndRequestPermissions()
    }

    private fun setupUI() {
        // 填充输入框
        binding.etAk.setText(YOUR_APP_KEY)
        binding.etSk.setText(YOUR_APP_SECRET)
        binding.etQrcode.setText(YOUR_CAMERA_QRCODE_ID)
        binding.etMac.setText(YOUR_CAMERA_MAC_ADDRESS)

        // 返回按钮
        binding.btnBack.setOnClickListener {
            Log.d(TAG, "点击返回按钮")
            finish()
        }

        binding.btnPtzUp.setOnClickListener{
            Toast.makeText(this,"按钮需要长按",Toast.LENGTH_SHORT).show()
        }

        binding.btnPtzDown.setOnClickListener{
            Toast.makeText(this,"按钮需要长按",Toast.LENGTH_SHORT).show()
        }

        binding.btnPtzLeft.setOnClickListener{
            Toast.makeText(this,"按钮需要长按",Toast.LENGTH_SHORT).show()
        }

        binding.btnPtzRight.setOnClickListener{
            Toast.makeText(this,"按钮需要长按",Toast.LENGTH_SHORT).show()
        }

        // 设置云台控制按钮的触摸监听
        // 注意：请确保布局中有对应 id 的按钮控件 btn_ptz_up、btn_ptz_down、btn_ptz_left、btn_ptz_right
        binding.btnPtzUp.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> reqPtzMotionCtrl(direction = 7)  // 向上（方向代码 7）
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reqPtzMotionCtrl(direction = 0) // 停止
            }
            true
        }

        binding.btnPtzDown.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> reqPtzMotionCtrl(direction = 3)  // 向下（方向代码 3）
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reqPtzMotionCtrl(direction = 0)
            }
            true
        }

        binding.btnPtzLeft.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> reqPtzMotionCtrl(direction = 5)  // 向左（方向代码 5）
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reqPtzMotionCtrl(direction = 0)
            }
            true
        }

        binding.btnPtzRight.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> reqPtzMotionCtrl(direction = 1)  // 向右（方向代码 1）
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> reqPtzMotionCtrl(direction = 0)
            }
            true
        }
    }

    /**
     * 调用云台操作接口，控制摄像头的云台运动。
     * @param direction 云台方向代码：上(7)、下(3)、左(5)、右(1)，停止操作传 0。
     * @param speed 速度，默认取值 SPEED_DEFAULT（范围1~7）。
     */
    private fun reqPtzMotionCtrl(direction: Int, speed: Int = SPEED_DEFAULT) {
        SMBCloudSDKContext.reqPtzMotionCtrl(
            SMBCloudSDKContext.getLifecycleScope(this),
            binding.etQrcode.text.toString(),         // 设备二维码
            binding.etMac.text.toString().uppercase(),  // MAC 地址（确保大写）
            1,                                          // 通道号（直连 IPC 默认1）
            direction,
            speed
        ) { errorCode ->
            if (!SMBCloudSDKContext.isRequestSuccess(errorCode)) {
                showToast(SMBCloudSDKContext.getErrorCodeMessage(errorCode))
                Log.e(TAG, "云台操作失败，direction=$direction, errorCode=$errorCode")
            } else {
                Log.d(TAG, "云台操作成功，direction=$direction")
            }
        }
    }

    private fun autoConnect() {
        Log.d(TAG, "开始自动连接")
        if (!hasRequiredPermissions()) {
            Log.w(TAG, "权限不足，取消自动连接")
            showToast("缺少必要权限，无法连接摄像头")
            return
        }

        val ak = YOUR_APP_KEY
        val sk = YOUR_APP_SECRET
        val qrCode = YOUR_CAMERA_QRCODE_ID
        val mac = YOUR_CAMERA_MAC_ADDRESS.uppercase()

        if (ak.isEmpty() || sk.isEmpty() || qrCode.isEmpty() || mac.isEmpty()) {
            Log.e(TAG, "配置字段缺失")
            showToast("配置字段缺失（AK、SK、QRCode、MAC）")
            return
        }
        if (!isValidMacAddress(mac)) {
            Log.e(TAG, "MAC 地址格式错误: $mac")
            showToast("MAC 地址格式错误（应为 XX-XX-XX-XX-XX-XX）")
            return
        }

        try {
            SMBCloudSDKContext.setAkSk(ak, sk)
            updateStatus("状态：AK/SK 已设置，准备设备...")
            Log.i(TAG, "AK/SK 设置成功")
        } catch (e: Exception) {
            Log.e(TAG, "设置 AK/SK 失败", e)
            showToast("设置 AK/SK 失败：${e.message}")
            return
        }

        currentDeviceBean = SMBCloudSDKDeviceBean().apply {
            this.qrCode = qrCode
            this.mac = mac
            this.channelId = 1
            Log.d(TAG, "创建设备信息：QR=$qrCode, MAC=$mac, Channel=1")
        }

        initializeAndStartPreview(currentDeviceBean!!)
    }

    private fun initializeAndStartPreview(deviceBean: SMBCloudSDKDeviceBean) {
        updateStatus("状态：正在初始化播放器...")
        Log.i(TAG, "开始初始化播放器...")

        try {
            playerManager?.release()
            playerManager = null
            binding.previewContainer.removeAllViews()

            playerManager = VMSPlayerManager(lifecycleScope)
            playerManager?.init(deviceBean)
            Log.i(TAG, "VMSPlayerManager 初始化完成")

            playerManager?.videoViewCallback = object : VMSPlayerManager.VideoViewCallback {
                override fun onVideoViewAdd() {
                    runOnUiThread {
                        Log.d(TAG, "视频视图添加回调触发")
                        binding.previewContainer.removeAllViews()
                        playerManager?.addVideoViewSafely(binding.previewContainer)
                        updateStatus("状态：视频画面已加载")
                        Log.i(TAG, "视频视图已添加到 preview_container")
                    }
                }

                override fun onGetScaleMode(): ScaleMode {
                    return ScaleMode.AspectFit
                }

                override fun isHandleTouchEvent(): Boolean {
                    return true
                }
            }
            Log.d(TAG, "videoViewCallback 已设置")

            playerManager?.playStatusCallback = object : VMSPlayerManager.PlayStatusCallback {
                override fun onStatusChange(status: PlayerAllStatus) {
                    val statusCode = status.channelStatus
                    val reasonCode = status.channelFinishReason
                    currentPlayerStatus = statusCode
                    Log.i(TAG, "播放状态变更：Status=$statusCode, Reason=$reasonCode")
                    runOnUiThread {
                        val statusText = getStatusText(statusCode, reasonCode)
                        updateStatus("状态：$statusText")
                        if (statusCode == SMBCloudSDKConstants.PlayerStatus.fialure) {
                            val errorMsg = playerManager?.getPlayerFinishMessage(reasonCode) ?: "未知错误 ($reasonCode)"
                            Log.e(TAG, "播放失败：$errorMsg")
                            showToast("播放失败：$errorMsg")
                        }
                    }
                }

                override fun onVolumeChange(enable: Boolean) {
                    Log.d(TAG, "音量变更：enable=$enable")
                }

                override fun onGetDeviceInfo(errorCode: Int) {
                    if (SMBCloudSDKContext.isRequestSuccess(errorCode)) {
                        Log.i(TAG, "通过回调更新设备信息成功")
                    } else {
                        val errorMsg = SMBCloudSDKContext.getErrorCodeMessage(errorCode)
                        Log.w(TAG, "更新设备信息失败：$errorMsg")
                    }
                }

                override fun onPlayTimeUpdate(progressTime: Long) {}
                override fun onQualityChange(quality: Int) {
                    Log.d(TAG, "清晰度变更：quality=$quality")
                }
            }
            Log.d(TAG, "playStatusCallback 已设置")

            startCameraPreviewInternal()

        } catch (t: Throwable) {
            Log.e(TAG, "初始化播放器或启动预览失败", t)
            updateStatus("状态：播放器初始化异常")
            showToast("播放器初始化失败：${t.message}")
        }
    }

    private fun startCameraPreviewInternal(resolution: Resolution = Resolution.HD) {
        val currentPlayerManager = playerManager
        if (currentPlayerManager == null) {
            Log.e(TAG, "无法开始预览，playerManager 为 null")
            updateStatus("状态：错误 - 播放器未准备好")
            return
        }

        updateStatus("状态：正在请求播放信息...")
        Log.i(TAG, "请求播放信息，分辨率：${resolution.name}")

        try {
            currentPlayerManager.reqGetPlayInfo(
                lifecycleScope,
                SMBCloudSDKConstants.ClientType.CLIENT_TYPE_ANDROID,
                StreamType.StreamTypeVideo,
                resolution.value
            ) { errorCode, moduleSpec, streamUrl ->
                if (SMBCloudSDKContext.isRequestSuccess(errorCode)) {
                    Log.i(TAG, "获取播放信息成功！Stream URL: $streamUrl")
                    currentPlayerManager.startPreview(resolution)
                    Log.i(TAG, "已发送开始预览指令")
                } else {
                    val errorMsg = SMBCloudSDKContext.getErrorCodeMessage(errorCode)
                    Log.e(TAG, "获取播放信息失败：$errorMsg (错误码: $errorCode)")
                    updateStatus("状态：获取播放信息失败 - $errorMsg")
                    showToast("获取播放信息失败：$errorMsg")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "请求播放信息失败", e)
            updateStatus("状态：播放请求异常")
            showToast("播放请求失败：${e.message}")
        }
    }

    private fun updateStatus(message: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            try {
                binding.tvStatus.text = message
            } catch (e: Exception) {
                Log.e(TAG, "更新状态失败", e)
            }
        }
        Log.d(TAG, "状态更新：$message")
    }

    private fun showToast(message: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
    }

    private fun getStatusText(statusCode: Int, reasonCode: Int): String {
        return when (statusCode) {
            SMBCloudSDKConstants.PlayerStatus.loading -> "加载中..."
            SMBCloudSDKConstants.PlayerStatus.playing -> "播放中"
            SMBCloudSDKConstants.PlayerStatus.paused -> "已暂停"
            SMBCloudSDKConstants.PlayerStatus.stopped -> "已停止"
            SMBCloudSDKConstants.PlayerStatus.fialure -> "播放失败（原因：${getFailureReasonText(reasonCode)}）"
            SMBCloudSDKConstants.PlayerStatus.noStream -> "无信号/无流"
            SMBCloudSDKConstants.PlayerStatus.invalid -> "无效状态"
            else -> "未知状态 ($statusCode)"
        }
    }

    private fun getFailureReasonText(reasonCode: Int): String {
        val sdkMessage = playerManager?.getPlayerFinishMessage(reasonCode)
        return if (!sdkMessage.isNullOrEmpty() && sdkMessage != reasonCode.toString()) {
            sdkMessage
        } else {
            when (reasonCode) {
                BizMediaFinishReason.DeviceOffline -> "设备离线"
                BizMediaFinishReason.AuthFailed -> "认证失败"
                BizMediaFinishReason.TooManyClient -> "连接数过多"
                BizMediaFinishReason.Timeout -> "连接超时"
                BizMediaFinishReason.NoInternetConnection -> "无网络连接"
                BizMediaFinishReason.DeviceUnbind -> "设备已解绑"
                BizMediaFinishReason.ServiceUnavailable -> "服务不可用"
                else -> "错误码: $reasonCode"
            }
        }
    }

    private fun isValidMacAddress(mac: String?): Boolean {
        return mac?.matches("^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$".toRegex()) ?: false
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d(TAG, "屏幕配置变更")
        if (playerManager != null && (currentPlayerStatus == SMBCloudSDKConstants.PlayerStatus.playing || currentPlayerStatus == SMBCloudSDKConstants.PlayerStatus.paused)) {
            Log.i(TAG, "播放器正在播放/暂停 (状态: $currentPlayerStatus)，重新添加视频视图...")
            binding.previewContainer.postDelayed({
                if (playerManager != null) {
                    binding.previewContainer.removeAllViews()
                    playerManager?.addVideoViewSafely(binding.previewContainer)
                    Log.i(TAG, "视频视图重新添加")

                    //FaceOverView尺寸更新
                    faceOverlayView.post{
                        faceOverlayView.setPreviewSize(
                            binding.previewContainer.width,
                            binding.previewContainer.height
                        )
                        faceOverlayView.invalidate()
                    }
                } else {
                    Log.w(TAG, "postDelayed 时 playerManager 为 null")
                }
            }, 100)
        } else {
            Log.d(TAG, "播放器未运行或状态未知 (状态: $currentPlayerStatus)")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.w(TAG, "Video Activity 销毁 - 释放播放器资源")
        playerManager?.release()
        playerManager = null
        handler.removeCallbacksAndMessages(null)

        //释放ML Kit资源
        faceDetector.close()
        Log.d(TAG,"ML Kit FaceDetector closed")
    }

    private fun hasRequiredPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.INTERNET)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.RECORD_AUDIO)
        }
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            Log.i(TAG, "请求权限：${permissionsNeeded.joinToString()}")
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            Log.i(TAG, "所有必要权限已授予")
            handler.postDelayed({ autoConnect() }, 500)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            grantResults.forEachIndexed { index, result ->
                val permission = permissions[index]
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "权限被拒绝：$permission")
                    showToast("$permission 权限被拒绝")
                    allGranted = false
                } else {
                    Log.i(TAG, "权限已授予：$permission")
                }
            }
            if (allGranted) {
                Log.i(TAG, "所有请求的权限已授予")
                handler.postDelayed({ autoConnect() }, 500)
            }
        }
    }
}

object BizMediaFinishReason {
    const val DeviceOffline = 14
    const val AuthFailed = 1
    const val TooManyClient = 8
    const val Timeout = 0
    const val NoInternetConnection = 16
    const val DeviceUnbind = 15
    const val ServiceUnavailable = 6
}
