package com.xgs.flutter_live

import io.flutter.plugin.common.MethodCall
import javax.annotation.Nonnull

/**
 * Created  on 2019/10/28.
 *
 * @author grey
 */
class BJYBackOption {
    private var roomId: String? = null
    private var token: String? = null
    private var sessionId: String? = null
    fun create(@Nonnull call: MethodCall): BJYBackOption {
        roomId = call.argument("roomId")
        token = call.argument("token")
        sessionId = call.argument("sessionId")
        return this
    }

    fun getRoomId(): String {
        return if (roomId == null) "0" else roomId!!
    }

    fun getToken(): String {
        return if (token == null) "" else token!!
    }

    fun getSessionId(): String {
        return if (sessionId == null) "0" else sessionId!!
    }
}