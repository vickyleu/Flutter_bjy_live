package com.xgs.flutter_live;

import android.content.Context;

import com.baijiayun.download.DownloadManager;
import com.baijiayun.download.DownloadTask;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * Created  on 2019/10/11.
 *
 * @author grey
 */
public class FlutterLivePlugin implements MethodCallHandler, BJYController.VideoProgressListener {

    private final MethodChannel methodChannel;
    private MethodChannel.Result result;
    private final Registrar registrar;

    private DownloadManager downloadManager;
    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        new FlutterLivePlugin(registrar);
    }

    @Override
    public void onMethodCall(@Nonnull MethodCall call, @Nonnull MethodChannel.Result result) {
        if (registrar.activity() == null) {
            result.error("no_activity", "Flutter_Live plugin requires a foreground activity.", null);
            return;
        }
        this.result = result;
        if (call.method.equals("startLive")) {
            BJYController.startLiveActivity(registrar.activity(), new BJYLiveOption().create(call));
            return;
        }else if (call.method.equals("startBack")) {
            BJYController.startBJYPlayBack(registrar.activity(), new BJYBackOption().create(call));
            return;
        }else if (call.method.equals("startVideo")) {
            BJYController.startBJYPVideo(registrar.activity(), new BJYVideoOption().create(call));
        }else if (call.method.equals("addingDownloadQueue")) {
            String classID = call.argument("classID");
            String sessionID = call.argument("sessionID");//todo Android和iOS不同的字段
            String userId = call.argument("userId");
            downloadManagerCheck(registrar.activity(),userId);
            BJYController.addingDownloadQueue(registrar.activity(),methodChannel,result,downloadManager,classID,sessionID,userId);
        }else if (call.method.equals("pauseDownloadQueue")) {
            String identifier = call.argument("identifier");
            String sessionId = call.argument("sessionId");//todo Android和iOS不同的字段
            String userId = call.argument("userId");
            boolean pause = (boolean) call.argument("pause");
            downloadManagerCheck(registrar.activity(),userId);
            BJYController.pauseDownloadQueue(registrar.activity(),methodChannel,downloadManager, userId,identifier,sessionId,pause);
        }else if (call.method.equals("queryDownloadQueue")) {
            String userId = call.argument("userId");
            downloadManagerCheck(registrar.activity(),userId);
            BJYController.queryDownloadQueue(registrar.activity(),result,downloadManager, userId);
        }else if (call.method.equals("removeDownloadQueue")) {
            String identifier = call.argument("identifier");
            String sessionId = call.argument("sessionId");//todo Android和iOS不同的字段
            String userId = call.argument("userId");
            downloadManagerCheck(registrar.activity(),userId);
            BJYController.removeDownloadQueue(registrar.activity(),result,downloadManager,userId,identifier,sessionId);
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
    private FlutterLivePlugin(Registrar registrar) {
        // 设置监听
        BJYController.videoProgressListener = this;

        this.registrar = registrar;
        methodChannel = new MethodChannel(registrar.messenger(), "flutter_live");
        methodChannel.setMethodCallHandler(this);
    }


    @Override
    public void onPlayRateOfProgress(int currentTime, int duration) {
        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("progress", currentTime);
        resultMap.put("totalProgress", duration);
        result.success(resultMap);
    }
}
