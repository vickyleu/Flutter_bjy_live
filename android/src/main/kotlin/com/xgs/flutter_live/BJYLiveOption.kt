package com.xgs.flutter_live

import io.flutter.plugin.common.MethodCall
import javax.annotation.Nonnull

/**
 * Created  on 2019/10/26.
 *
 * @author grey
 */
class BJYLiveOption {
    private var userName: String? = null
    private var avatarUrl: String? = null
    private var userNum: String? = null
    private var sign: String? = null
    private var roomId: String? = null
    fun create(@Nonnull call: MethodCall): BJYLiveOption {
        userName = call.argument("userName")
        userNum = call.argument("userNum")
        avatarUrl = call.argument("userAvatar")
        sign = call.argument("sign")
        roomId = call.argument("roomId")
        return this
    }

    fun getUserName(): String {
        return if (userName == null) "" else userName!!
    }

    fun getAvatarUrl(): String {
        return if (avatarUrl == null) "" else avatarUrl!!
    }

    fun getUserNum(): String {
        return if (userNum == null) "" else userNum!!
    }

    fun getSign(): String {
        return if (sign == null) "" else sign!!
    }

    fun getRoomId(): Long {
        return try {
            roomId?.toLong()?:0L
        } catch (e: Exception) {
            0L
        }
    }
}