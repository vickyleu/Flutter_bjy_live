package com.xgs.flutter_live

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import com.baijiayun.download.DownloadModel
import com.baijiayun.videoplayer.VideoPlayerFactory
import com.baijiayun.videoplayer.event.BundlePool
import com.baijiayun.videoplayer.ui.activity.BaseActivity
import com.baijiayun.videoplayer.ui.event.UIEventKey
import com.baijiayun.videoplayer.ui.listener.IComponentEventListener
import com.baijiayun.videoplayer.util.Utils
import com.xgs.flutter_live.video.CustomBJYVideoView
import com.xgs.flutter_live.video.IPlayProgressListener

/**
 * Created  on 2019/10/24.
 *
 * @author grey
 */
class BJYVideoPlayerActivity : BaseActivity() {
    var bjyVideoView: CustomBJYVideoView? = null
    var isOffline = false
    var videoId = 0L
    var token = ""
    var userName = ""
    var userId = ""
    var title = ""
    var downloadVideo: DownloadModel? = null
    var mprogress = 0
    var totalProgress = 0
    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        BJYController.onPlayRateOfProgress(mprogress, totalProgress)
        bjyVideoView?.onDestroy()
    }

    override fun requestLayout(isLandscape: Boolean) {
        super.requestLayout(isLandscape)
        val layoutParams = bjyVideoView?.getLayoutParams() as LinearLayout.LayoutParams
        if (isLandscape) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        } else {
            layoutParams.width = Utils.getScreenWidthPixels(this)
            layoutParams.height = layoutParams.width * 9 / 16
        }
        bjyVideoView?.setLayoutParams(layoutParams)
        bjyVideoView?.sendCustomEvent(UIEventKey.CUSTOM_CODE_REQUEST_TOGGLE_SCREEN, BundlePool.obtain(isLandscape))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.a_bjv_video_player)
        findView()
        bindingView()
        initView()
    }

    fun findView() {
        bjyVideoView = findViewById(R.id.video_player)
    }

    fun bindingView() {
        bjyVideoView?.setComponentEventListener(IComponentEventListener { eventCode, bundle ->
            when (eventCode) {
                UIEventKey.CUSTOM_CODE_REQUEST_BACK -> if (isLandscape) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    finish()
                }
                UIEventKey.CUSTOM_CODE_REQUEST_TOGGLE_SCREEN -> requestedOrientation = if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                else -> {
                }
            }
        })
    }

    fun initView() {
        val bundle = intent.extras
        if (bundle != null) {
            isOffline = bundle.getBoolean("isOffline", false)
            videoId = bundle.getLong("videoId", 0L)
            token = bundle.getString("token", "")
            userName = bundle.getString("userName", "")
            userId = bundle.getString("userId", "")
            title = bundle.getString("title", "")
            if (isOffline) {
                downloadVideo = bundle.getParcelable("videoDownloadData")
            }
        }
        bjyVideoView?.initOtherInfo(title)
        bjyVideoView?.initPlayer(VideoPlayerFactory.Builder() //后台暂停播放
                .setSupportBackgroundAudio(true) //开启循环播放
                .setSupportLooping(false) //开启记忆播放
                .setSupportBreakPointPlay(true, this)
//                .setUserInfo(userName, userId)
                //绑定activity生命周期
                .setLifecycle(lifecycle)
                .build())
        bjyVideoView?.setPlayProgressListener(object : IPlayProgressListener{
            override fun onProgressCallBack(progress: Int, totalProgress: Int) {
                if (mprogress <= progress) {
                    mprogress = progress
                }
                if (this@BJYVideoPlayerActivity.totalProgress <= totalProgress) {
                    this@BJYVideoPlayerActivity.totalProgress = totalProgress
                }
            }
        })
        if (isOffline) {
            bjyVideoView?.setupLocalVideoWithDownloadModel(intent.getSerializableExtra("videoDownloadModel") as DownloadModel)
        } else {
            bjyVideoView?.setupOnlineVideoWithId(videoId, token, true)
        }
        if (isLandscape) {
            requestLayout(true)
        }
    }
}