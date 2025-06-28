// FileName: MyApplication.kt (注意文件扩展名是 .kt)
// Path: app/src/main/java/com/example/myapplication/MyApplication.kt (根据你的包名调整路径)
package com.example.myapplication // 你的包名

import android.app.Application
import android.util.Log
import com.tplink.smbdeveplatsdk.SMBCloudSDKContext // 确保导入正确

class MyApplication : Application() { // 继承 Application

    //伴生对象，类似于 Java 中的静态成员
    companion object {
        private const val TAG = "MyApplication"

        // 从你的 VideoPlaybackFragment.Companion object 中获取这些常量
        // 或者你可以将这些常量也定义在这里，或者从更安全的地方读取（例如 BuildConfig 或加密存储）
        private const val YOUR_APP_KEY = "34b45db265eb4ecba48494cda4497369"
        private const val YOUR_APP_SECRET = "47646a74e43b4f3eb498a92bbdd47e1e"

        // 可选：用于 Activity 检查配置状态的标志位
        var sdkBaseConfigSuccess: Boolean = false
            private set // 只允许在 MyApplication 内部修改
    }

    override fun onCreate() {
        super.onCreate()

        try {
            // 1. 初始化 SMBCloudSDK
            SMBCloudSDKContext.init(this) // 'this' 在这里是 Application context
            Log.i(TAG, "SMBCloudSDK - init() success in Application.")

            // 2. 设置 AK/SK
            SMBCloudSDKContext.setAkSk(YOUR_APP_KEY, YOUR_APP_SECRET)
            Log.i(TAG, "SMBCloudSDK - setAkSk() success in Application. AK/SK is now configured globally.")

            sdkBaseConfigSuccess = true // 标记配置成功

        } catch (e: Exception) {
            // 捕获所有可能的初始化和设置异常
            Log.e(TAG, "SMBCloudSDK - Critical error during init() or setAkSk() in Application!", e)
            sdkBaseConfigSuccess = false // 标记配置失败
            // 这种级别的错误通常意味着依赖此SDK的核心功能可能无法工作。
            // 你可以在这里设置一个全局错误标志，Activity 启动时检查此标志。
            // throw RuntimeException("Failed to initialize critical SDK components", e) // 或者让应用崩溃，以便快速发现问题
        }
    }
}