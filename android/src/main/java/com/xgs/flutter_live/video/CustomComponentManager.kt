package com.xgs.flutter_live.video

import android.content.Context
import com.baijiayun.BJYPlayerSDK
import com.baijiayun.videoplayer.IBJYVideoPlayer
import com.baijiayun.videoplayer.ui.component.*
import com.baijiayun.videoplayer.ui.event.UIEventKey
import com.xgs.flutter_live.widget.CustomControllerComponent

/**
 * Created  on 2019/10/24.
 *
 * @author grey
 */
class CustomComponentManager(context: Context?) : ComponentManager(context) {
    private var context: Context? = null
    private var bjyPlayer: IBJYVideoPlayer? = null
    private var title: String? = null

    constructor(context: Context?, title: String?,player: IBJYVideoPlayer) : this(context) {
        this.context = context
        this.title = title
        this.bjyPlayer = player
        generateCustomComponentList()
    }

    private fun generateCustomComponentList() {
        release()
        addComponent(UIEventKey.KEY_LOADING_COMPONENT, LoadingComponent(context))
        addComponent(UIEventKey.KEY_GESTURE_COMPONENT, GestureComponent(context))
        //controller 需在gesture布局上方，否则会有事件冲突
        val controllerComponent = CustomControllerComponent(context, bjyPlayer)
        controllerComponent.setTitleValue(title)
        addComponent(UIEventKey.KEY_CONTROLLER_COMPONENT, controllerComponent)
        addComponent(UIEventKey.KEY_ERROR_COMPONENT, ErrorComponent(context))
        addComponent(UIEventKey.KEY_MENU_COMPONENT, MenuComponent(context))
        if (BJYPlayerSDK.IS_DEVELOP_MODE) {
            addComponent(UIEventKey.KEY_VIDEO_INFO_COMPONENT, MediaPlayerDebugInfoComponent(context))
        }
    }
}