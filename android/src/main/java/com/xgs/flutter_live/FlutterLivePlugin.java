package com.xgs.flutter_live;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import com.baijiayun.BJYPlayerSDK;
import com.baijiayun.download.DownloadManager;
import com.baijiayun.download.DownloadTask;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Created  on 2019/10/11.
 *
 * @author grey
 */
public class FlutterLivePlugin implements FlutterPlugin, ActivityAware,MethodCallHandler, VideoProgressListener {

    private static final String CHANNEL_NAME = "flutter_live";
    private  MethodChannel methodChannel;
    private MethodChannel.Result result;
    private WeakReference<Activity> currentActivity;
    private DownloadManager downloadManager;


    /** Plugin registration. */
    public static void registerWith(Registrar registrar) {
        FlutterLivePlugin flutterLivePlugin = new FlutterLivePlugin();
        flutterLivePlugin.setupChannel(registrar.messenger());

    }


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
        setupChannel(binding.getBinaryMessenger());
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        teardownChannel();
    }

    private void setupChannel(BinaryMessenger messenger) {
        methodChannel = new MethodChannel(messenger, CHANNEL_NAME);
        methodChannel.setMethodCallHandler(this);
        // 设置监听
        BJYController.videoProgressListener = this;

    }

    private void teardownChannel() {
        methodChannel.setMethodCallHandler(null);
        methodChannel = null;
    }


    @Override
    public void onMethodCall(@Nonnull MethodCall call, @Nonnull MethodChannel.Result result) {
        if (currentActivity==null||currentActivity.get() == null) {
            result.error("no_activity", "Flutter_Live plugin requires a foreground activity.", null);
            return;
        }
        this.result = result;
        if ("register".equals(call.method)) {
            //配置sdk
            new BJYPlayerSDK.Builder(currentActivity.get().getApplication())
                    .setDevelopMode(BuildConfig.DEBUG)
                    .build();
            return;
        }else  if ("startLive".equals(call.method)) {
            BJYController.startLiveActivity(currentActivity.get(), new BJYLiveOption().create(call));
            return;
        }else if ("startBack".equals(call.method)) {
            BJYController.startBJYPlayBack(currentActivity.get(), new BJYBackOption().create(call));
            return;
        }else if ("startVideo".equals(call.method)) {
            BJYController.startBJYPVideo(currentActivity.get(), new BJYVideoOption().create(call));
        }else if ("addingDownloadQueue".equals(call.method)) {
            String classID = call.argument("classID");
            String userId = call.argument("userId");
            String token = call.argument("token");
            downloadManagerCheck(currentActivity.get(),userId);
            BJYController.addingDownloadQueue(currentActivity.get(),methodChannel,result,downloadManager,classID,token,userId);
        }else if ("pauseDownloadQueue".equals(call.method)) {
            String identifier = call.argument("identifier");
            String userId = call.argument("userId");
            boolean pause = (boolean) call.argument("pause");
            downloadManagerCheck(currentActivity.get(),userId);
            BJYController.pauseDownloadQueue(result,downloadManager,identifier,pause);
        }else if ("pauseAllDownloadQueue".equals(call.method)) {
            String userId = call.argument("userId");
            boolean pause = (boolean) call.argument("pause");
            downloadManagerCheck(currentActivity.get(),userId);
            BJYController.pauseAllDownloadQueue(result,downloadManager,pause,userId);
        }else if ("queryDownloadQueue".equals(call.method)) {
            String userId = call.argument("userId");
            downloadManagerCheck(currentActivity.get(),userId);
            BJYController.queryDownloadQueue(result,downloadManager,userId);
        }else if ("removeDownloadQueue".equals(call.method)) {
            String identifier = call.argument("identifier");
            String userId = call.argument("userId");
            downloadManagerCheck(currentActivity.get(),userId);
            BJYController.removeDownloadQueue(result,downloadManager,identifier);
        }
    }

     private  void  downloadManagerCheck(Context context,String userId) {
        String identifier = "download_Identifier_"+userId;
        if (downloadManager == null){
            downloadManager = CustomDownloadService.getDownloadManager(context);
        } else {
            for (DownloadTask task:downloadManager.getAllTasks()) {
                task.pause(); ///关闭所有下载中的任务
            }
        }
         //设置缓存文件路径
         String pathStr = context.getApplicationContext().getFilesDir().getAbsolutePath()+"/download/";
         downloadManager.setTargetFolder(pathStr);
         downloadManager.loadDownloadInfo(identifier,true);

    }



    @Override
    public void onPlayRateOfProgress(int currentTime, int duration) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("progress", currentTime);
        resultMap.put("totalProgress", duration);
        result.success(resultMap);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        currentActivity = new WeakReference<Activity>(binding.getActivity());
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        onAttachedToActivity(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        currentActivity = null;
    }


}
