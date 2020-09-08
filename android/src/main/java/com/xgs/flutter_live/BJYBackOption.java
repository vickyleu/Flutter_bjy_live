package com.xgs.flutter_live;

import javax.annotation.Nonnull;

import io.flutter.plugin.common.MethodCall;

/**
 * Created  on 2019/10/28.
 *
 * @author grey
 */
class BJYBackOption {

    private String roomId;
    private String token;
    private String sessionId;
    private String userName;
    private String userNum;

    BJYBackOption create(@Nonnull MethodCall call) {
        this.roomId = call.argument("roomId");
        this.token = call.argument("token");
        this.sessionId = call.argument("sessionId");
        this.userName = call.argument("userName");
        this.userNum = call.argument("userNum");
        return this;
    }

    String getRoomId() {
        return roomId == null ? "0" : roomId;
    }

    String getToken() {
        return token == null ? "" : token;
    }

    String getSessionId() {
        return sessionId == null ? "0" : sessionId;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserNum() {
        return userNum;
    }
}
