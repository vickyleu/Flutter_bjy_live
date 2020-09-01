package com.xgs.flutter_live

import com.xgs.flutter_live.BJYController.VideoProgressListener
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.util.*
import javax.annotation.Nonnull

/**
 * Created  on 2019/10/11.
 *
 * @author grey
 */
class FlutterLivePlugin private constructor(registrar: Registrar) : MethodCallHandler, VideoProgressListener {
    private val methodChannel: MethodChannel
    private var result: MethodChannel.Result? = null
    private val registrar: Registrar
    override fun onMethodCall(@Nonnull call: MethodCall, @Nonnull result: MethodChannel.Result) {
        if (registrar.activity() == null) {
            result.error("no_activity", "Flutter_Live plugin requires a foreground activity.", null)
            return
        }
        this.result = result
        if (call.method == "startLive") {
            BJYController.startLiveActivity(registrar.activity(), BJYLiveOption().create(call))
            return
        }
        if (call.method == "startBack") {
            BJYController.startBJYPlayBack(registrar.activity(), BJYBackOption().create(call))
            return
        }
        if (call.method == "startVideo") {
            BJYController.startBJYPVideo(registrar.activity(), BJYVideoOption().create(call))
        }
        if (call.method == "addingDownloadQueue") {
            BJYController.startBJYPVideo(registrar.activity(), BJYVideoOption().create(call))
        }
    }

    override fun onPlayRateOfProgress(currentTime: Int, duration: Int) {
        val resultMap: MutableMap<String, Any> = HashMap()
        resultMap["progress"] = currentTime
        resultMap["totalProgress"] = duration
        result?.success(resultMap)
    }

    companion object {
        /**
         * Plugin registration.
         */
        fun registerWith(registrar: Registrar) {
            FlutterLivePlugin(registrar)
        }
    }

    init {
        // 设置监听
        BJYController.videoProgressListener = this
        this.registrar = registrar
        methodChannel = MethodChannel(registrar.messenger(), "flutter_live")
        methodChannel.setMethodCallHandler(this)
    }
}