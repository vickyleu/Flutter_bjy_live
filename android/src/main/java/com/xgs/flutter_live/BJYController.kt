package com.xgs.flutter_live

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.Toast
import com.baijiayun.live.ui.LiveSDKWithUI
import com.baijiayun.live.ui.LiveSDKWithUI.LiveRoomUserModel
import com.baijiayun.livecore.context.LPConstants
import com.baijiayun.videoplayer.ui.playback.PBRoomUI

/**
 * Created  on 2019/10/26.
 *
 * @author grey
 */
object BJYController {
    var videoProgressListener: VideoProgressListener? = null

    // 跳转直播
    fun startLiveActivity(activity: Activity?, option: BJYLiveOption) {
        // 编辑用户信息
        val userModel = LiveRoomUserModel(option.getUserName(), option.getAvatarUrl(), option.getUserNum(), LPConstants.LPUserType.Student)
        // 进入直播房间
        LiveSDKWithUI.enterRoom(activity!!, option.getRoomId(), option.getSign(), userModel) { s -> Toast.makeText(activity, s, Toast.LENGTH_SHORT).show() }

        //退出直播间二次确认回调 无二次确认无需设置
        LiveSDKWithUI.setRoomExitListener { context, callback -> callback.exit() }

        //设置直播单点登录
        LiveSDKWithUI.setEnterRoomConflictListener { context, type, callback ->
            if (context != null) {
                // 单点登录冲突 endType为冲突方终端类型
                AlertDialog.Builder(context)
                        .setTitle("提示")
                        .setMessage("已在其他设备观看")
                        .setCancelable(true)
                        .setPositiveButton("确定") { dialog, which -> callback.exit() }
                        .create()
                        .show()
            }
        }
    }

    // 跳转到回放
    fun startBJYPlayBack(activity: Activity?, backOption: BJYBackOption) {
        PBRoomUI.enterPBRoom(activity, backOption.getRoomId(), backOption.getToken(), backOption.getSessionId()) { s -> Toast.makeText(activity, s, Toast.LENGTH_SHORT).show() }
    }

    // 跳转到点播
    fun startBJYPVideo(activity: Activity, videoOption: BJYVideoOption) {
        val intent = Intent(activity, BJYVideoPlayerActivity::class.java)
        intent.putExtras(videoOption.bundle())
        activity.startActivity(intent)
    }

    // 进度回调
    fun onPlayRateOfProgress(currentTime: Int, duration: Int) {
        if (videoProgressListener != null) {
            videoProgressListener?.onPlayRateOfProgress(currentTime, duration)
        }
    }

    interface VideoProgressListener {
        fun onPlayRateOfProgress(currentTime: Int, duration: Int)
    }
}