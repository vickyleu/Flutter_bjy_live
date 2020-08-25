package com.xgs.flutter_live.widget

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import com.baijiayun.videoplayer.ui.R as BjyResource
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.baijiayun.BJYPlayerSDK
import com.baijiayun.videoplayer.IBJYVideoPlayer
import com.baijiayun.videoplayer.event.BundlePool
import com.baijiayun.videoplayer.event.EventKey
import com.baijiayun.videoplayer.event.OnPlayerEventListener
import com.baijiayun.videoplayer.log.BJLog
import com.baijiayun.videoplayer.player.PlayerStatus
import com.baijiayun.videoplayer.ui.component.BaseComponent
import com.baijiayun.videoplayer.ui.event.UIEventKey
import com.baijiayun.videoplayer.ui.listener.OnTouchGestureListener
import com.baijiayun.videoplayer.ui.utils.NetworkUtils
import com.baijiayun.videoplayer.util.Utils
import java.lang.Runnable

/**
 * Created by yongjiaming on 2018/8/7
 */
class CustomControllerComponent(context: Context?, val bjyPlayer: IBJYVideoPlayer?) : BaseComponent(context), OnTouchGestureListener {
    private val MSG_CODE_DELAY_HIDDEN_CONTROLLER = 101
    var mTopContainer: View? = null
    var mBottomContainer: View? = null
    var mBackIcon: ImageView? = null
    var mTopTitle: TextView? = null
    var mStateIcon: ImageView? = null
    var mCurrTime: TextView? = null
    var mTotalTime: TextView? = null
    var mSwitchScreen: ImageView? = null
    var mSeekBar: SeekBar? = null

    private var mBufferPercentage = 0
    private var mSeekProgress = -1
    private val mControllerTopEnable = true
    private var title: String? = null
    private var mBottomAnimator: ObjectAnimator? = null
    private var mTopAnimator: ObjectAnimator? = null
    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_CODE_DELAY_HIDDEN_CONTROLLER -> setControllerState(false)
            }
        }
    }

    fun setTitleValue(title: String?) {
        this.title = title
    }

    override fun onCreateComponentView(context: Context): View {
        return View.inflate(context, BjyResource.layout.layout_controller_component_new, null)
    }

    override fun onPlayerEvent(eventCode: Int, bundle: Bundle) {
        when (eventCode) {
            OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE -> {
                val status = bundle.getSerializable(EventKey.SERIALIZABLE_DATA) as? PlayerStatus
                        ?:return
                when (status) {
                    PlayerStatus.STATE_PAUSED -> mStateIcon?.isSelected = true
                    PlayerStatus.STATE_STARTED -> {
                        mStateIcon?.isSelected = false
                        sendDelayHiddenMessage()
                    }
                    PlayerStatus.STATE_INITIALIZED -> {
                        mBufferPercentage = 0
                        updateUI(0, 0)
                        if (title != null && title != "") {
                            setTitle(title!!)
                        } else if (stateGetter.videoInfo != null) {
                            setTitle(stateGetter?.videoInfo?.videoTitle?:"")
                        }
                    }
                }
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE -> {
                val currentTime = bundle.getInt(EventKey.INT_DATA)
                updateUI(currentTime, stateGetter.duration)
                //通过这种方式判断视频播完完成，因为STATE_PLAYBACK_COMPLETED状态回调之后还会补发一个timer update
                if (stateGetter.playerStatus == PlayerStatus.STATE_PLAYBACK_COMPLETED && currentTime == stateGetter.duration) {
                    mStateIcon?.isSelected = true
                    mSeekBar?.progress = 0
                    mSeekBar?.secondaryProgress = 0
                    setCurrTime(0, stateGetter.duration)
                }
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_UPDATE -> {
                BJLog.d("bjy", "buffering update " + bundle.getInt(EventKey.INT_DATA))
                setSecondProgress(bundle.getInt(EventKey.INT_DATA))
            }
            else -> {
            }
        }
    }

    override fun onCustomEvent(eventCode: Int, bundle: Bundle) {
        when (eventCode) {
            UIEventKey.CUSTOM_CODE_REQUEST_TOGGLE_SCREEN -> {
                setSwitchScreenIcon(bundle.getBoolean(EventKey.BOOL_DATA))
                sendDelayHiddenMessage()
            }
            UIEventKey.CUSTOM_CODE_NETWORK_CHANGE_TO_MOBILE -> mStateIcon?.isSelected = false
            UIEventKey.CUSTOM_CODE_TAP_PPT -> toggleController()
            UIEventKey.CUSTOM_CODE_CONTROLLER_STATUS_CHANGE -> setControllerState(false)
            else -> {
            }
        }
    }

    override fun onComponentEvent(eventCode: Int, bundle: Bundle) {
        when (eventCode) {
            UIEventKey.CUSTOM_CODE_REQUEST_SEEK -> {
                val seekToPos = bundle.getInt(EventKey.INT_DATA)
                updateUI(seekToPos, stateGetter.duration)
            }
        }
    }

    override fun onInitView() {
        mTopContainer = findViewById( BjyResource.id.cover_player_controller_top_container)
        mBottomContainer = findViewById(BjyResource.id.cover_player_controller_bottom_container)
        mBackIcon = findViewById(BjyResource.id.cover_player_controller_image_view_back_icon)
        mTopTitle = findViewById(BjyResource.id.cover_player_controller_text_view_video_title)
        mStateIcon = findViewById(BjyResource.id.cover_player_controller_image_view_play_state)
        mCurrTime = findViewById(BjyResource.id.cover_player_controller_text_view_curr_time)
        mTotalTime = findViewById(BjyResource.id.cover_player_controller_text_view_total_time)
        mSwitchScreen = findViewById(BjyResource.id.cover_player_controller_image_view_switch_screen)
        mSeekBar = findViewById(BjyResource.id.cover_player_controller_seek_bar)
        mBackIcon?.setOnClickListener(View.OnClickListener { notifyComponentEvent(UIEventKey.CUSTOM_CODE_REQUEST_BACK, null) })
        mStateIcon?.setOnClickListener(View.OnClickListener {
            val selected = mStateIcon?.isSelected ?:false
            if (selected) {
                requestPlay(null)
            } else {
                requestPause(null)
            }
            mStateIcon?.setSelected(!selected)
        })
        mSwitchScreen?.setOnClickListener(View.OnClickListener { notifyComponentEvent(UIEventKey.CUSTOM_CODE_REQUEST_TOGGLE_SCREEN, null) })
        mSeekBar?.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    updateUI(progress, seekBar.max)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                mSeekProgress = seekBar.progress
                mHandler.removeCallbacks(mSeekEventRunnable)
                mHandler.postDelayed(mSeekEventRunnable, 300)
            }
        })
    }

    override fun setKey() {
        super.key = UIEventKey.KEY_CONTROLLER_COMPONENT
    }

    private val mSeekEventRunnable = Runnable {
        if (mSeekProgress < 0) return@Runnable
        val bundle = BundlePool.obtain()
        bundle.putInt(EventKey.INT_DATA, mSeekProgress)
        requestSeek(bundle)
    }

    private fun setControllerState(state: Boolean) {
        if (state) {
            sendDelayHiddenMessage()
        } else {
            removeDelayHiddenMessage()
        }
        setTopContainerState(state)
        setBottomContainerState(state)
    }

    private val isControllerShow: Boolean
        get() = mBottomContainer?.visibility == View.VISIBLE

    private fun toggleController() {
        if (bjyPlayer?.isPlayLocalVideo != true && !NetworkUtils.isNetConnected(context)) {
            return
        }
        if (isControllerShow) {
            setControllerState(false)
        } else {
            setControllerState(true)
        }
    }

    private fun sendDelayHiddenMessage() {
        //调试模式不隐藏进度条
        if (BJYPlayerSDK.IS_DEVELOP_MODE) {
            return
        }
        removeDelayHiddenMessage()
        mHandler.sendEmptyMessageDelayed(MSG_CODE_DELAY_HIDDEN_CONTROLLER, 5000)
    }

    private fun removeDelayHiddenMessage() {
        mHandler.removeMessages(MSG_CODE_DELAY_HIDDEN_CONTROLLER)
    }

    private fun setCurrTime(curr: Int, duration: Int) {
        mCurrTime?.text = Utils.formatDuration(curr, duration >= 3600)
    }

    private fun setTotalTime(duration: Int) {
        mTotalTime?.text = Utils.formatDuration(duration)
    }

    private fun setSeekProgress(curr: Int, duration: Int) {
        mSeekBar?.max = duration
        mSeekBar?.progress = curr
        val secondProgress = mBufferPercentage * 1.0f / 100 * duration
        mSeekBar?.secondaryProgress = secondProgress.toInt()
    }

    private fun setSecondProgress(bufferPercent: Int) {
        mBufferPercentage = bufferPercent
        val secondProgress = mBufferPercentage * 1.0f / 100 * ((mSeekBar?.max)?:0)
        mSeekBar?.secondaryProgress = secondProgress.toInt()
    }

    private fun setTitle(text: String) {
        mTopTitle?.text = text
    }

    private fun setSwitchScreenIcon(isFullScreen: Boolean) {
        mSwitchScreen?.setImageResource(if (isFullScreen) BjyResource.mipmap.icon_exit_full_screen else BjyResource.mipmap.icon_full_screen)
    }

    private fun setScreenSwitchEnable(screenSwitchEnable: Boolean) {
        mSwitchScreen?.visibility = if (screenSwitchEnable) View.VISIBLE else View.GONE
    }

    private fun updateUI(curr: Int, duration: Int) {
        setSeekProgress(curr, duration)
        setCurrTime(curr, duration)
        setTotalTime(duration)
    }

    private fun setTopContainerState(state: Boolean) {
        if (mControllerTopEnable) {
            mTopContainer?.clearAnimation()
            cancelTopAnimation()
            ObjectAnimator.ofFloat()
            mTopAnimator = ObjectAnimator.ofFloat(mTopContainer,
                    "alpha", if (state) 0.0f else 1.0f, if (state) 1.0f else 0.0f)
                    .setDuration(300)
            mTopAnimator?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    if (state) {
                        mTopContainer?.visibility = View.VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (!state) {
                        mTopContainer?.visibility = View.GONE
                    }
                }
            })
            mTopAnimator?.start()
        } else {
            mTopContainer?.visibility = View.GONE
        }
    }

    private fun cancelBottomAnimation() {
        if (mBottomAnimator != null) {
            mBottomAnimator?.cancel()
            mBottomAnimator?.removeAllListeners()
            mBottomAnimator?.removeAllUpdateListeners()
        }
    }

    private fun cancelTopAnimation() {
        if (mTopAnimator != null) {
            mTopAnimator?.cancel()
            mTopAnimator?.removeAllListeners()
            mTopAnimator?.removeAllUpdateListeners()
        }
    }

    private fun setBottomContainerState(state: Boolean) {
        mBottomContainer?.clearAnimation()
        cancelBottomAnimation()
        mBottomAnimator = ObjectAnimator.ofFloat(mBottomContainer,
                "alpha", if (state) 0f else 1f, if (state) 1f else 0f).setDuration(300)
        mBottomAnimator?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (state) {
                    mBottomContainer?.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (!state) {
                    mBottomContainer?.visibility = View.GONE
                }
            }
        })
        mBottomAnimator?.start()
    }

    override fun onSingleTapUp(event: MotionEvent) {
        toggleController()
    }

    override fun onDoubleTap(event: MotionEvent) {}
    override fun onDown(event: MotionEvent) {}
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float) {}
    override fun onEndGesture() {}
}