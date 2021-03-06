package com.xgs.flutter_live;

import com.google.gson.Gson;

import javax.annotation.Nonnull;

import io.flutter.plugin.common.MethodCall;

/**
 * Created  on 2019/10/26.
 *
 * @author grey
 */
class BJYLiveOption {

    private String userName;
    private String avatarUrl;
    private String userNum;
    private String sign;
    private String roomId;
    ///大班课
    private boolean interactive;

    BJYLiveOption create(@Nonnull MethodCall call) {
        this.userName = call.argument("userName");
        this.userNum = call.argument("userNum");
        this.avatarUrl = call.argument("userAvatar");
        this.sign = call.argument("sign");
        this.roomId = call.argument("roomId");
        this.interactive = call.argument("interactive");
        return this;
    }

    String getUserName() {
        return userName == null ? "" : userName;
    }

    String getAvatarUrl() {
        return avatarUrl == null ? "" : avatarUrl;
    }

    String getUserNum() {
        return userNum == null ? "" : userNum;
    }

    String getSign() {
        return sign == null ? "" : sign;
    }

    public boolean isInteractive() {
        return interactive;
    }

    Long getRoomId() {
        try {
            return Long.parseLong(roomId);
        } catch (Exception e) {
            return 0L;
        }
    }


}
