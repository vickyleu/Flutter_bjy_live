package com.xgs.flutter_live;

import android.app.Activity;
import android.content.Context;
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
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.videoplayer.ui.bean.VideoPlayerConfig;
import com.baijiayun.videoplayer.ui.playback.PBRoomUI;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;
import io.reactivex.android.schedulers.AndroidSchedulers;

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
        VideoPlayerConfig playerConfig=new VideoPlayerConfig();
        playerConfig.userId=backOption.getUserNum();
        playerConfig.userName=backOption.getUserName();
        PBRoomUI.enterPBRoom(activity, backOption.getRoomId(), backOption.getToken(), backOption.getSessionId(), playerConfig, s -> Toast.makeText(activity, s, Toast.LENGTH_SHORT).show());
    }

    static void startBJYLocalPlayBack(final Activity activity, BJYBackOption backOption, DownloadManager downloadManager) {
        VideoPlayerConfig playerConfig=new VideoPlayerConfig();
        playerConfig.userId=backOption.getUserNum();
        playerConfig.userName=backOption.getUserName();
        String identifier=backOption.getIdentifier();
        final DownloadTask item = downloadManager.getTaskByRoom(Long.parseLong(identifier), 0);
        if(item==null) {
            return;
        }
        DownloadModel videoModel=item.getVideoDownloadInfo();
        DownloadModel signalModel=item.getSignalDownloadInfo();
        PBRoomUI.enterLocalPBRoom(activity,videoModel, signalModel, playerConfig);
    }


    // 跳转到点播
    public  static void startBJYPVideo(final Activity activity, BJYVideoOption videoOption) {
        Intent intent = new Intent(activity, BJYVideoPlayerActivity.class);
        intent.putExtras(videoOption.bundle());
        activity.startActivity(intent);
    }


    // 开启下载
    static void addingDownloadQueue(Context context, MethodChannel channel,MethodChannel.Result result,DownloadManager downloadManager,
                                    String roomId, String token,String userId) {
        downloadManager.newPlaybackDownloadTask(userId, Long.parseLong(roomId), 0,token, "回放下载")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downloadTask -> {
                    downloadTask = downloadManager.getTaskByRoom(Long.parseLong(roomId), 0);
                    bindListener(channel, result, userId, downloadTask);
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
                    ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                    int state =3;
                    Map<String,Object> dict=new HashMap<>();
                    dict.put("progress",progress);
                    dict.put("size",size);
                    dict.put("userId",userId);
                    dict.put("state",state);
                    dict.put("path",null);
                    dict.put("speed","0K");
                    dict.put("itemIdentifier",roomId);
                    dict.put("fileName",null);

                    channel.invokeMethod("notifyChange",  dict);
                }).toString();
    }

    static void bindListener(MethodChannel channel, MethodChannel.Result result, String userId, DownloadTask downloadTask) {
        downloadTask.setDownloadListener(getDownloadListener(channel, result, userId));
    }

    @NotNull
    private static DownloadListener getDownloadListener(MethodChannel channel, MethodChannel.Result result,String userId) {
        return new DownloadListener() {
            @Override
            public void onStarted(DownloadTask task) {
                Log.e("视频下载","视频下载onStarted ====================");
                double size = task.getTotalLength();
                float progress = 0;
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId+"";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败

                Map<String,Object> dict=new HashMap<>();
                dict.put("code",1);
                dict.put("userId",userId);
                dict.put("msg","开始下载");
                dict.put("speed","0K");
                dict.put("progress",progress);
                dict.put("size",size);
                dict.put("path",path);
                dict.put("state",0);

                dict.put("itemIdentifier",itemIdentifier);
                dict.put("fileName",fileName);
                result.success(dict);
            }

            @Override
            public void onPaused(DownloadTask task) {
                Log.e("视频下载","视频下载onPaused ====================");
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId+"";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state =2;
                Map<String,Object> dict=new HashMap<>();
                dict.put("progress",progress);
                dict.put("size",size);
                dict.put("userId",userId);
                dict.put("state",state);
                dict.put("path",path);
                dict.put("speed","0K");

                dict.put("itemIdentifier",itemIdentifier);
                dict.put("fileName",fileName);
                channel.invokeMethod("notifyChange",  dict);
            }

            @Override
            public void onDeleted(DownloadTask task) {
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
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId+"";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state =3;
                Map<String,Object> dict=new HashMap<>();
                dict.put("progress",progress);
                dict.put("size",size);
                dict.put("userId",userId);
                dict.put("state",state);
                dict.put("path",path);
                dict.put("speed","0K");

                dict.put("itemIdentifier",itemIdentifier);
                dict.put("fileName",fileName);
                channel.invokeMethod("notifyChange",  dict);
            }

            @Override
            public void onFinish(DownloadTask task) {
                Log.e("视频下载","视频下载onFinish ===========videoFilePath："+task.getVideoFilePath());
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                String fileName = task.getVideoFileName();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId+"";
                String path = task.getVideoFilePath();
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state =1;
                Map<String,Object> dict=new HashMap<>();
                dict.put("progress",progress);
                dict.put("size",size);
                dict.put("userId",userId);
                dict.put("state",state);
                dict.put("path",path);
                dict.put("speed","0K");

                dict.put("itemIdentifier",itemIdentifier);
                dict.put("fileName",fileName);
                DownloadModel videoModel=task.getVideoDownloadInfo();
                DownloadModel signalModel=task.getSignalDownloadInfo();



                dict.put("videoModel",fileName);

                channel.invokeMethod("notifyChange",  dict);
            }

            @Override
            public void onProgress(DownloadTask task) {
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载","视频下载onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
                long speed = task.getSpeed();
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId+"";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state = 0;
                Map<String,Object> dict=new HashMap<>();
                dict.put("progress",progress);
                dict.put("size",size);
                dict.put("state",state);
                dict.put("userId",userId);
                dict.put("path",path);
                dict.put("speed",getFileSizeString(speed));

                dict.put("itemIdentifier",itemIdentifier);
                dict.put("fileName",fileName);
                channel.invokeMethod("notifyChange",  dict);
            }
        };
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

    static void pauseAllDownloadQueue(MethodChannel channel,MethodChannel.Result result, DownloadManager downloadManager,boolean pause,String userId) {
        List<DownloadTask> tasks = downloadManager.getAllTasks();
        List<Map<String,Object>>list=new ArrayList<>();
        for (DownloadTask task:tasks) {
            double progress = task.getDownloadedLength();
            double size = task.getTotalLength();
            String fileName = task.getVideoFileName();
            DownloadModel info = task.getVideoDownloadInfo();
            String itemIdentifier = info.roomId+"";
            String path = task.getVideoFilePath();
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            Map<String,Object> dict=new HashMap<>();
            dict.put("progress",progress);
            dict.put("size",size);
            dict.put("userId",userId);
            dict.put("path",path);
            dict.put("speed","0K");
            dict.put("itemIdentifier",itemIdentifier);
            dict.put("fileName",fileName);
            if(task.getTaskStatus() != TaskStatus.Error && task.getTaskStatus()  != TaskStatus.Finish){
                if (pause) {
                    task.pause();
                    dict.put("state",2);
                } else {
                    task.restart();
                    dict.put("state",0);
                }
            }else if(task.getTaskStatus()==TaskStatus.Error){
                dict.put("state",3);
            }else if(task.getTaskStatus()== TaskStatus.Finish){
                dict.put("state",1);
            }
            list.add(dict);
        }

        Map<String,Object> dict=new HashMap<>();
        if(pause){
            dict.put("code",1);
            dict.put("msg","暂停成功");
            dict.put("data",list);
            result.success(dict);
        }else {
            dict.put("code",1);
            dict.put("msg","恢复成功");
            dict.put("data",list);
            result.success(dict);
        }
    }
    static void pauseDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager, String roomId, boolean pause) {
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(roomId),//roomId
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

    static void queryDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager,String userId) {
        List<DownloadTask> arr = downloadManager.getAllTasks();
        List<Map<String,Object>> list=new ArrayList<>();
        for (DownloadTask element:arr) {
            float progress = element.getProgress();
            double size = element.getTotalLength();
            String fileName = element.getVideoFileName();
            DownloadModel info = element.getVideoDownloadInfo();
            String itemIdentifier = info.roomId+"";
            String path = element.getVideoFilePath();
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            int state = (info.status == TaskStatus.Finish) ? 1 : ((info.status == TaskStatus.Error||info.status == TaskStatus.Cancel) ? 3 :
                    ((info.status== TaskStatus.Pause) ? 2 : 0));
            Map<String,Object> dict=new HashMap<>();
            dict.put("progress",progress);
            dict.put("size",size);
            dict.put("state",state);
            dict.put("path",path);
            dict.put("userId",userId);
            dict.put("itemIdentifier",itemIdentifier);
            dict.put("fileName",fileName);
            dict.put("speed",getFileSizeString(element.getSpeed()));

            list.add(dict);
        }
        result.success(list);
    }

    static void removeDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager,String roomId) {
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(roomId),//roomId
                0
        );
        Map<String,Object> dict=new HashMap<>();
        if(task!=null){
            task.setDownloadListener(null);
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
