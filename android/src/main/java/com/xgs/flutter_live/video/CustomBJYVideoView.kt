package com.xgs.flutter_live.video

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.baijiayun.BJYPlayerSDK
import com.baijiayun.constant.VideoDefinition
import com.baijiayun.download.DownloadModel
import com.baijiayun.glide.Glide
import com.baijiayun.videoplayer.IBJYVideoPlayer
import com.baijiayun.videoplayer.event.BundlePool
import com.baijiayun.videoplayer.event.EventKey
import com.baijiayun.videoplayer.event.OnPlayerEventListener
import com.baijiayun.videoplayer.listeners.OnBufferingListener
import com.baijiayun.videoplayer.log.BJLog
import com.baijiayun.videoplayer.player.PlayerStatus
import com.baijiayun.videoplayer.render.AspectRatio
import com.baijiayun.videoplayer.render.IRender
import com.baijiayun.videoplayer.ui.R
import com.baijiayun.videoplayer.ui.event.UIEventKey
import com.baijiayun.videoplayer.ui.utils.NetworkUtils
import com.baijiayun.videoplayer.ui.widget.BaseVideoView
import com.baijiayun.videoplayer.ui.widget.ComponentContainer
import com.baijiayun.videoplayer.widget.BJYPlayerView
import com.baijiayun.videoplayer.ui.R as BjyResource
/**
 *
 */
class CustomBJYVideoView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : BaseVideoView(context, attrs, defStyleAttr) {
    private var bjyPlayerView: BJYPlayerView? = null
    private var videoId: Long = 0
    private var token: String? = null
    private var title: String? = null
    private var encrypted = false
    private var audioCoverIv: ImageView? = null
    private var mAspectRatio = AspectRatio.AspectRatio_16_9.ordinal
    private var mRenderType = IRender.RENDER_TYPE_SURFACE_VIEW
    private var isPlayOnlineVideo = false
    private var iPlayProgressListener: IPlayProgressListener? = null
    override fun init(context: Context, attrs: AttributeSet, defStyleAttr: Int) {
        bjyPlayerView = BJYPlayerView(context, attrs)
        addView(bjyPlayerView)
        audioCoverIv = ImageView(context)
        audioCoverIv?.scaleType = ImageView.ScaleType.FIT_XY
        audioCoverIv?.visibility = GONE
        audioCoverIv?.layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(audioCoverIv)
    }
    /**
     * @param videoPlayer
     * @param shouldRenderCustomComponent 是否渲染播放器组件。回放只显示视频，不显示其它组件
     */
    /**
     * 初始化播放器
     */
    @JvmOverloads
    fun initPlayer(videoPlayer: IBJYVideoPlayer, shouldRenderCustomComponent: Boolean = true) {
        bjyVideoPlayer = videoPlayer
        bjyVideoPlayer.bindPlayerView(bjyPlayerView)

        //初始化videoplayer之后才能设置宽高比
        bjyPlayerView?.setRenderType(mRenderType)
        bjyPlayerView?.setAspectRatio(AspectRatio.values()[mAspectRatio])
        if (shouldRenderCustomComponent) {
            initComponentContainer()
            bjyVideoPlayer.addOnPlayerErrorListener { error ->
                val bundle = BundlePool.obtain()
                bundle.putString(EventKey.STRING_DATA, error.message)
                componentContainer.dispatchErrorEvent(error.code, bundle)
            }
            bjyVideoPlayer.addOnPlayingTimeChangeListener { currentTime, duration ->
                if (iPlayProgressListener != null) {
                    iPlayProgressListener?.onProgressCallBack(currentTime, duration)
                }
                //只通知到controller component
                val bundle = BundlePool.obtainPrivate(UIEventKey.KEY_CONTROLLER_COMPONENT, currentTime)
                componentContainer.dispatchPlayEvent(OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE, bundle)
            }
            bjyVideoPlayer.addOnBufferUpdateListener { bufferedPercentage -> //只通知到controller component
                val bundle = BundlePool.obtainPrivate(UIEventKey.KEY_CONTROLLER_COMPONENT, bufferedPercentage)
                componentContainer.dispatchPlayEvent(OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_UPDATE, bundle)
            }
            bjyVideoPlayer.addOnBufferingListener(object : OnBufferingListener {
                override fun onBufferingStart() {
                    BJLog.d("bjy", "onBufferingStart invoke")
                    componentContainer.dispatchPlayEvent(UIEventKey.PLAYER_CODE_BUFFERING_START, null)
                }

                override fun onBufferingEnd() {
                    BJLog.d("bjy", "onBufferingEnd invoke")
                    componentContainer.dispatchPlayEvent(UIEventKey.PLAYER_CODE_BUFFERING_END, null)
                }
            })
        } else {
            //回放模式下不监听网络
            useDefaultNetworkListener = false
        }
        bjyVideoPlayer.addOnPlayerStatusChangeListener { status ->
            if (status == PlayerStatus.STATE_PREPARED) {
                updateAudioCoverStatus(bjyVideoPlayer.videoInfo != null && bjyVideoPlayer.videoInfo?.definition == VideoDefinition.Audio)
            }
            if (componentContainer != null) {
                val bundle = BundlePool.obtain(status)
                componentContainer.dispatchPlayEvent(OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE, bundle)
            }
        }
    }

    fun initOtherInfo(title: String?) {
        this.title = title
    }

    private fun initComponentContainer() {
        componentContainer = ComponentContainer(context)
        componentContainer.init(this, CustomComponentManager(context, title, bjyVideoPlayer!!))
        componentContainer.setOnComponentEventListener(internalComponentEventListener)
        addView(componentContainer, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT))
    }

    override fun requestPlayAction() {
        super.requestPlayAction()
        //视频未初始化成功则请求视频地址
        if (isPlayOnlineVideo && (videoInfo == null || videoInfo?.videoId == 0L)) {
            setupOnlineVideoWithId(videoId, token, encrypted)
            sendCustomEvent(UIEventKey.CUSTOM_CODE_REQUEST_VIDEO_INFO, null)
        } else {
            play()
        }
    }
    /**
     * 设置播放百家云在线视频
     *
     * @param videoId   视频id
     * @param token     需要集成方后端调用百家云后端的API获取
     * @param encrypted 是否加密
     */
    /**
     * 设置播放百家云在线视频
     *
     * @param videoId 视频id
     * @param token   需要集成方后端调用百家云后端的API获取
     */
    @JvmOverloads
    fun setupOnlineVideoWithId(videoId: Long, token: String?, encrypted: Boolean = true) {
        this.videoId = videoId
        this.token = token
        this.encrypted = encrypted
        if (useDefaultNetworkListener) {
            registerNetChangeReceiver()
        }
        if (!enablePlayWithMobileNetwork && NetworkUtils.isMobile(NetworkUtils.getNetworkState(context))) {
            sendCustomEvent(UIEventKey.CUSTOM_CODE_NETWORK_CHANGE_TO_MOBILE, null)
        } else {
            bjyVideoPlayer.setupOnlineVideoWithId(videoId, token)
        }
        isPlayOnlineVideo = true
    }

    /**
     * 设置播放本地文件路径(不支持记忆播放)
     *
     * @param path 视频文件绝对路径
     */
    fun setupLocalVideoWithFilePath(path: String?) {
        bjyVideoPlayer.setupLocalVideoWithFilePath(path)
        isPlayOnlineVideo = false
    }

    fun setupLocalVideoWithDownloadModel(downloadModel: DownloadModel?) {
        bjyVideoPlayer.setupLocalVideoWithDownloadModel(downloadModel)
        isPlayOnlineVideo = false
    }

    /**
     * 更新纯音频占位图状态
     */
    private fun updateAudioCoverStatus(isAudio: Boolean) {
        if (isAudio) {
            audioCoverIv?.let {
                it.visibility = View.VISIBLE
                Glide.with(this)
                        .load(BJYPlayerSDK.AUDIO_ON_PICTURE)
                        .into(it)
            }
        } else {
            audioCoverIv?.visibility = View.GONE
        }
    }


    fun setPlayProgressListener(listener: IPlayProgressListener?) {
        iPlayProgressListener = listener
    }

    companion object {
        private const val TAG = "CustomBJYVideoView"
    }

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.BJVideoView, 0, 0)
        if (a.hasValue(R.styleable.BJVideoView_aspect_ratio)) {
            mAspectRatio = a.getInt(R.styleable.BJVideoView_aspect_ratio, AspectRatio.AspectRatio_16_9.ordinal)
        }
        if (a.hasValue(R.styleable.BJVideoView_render_type)) {
            mRenderType = a.getInt(R.styleable.BJVideoView_render_type, IRender.RENDER_TYPE_SURFACE_VIEW)
        }
        a.recycle()
    }
}