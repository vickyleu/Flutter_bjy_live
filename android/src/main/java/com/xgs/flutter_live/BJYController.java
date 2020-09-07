package com.xgs.flutter_live;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import com.baijiahulian.common.networkv2.HttpException;
import com.baijiayun.download.DownloadListener;
import com.baijiayun.download.DownloadManager;
import com.baijiayun.download.DownloadModel;
import com.baijiayun.download.DownloadTask;
import com.baijiayun.download.constant.TaskStatus;
import com.baijiayun.groupclassui.InteractiveClassUI;
import com.baijiayun.live.ui.LiveSDKWithUI;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.videoplayer.ui.playback.PBRoomUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;

/**
 * Created  on 2019/10/26.
 *
 * @author grey
 */
public class BJYController {

    public static VideoProgressListener videoProgressListener;

    // 跳转直播
    static void startLiveActivity(final Activity activity, BJYLiveOption option) {
        // 编辑用户信息
        InteractiveClassUI.LiveRoomUserModel userModel = new InteractiveClassUI.LiveRoomUserModel(option.getUserName(), option.getAvatarUrl(), option.getUserNum(), LPConstants.LPUserType.Student);
        // 进入直播房间
        InteractiveClassUI.enterRoom(activity, option.getRoomId(), option.getSign(), userModel, new InteractiveClassUI.InteractiveClassEnterRoomListener() {
            @Override
            public void onError(String s) {
                Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
            }
        });

        //LiveSDKWithUI.LiveRoomUserModel userModel = new LiveSDKWithUI.LiveRoomUserModel(option.getUserName(), option.getAvatarUrl(), option.getUserNum(), LPConstants.LPUserType.Student);
//        LiveSDKWithUI.enterRoom(activity, option.getRoomId(), option.getSign(), userModel, new LiveSDKWithUI.LiveSDKEnterRoomListener() {
//            @Override
//            public void onError(String s) {
//                Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
//            }
//        });

//        //退出直播间二次确认回调 无二次确认无需设置
//        LiveSDKWithUI.setRoomExitListener(new LiveSDKWithUI.LPRoomExitListener() {
//            @Override
//            public void onRoomExit(Context context, LiveSDKWithUI.LPRoomExitCallback callback) {
//                callback.exit();
//            }
//        });
//
//        //设置直播单点登录
//
//        LiveSDKWithUI.setEnterRoomConflictListener(new LiveSDKWithUI.RoomEnterConflictListener() {
//            @Override
//            public void onConflict(Context context, LPConstants.LPEndType type, final LiveSDKWithUI.LPRoomExitCallback callback) {
//                if (context != null) {
//                    // 单点登录冲突 endType为冲突方终端类型
//                    new AlertDialog.Builder(context)
//                            .setTitle("提示")
//                            .setMessage("已在其他设备观看")
//                            .setCancelable(true)
//                            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    callback.exit();
//                                }
//                            })
//                            .create()
//                            .show();
//                }
//            }
//        });
    }

    // 跳转到回放
    static void startBJYPlayBack(final Activity activity, BJYBackOption backOption) {
        PBRoomUI.enterPBRoom(activity, backOption.getRoomId(), backOption.getToken(), backOption.getSessionId(), new PBRoomUI.OnEnterPBRoomFailedListener() {
            @Override
            public void onEnterPBRoomFailed(String s) {
                Toast.makeText(activity, s, Toast.LENGTH_SHORT).show();
            }
        });
    }


    // 跳转到点播
    static void startBJYPVideo(final Activity activity, BJYVideoOption videoOption) {
        Intent intent = new Intent(activity, BJYVideoPlayerActivity.class);
        intent.putExtras(videoOption.bundle());
        activity.startActivity(intent);
    }

    // 开启下载
    static void addingDownloadQueue(Context context, MethodChannel channel,MethodChannel.Result result,DownloadManager downloadManager,
                                    String roomId, String token,String userId) {
        downloadManager.newPlaybackDownloadTask(userId+0, Long.parseLong(roomId), 0,token, "回放下载")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downloadTask -> {
                    downloadTask = downloadManager.getTaskByRoom(Long.parseLong(roomId), 0);
                    downloadTask.setDownloadListener(new DownloadListener() {
                        @Override
                        public void onStarted(DownloadTask task) {
                            Log.e("视频下载","视频下载onStarted ====================");
                            double size = task.getTotalLength();
                            float progress = 0;
                            String finaName = task.getVideoFileName();
                            String coverImageUrl= "";
                            DownloadModel info = task.getVideoDownloadInfo();
                            String itemIdentifier = info.roomId+"";
                            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败

                            Map<String,Object> dict=new HashMap<>();
                            dict.put("code",1);
                            dict.put("msg","开始下载");
                            dict.put("speed","0K");
                            dict.put("progress",progress);
                            dict.put("size",size);
                            dict.put("state",0);

                            dict.put("itemIdentifier",itemIdentifier);
                            dict.put("finaName",finaName);
                            dict.put("coverImageUrl",coverImageUrl);
                            result.success(dict);
                        }

                        @Override
                        public void onPaused(DownloadTask task) {
                            Log.e("视频下载","视频下载onPaused ====================");
                            double progress = task.getDownloadedLength();
                            double size = task.getTotalLength();
                            Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                            String finaName = task.getVideoFileName();
                            String coverImageUrl= "";
                            DownloadModel info = task.getVideoDownloadInfo();
                            String itemIdentifier = info.roomId+"";
                            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                            int state =2;
                            Map<String,Object> dict=new HashMap<>();
                            dict.put("progress",progress);
                            dict.put("size",size);
                            dict.put("state",state);
                            dict.put("speed","0K");

                            dict.put("itemIdentifier",itemIdentifier);
                            dict.put("finaName",finaName);
                            dict.put("coverImageUrl",coverImageUrl);

                            channel.invokeMethod("notifyChange",  dict);
                        }

                        @Override
                        public void onDeleted(DownloadTask p0) {
                            Log.e("视频下载","视频下载onDeleted ====================");

                        }

                        @Override
                        public void onError(DownloadTask task, HttpException p1) {
                            Log.e("视频下载","视频下载onError ====================");

                            Map<String,Object> dict1=new HashMap<>();
                            dict1.put("code",0);
                            dict1.put("msg","下载失败");
                            try {
                                result.success(dict1);
                            }catch (Exception ignored){}
                            double progress = task.getDownloadedLength();
                            double size = task.getTotalLength();
                            Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                            String finaName = task.getVideoFileName();
                            String coverImageUrl= "";
                            DownloadModel info = task.getVideoDownloadInfo();
                            String itemIdentifier = info.roomId+"";
                            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                            int state =3;
                            Map<String,Object> dict=new HashMap<>();
                            dict.put("progress",progress);
                            dict.put("size",size);
                            dict.put("state",state);
                            dict.put("speed","0K");

                            dict.put("itemIdentifier",itemIdentifier);
                            dict.put("finaName",finaName);
                            dict.put("coverImageUrl",coverImageUrl);

                            channel.invokeMethod("notifyChange",  dict);
                        }

                        @Override
                        public void onFinish(DownloadTask task) {
                            Log.e("视频下载","视频下载onFinish ===========videoFilePath："+task.getVideoFilePath());
                            double progress = task.getDownloadedLength();
                            double size = task.getTotalLength();
                            Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                            String finaName = task.getVideoFileName();
                            String coverImageUrl= "";
                            DownloadModel info = task.getVideoDownloadInfo();
                            String itemIdentifier = info.roomId+"";
                            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                            int state =1;
                            Map<String,Object> dict=new HashMap<>();
                            dict.put("progress",progress);
                            dict.put("size",size);
                            dict.put("state",state);
                            dict.put("speed","0K");

                            dict.put("itemIdentifier",itemIdentifier);
                            dict.put("finaName",finaName);
                            dict.put("coverImageUrl",coverImageUrl);

                            channel.invokeMethod("notifyChange",  dict);
                        }

                        @Override
                        public void onProgress(DownloadTask task) {
                            double progress = task.getDownloadedLength();
                            double size = task.getTotalLength();
                            Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                            long speed = task.getSpeed();
                            String finaName = task.getVideoFileName();
                            String coverImageUrl= "";
                            DownloadModel info = task.getVideoDownloadInfo();
                            String itemIdentifier = info.roomId+"";
                            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                            int state = (info.status == TaskStatus.Finish) ? 1 : ((info.status == TaskStatus.Error||info.status == TaskStatus.Cancel) ? 3 :
                                    ((info.status== TaskStatus.Pause) ? 2 : 0));
                            Map<String,Object> dict=new HashMap<>();
                            dict.put("progress",progress);
                            dict.put("size",size);
                            dict.put("state",state);
                            dict.put("speed",getFileSizeString(speed));

                            dict.put("itemIdentifier",itemIdentifier);
                            dict.put("finaName",finaName);
                            dict.put("coverImageUrl",coverImageUrl);

                            channel.invokeMethod("notifyChange",  dict);
                        }
                    });
                    downloadTask.start();
                }, throwable -> {
                    throwable.printStackTrace();

                    Map<String,Object> dict1=new HashMap<>();
                    dict1.put("code",0);
                    dict1.put("msg","下载失败");
                    try {
                        result.success(dict1);
                    }catch (Exception ignored){}
                    double progress = 0;
                    double size = 0;
                    String coverImageUrl= "";
                    ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                    int state =3;
                    Map<String,Object> dict=new HashMap<>();
                    dict.put("progress",progress);
                    dict.put("size",size);
                    dict.put("state",state);
                    dict.put("speed","0K");

                    dict.put("itemIdentifier",roomId);
                    dict.put("finaName",null);
                    dict.put("coverImageUrl",coverImageUrl);

                    channel.invokeMethod("notifyChange",  dict);
                }).toString();
    }

    static String getFileSizeString(long size) {
        if(size >= 1024*1024.0)//大于1M，则转化成M单位的字符串
        {
            return String.format(Locale.CHINA,"%1.2fM",(size / 1024.0 / 1024.0));
        }
        else if(size >= 1024.0 && size<1024.0*1024.0) //不到1M,但是超过了1KB，则转化成KB单位
        {
            return String.format(Locale.CHINA,"%1.2fK",size / 1024.0);
        }
        else//剩下的都是小于1K的，则转化成B单位
        {
            return String.format(Locale.CHINA, "%1.2fB",(double)size);
        }
    }

    static void pauseDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager, String identifier, boolean pause) {
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(identifier),//roomId
                0
        );
        Map<String,Object> dict=new HashMap<>();
        if(pause){
            task.pause();
            dict.put("code",1);
            dict.put("msg","暂停成功");
            result.success(dict);
        }else {
            task.restart();
            dict.put("code",1);
            dict.put("msg","恢复成功");
            result.success(dict);
        }
    }

    static void queryDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager) {
        List<DownloadTask> arr = downloadManager.getAllTasks();
        List<Map<String,Object>> list=new ArrayList<>();
        for (DownloadTask element:arr) {
            float progress = element.getProgress();
            double size = element.getTotalLength();
            String finaName = element.getVideoFileName();
            String coverImageUrl= "";
            DownloadModel info = element.getVideoDownloadInfo();
            String itemIdentifier = info.roomId+"";
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            int state = (info.status == TaskStatus.Finish) ? 1 : ((info.status == TaskStatus.Error||info.status == TaskStatus.Cancel) ? 3 :
                    ((info.status== TaskStatus.Pause) ? 2 : 0));
            Map<String,Object> dict=new HashMap<>();
            dict.put("progress",progress);
            dict.put("size",size);
            dict.put("state",state);

            dict.put("itemIdentifier",itemIdentifier);
            dict.put("finaName",finaName);
            dict.put("coverImageUrl",coverImageUrl);
            dict.put("speed",getFileSizeString(element.getSpeed()));

            list.add(dict);
        }
        result.success(list);
    }

    static void removeDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager,String identifier) {
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(identifier),//roomId
                0
        );
        Map<String,Object> dict=new HashMap<>();
        if(task!=null){
            downloadManager.deleteTask(task);
            dict.put("code",1);
            dict.put("msg","删除成功");
            result.success(dict);
        }else{
            dict.put("code",0);
            dict.put("msg","删除失败");
            result.success(dict);
        }
    }



    // 进度回调
    static void onPlayRateOfProgress(int currentTime, int duration) {
        if (videoProgressListener != null) {
            videoProgressListener.onPlayRateOfProgress(currentTime, duration);
        }
    }



}
