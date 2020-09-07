//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.xgs.flutter_live.widget;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.baijiayun.videoplayer.ui.component.BaseComponent;
import com.baijiayun.videoplayer.ui.component.ComponentManager;
import com.baijiayun.videoplayer.ui.event.EventDispatcher;
import com.baijiayun.videoplayer.ui.listener.IComponent;
import com.baijiayun.videoplayer.ui.listener.IComponentEventListener;
import com.baijiayun.videoplayer.ui.listener.IFilter;
import com.baijiayun.videoplayer.ui.listener.PlayerStateGetter;
import com.baijiayun.videoplayer.ui.widget.ComponentContainer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FLTTComponentContainer extends ComponentContainer {

    public FLTTComponentContainer(@NonNull Context context) {
        super(context);
    }

    public FLTTComponentContainer(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FLTTComponentContainer(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(PlayerStateGetter stateGetter,ComponentManager manager) {
        super.init(stateGetter);
        try {
            Field field = this.getClass().getDeclaredField("componentManager");
            field.setAccessible(true);
            field.set(this,manager);
            Field field2 = this.getClass().getDeclaredField("eventDispatcher");
            field2.setAccessible(true);
            field2.set(this, new EventDispatcher(manager));
            manager.forEach((component) -> {
                try {
                    Method method = this.getClass().getDeclaredMethod("addComponent",IComponent.class);
                    method.setAccessible(true);
                    method.invoke(this, component);
                }catch (Exception ignored){}
            });
        }catch (Exception ignored){}

    }

    //    public void init(PlayerStateGetter stateGetter) {
//        this.stateGetter = stateGetter;
//        this.componentManager = new ComponentManager(this.getContext());
//        this.eventDispatcher = new EventDispatcher(this.componentManager);
//        this.componentManager.forEach((component) -> {
//            this.addComponent(component);
//        });
//    }
}
