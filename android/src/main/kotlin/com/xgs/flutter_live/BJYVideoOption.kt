package com.xgs.flutter_live

import android.os.Bundle
import io.flutter.plugin.common.MethodCall
import javax.annotation.Nonnull

/**
 * Created  on 2019/10/28.
 *
 * @author grey
 */
class BJYVideoOption {
    private var videoId: String? = null
    private var token: String? = null
    private var userName: String? = null
    private var userId: String? = null
    var title: String? = null
        private set

    fun create(@Nonnull call: MethodCall): BJYVideoOption {
        videoId = call.argument("videoId")
        token = call.argument("token")
        userName = call.argument("userName")
        userId = call.argument("userId")
        title = call.argument("title")
        return this
    }

    fun getVideoId(): Long {
        return try {
            videoId?.toLong()?:0L
        } catch (e: Exception) {
            0L
        }
    }

    fun getToken(): String {
        return if (token == null) "" else token!!
    }

    fun getUserName(): String {
        return if (userName == null) "匿名用户" else userName!!
    }

    fun getUserId(): String {
        return if (userId == null) "" else userId!!
    }

    fun bundle(): Bundle {
        val bundle = Bundle()
        bundle.putBoolean("isOffline", false)
        bundle.putLong("videoId", getVideoId())
        bundle.putString("token", getToken())
        bundle.putString("userName", getUserName())
        bundle.putString("userId", getUserId())
        bundle.putString("title", title)
        return bundle
    }
}