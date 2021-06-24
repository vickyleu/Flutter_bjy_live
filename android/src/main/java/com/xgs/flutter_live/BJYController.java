package com.xgs.flutter_live;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.baijiahulian.common.networkv2.HttpException;
import com.baijiayun.download.DownloadListener;
import com.baijiayun.download.DownloadManager;
import com.baijiayun.download.DownloadModel;
import com.baijiayun.download.DownloadTask;
import com.baijiayun.download.constant.TaskStatus;
import com.baijiayun.groupclassui.InteractiveClassUI;
import com.baijiayun.live.ui.LiveSDKWithUI;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPSignEnterRoomModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.videoplayer.ui.bean.VideoPlayerConfig;
import com.baijiayun.videoplayer.ui.playback.PBRoomUI;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
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
        if(option.isInteractive()){///专业小班课
            InteractiveClassUI.enterRoom(activity,new LPSignEnterRoomModel(option.getRoomId(),
                    new LiveRoomUserModel(option.getUserName(), option.getAvatarUrl(), option.getUserNum(), LPConstants.LPUserType.Student),
                    option.getSign()));
        }else{ ////大班课
            LiveSDKWithUI.enterRoom(activity, new LPSignEnterRoomModel(option.getRoomId(),
                    new LiveRoomUserModel(option.getUserName(), option.getAvatarUrl(), option.getUserNum(), LPConstants.LPUserType.Student),
                    option.getSign()));
            //退出直播间二次确认回调 无二次确认无需设置
            LiveSDKWithUI.setRoomExitListener((context, callback) -> callback.exit());
            //设置直播单点登录
            LiveSDKWithUI.setEnterRoomConflictListener((context, type, callback) -> {
                if (context != null) {
                    // 单点登录冲突 endType为冲突方终端类型
                    new AlertDialog.Builder(context)
                            .setTitle("提示")
                            .setMessage("已在其他设备观看")
                            .setCancelable(true)
                            .setPositiveButton("确定", (dialog, which) -> callback.exit())
                            .create()
                            .show();
                }
            });
        }

    }

    // 跳转到回放
    static void startBJYPlayBack(final Activity activity, BJYBackOption backOption) {
        VideoPlayerConfig playerConfig = new VideoPlayerConfig();
        playerConfig.userId = backOption.getUserNum();
        playerConfig.userName = backOption.getUserName();
        PBRoomUI.enterPBRoom(activity, backOption.getRoomId(), backOption.getToken(), backOption.getSessionId(), playerConfig, s -> Toast.makeText(activity, s, Toast.LENGTH_SHORT).show());
    }

    static void startBJYLocalPlayBack(final Activity activity, BJYBackOption backOption, DownloadManager downloadManager) {
        VideoPlayerConfig playerConfig = new VideoPlayerConfig();
        playerConfig.userId = backOption.getUserNum();
        playerConfig.userName = backOption.getUserName();
        String identifier = backOption.getIdentifier();
        final DownloadTask item = downloadManager.getTaskByRoom(Long.parseLong(identifier), 0);
        if (item == null) {
            return;
        }
        DownloadModel videoModel = item.getVideoDownloadInfo();
        DownloadModel signalModel = item.getSignalDownloadInfo();
        PBRoomUI.enterLocalPBRoom(activity, videoModel, signalModel, playerConfig);
    }


    // 跳转到点播
    public static void startBJYPVideo(final Activity activity, BJYVideoOption videoOption) {
        Intent intent = new Intent(activity, BJYVideoPlayerActivity.class);
        intent.putExtras(videoOption.bundle());
        activity.startActivity(intent);
    }

    static String BJYLOG = "😋😋😋百家云的LOG😋😋😋";

    // 开启下载
    static void addingDownloadQueue(Context context, MethodChannel channel, MethodChannel.Result result, DownloadManager downloadManager,
                                    String roomId, String token, String userId) {
        Log.e(BJYLOG, "addingDownloadQueue:" + Long.parseLong(roomId));
        downloadManager.newPlaybackDownloadTask(userId, Long.parseLong(roomId), 0, token, "回放下载")
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(downloadTask -> {
                    Log.e(BJYLOG, "subscribe:" + Long.parseLong(roomId));
                    Log.e("result", "subscribe:" + downloadTask);
                    downloadTask = downloadManager.getTaskByRoom(Long.parseLong(roomId), 0);
                    bindListener(channel, result, userId, downloadTask);
                    downloadTask.restart();
                }, throwable -> {
                    Log.e(BJYLOG, "throwable:" + Long.parseLong(roomId));
                    throwable.printStackTrace();
                    Log.e("result", "throwable:" + throwable);
                    Map<String, Object> dict1 = new HashMap<>();
                    dict1.put("code", 0);
                    dict1.put("msg", "下载失败");
                    try {
                        result.success(dict1);
                    } catch (Exception ignored) {
                    }
//                    double progress = 0;
//                    double size = 0;
//                    ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
//                    int state =3;
//                    Map<String,Object> dict=new HashMap<>();
//                    dict.put("progress",progress);
//                    dict.put("size",size);
//                    dict.put("userId",userId);
//                    dict.put("state",state);
//                    dict.put("path",null);
//                    dict.put("speed","0K");
//                    dict.put("itemIdentifier",roomId);
//                    dict.put("fileName",null);
//                    try {
//                        channel.invokeMethod("notifyChange",  dict);
//                    }catch (Exception e){}
                }).toString();
    }

    static void bindListener(MethodChannel channel, MethodChannel.Result result, String userId, DownloadTask downloadTask) {
        downloadTask.setDownloadListener(getDownloadListener(channel, result, userId));
    }

    @NotNull
    private static DownloadListener getDownloadListener(MethodChannel channel, MethodChannel.Result result, String userId) {
        return new DownloadListener() {
            @Override
            public void onStarted(DownloadTask task) {
                Log.e("视频下载", "视频下载onStarted ====================");
                double size = task.getTotalLength();
                Log.e(BJYLOG, "onStarted:" + task.getVideoDownloadInfo().roomId);
                float progress = 0;
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId + "";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败

                Map<String, Object> dict = new HashMap<>();
                dict.put("code", 1);
                dict.put("userId", userId);
                dict.put("msg", "开始下载");
                dict.put("speed", "0K");
                dict.put("progress", progress);
                dict.put("size", size);
                dict.put("path", path);
                dict.put("state", 0);

                dict.put("itemIdentifier", itemIdentifier);
                dict.put("fileName", fileName);
                try {
                    result.success(dict);
                } catch (Exception e) {
                }
            }

            @Override
            public void onPaused(DownloadTask task) {
                Log.e("视频下载", "视频下载onPaused ====================");
                Log.e(BJYLOG, "onPaused:" + task.getVideoDownloadInfo().roomId);
//                double progress = task.getDownloadedLength();
//                double size = task.getTotalLength();
//                int p=( (int) (progress * 100 / size));
//                Log.e("视频下载","视频下载onPaused onProgress==="+task.getVideoFileName()+"--->"+( (int) (progress * 100 / size))+" %");
//                String fileName = task.getVideoFileName();
//                String path = task.getVideoFilePath();
//                DownloadModel info = task.getVideoDownloadInfo();
//                String itemIdentifier = info.roomId+"";
//                Log.e("getTaskStatus",task.getTaskStatus().name());
//
//                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
//                int state =task.getTaskStatus()==TaskStatus.Finish?3:2;
//                Map<String,Object> dict=new HashMap<>();
//                dict.put("progress",progress);
//                dict.put("size",size);
//                dict.put("userId",userId);
//                dict.put("state",state);
//                dict.put("path",path);
//                dict.put("speed","0K");
//
//                dict.put("itemIdentifier",itemIdentifier);
//                dict.put("fileName",fileName);
//                try {
//                    channel.invokeMethod("notifyChange",  dict);
//                }catch (Exception e){}

            }

            @Override
            public void onDeleted(DownloadTask task) {
                Log.e("视频下载", "视频下载onDeleted ====================");
                Log.e(BJYLOG, "onDeleted:" + task.getVideoDownloadInfo().roomId);
            }

            @Override
            public void onError(DownloadTask task, HttpException p1) {
                Log.e("视频下载", "视频下载onError ====================");
                Log.e(BJYLOG, "onError:" + task.getVideoDownloadInfo().roomId);
                Map<String, Object> dict1 = new HashMap<>();
                dict1.put("code", 0);
                dict1.put("msg", "下载失败");
                try {
                    result.success(dict1);
                } catch (Exception ignored) {
                }
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载", "视频下载onError onProgress===" + task.getVideoFileName() + "--->" + ((int) (progress * 100 / size)) + " %");
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId + "";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state = 3;
                Map<String, Object> dict = new HashMap<>();
                dict.put("progress", progress);
                dict.put("size", size);
                dict.put("userId", userId);
                dict.put("state", state);
                dict.put("path", path);
                dict.put("speed", "0K");

                dict.put("itemIdentifier", itemIdentifier);
                dict.put("fileName", fileName);
                try {
                    channel.invokeMethod("notifyChange", dict);
                } catch (Exception e) {
                }
            }

            @Override
            public void onFinish(DownloadTask task) {
                Log.e("视频下载", "视频下载onFinish ===========videoFilePath：" + task.getVideoFilePath());
                Log.e(BJYLOG, "onFinish:" + task.getVideoDownloadInfo().roomId);
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载", "视频下载onFinish onProgress===" + task.getVideoFileName() + "--->" + ((int) (progress * 100 / size)) + " %");
                String fileName = task.getVideoFileName();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId + "";
                String path = task.getVideoFilePath();
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state = 1;
                Map<String, Object> dict = new HashMap<>();
                dict.put("progress", progress);
                dict.put("size", size);
                dict.put("userId", userId);
                dict.put("state", state);
                dict.put("path", path);
                dict.put("speed", "0K");

                dict.put("itemIdentifier", itemIdentifier);
                dict.put("fileName", fileName);
                DownloadModel videoModel = task.getVideoDownloadInfo();
                DownloadModel signalModel = task.getSignalDownloadInfo();


                dict.put("videoModel", fileName);
                try {
                    channel.invokeMethod("notifyChange", dict);
                } catch (Exception e) {
                }
            }

            @Override
            public void onProgress(DownloadTask task) {
                Log.e(BJYLOG, "onProgress:" + task.getVideoDownloadInfo().roomId);
                double progress = task.getDownloadedLength();
                double size = task.getTotalLength();
                Log.e("视频下载", "视频下载 onProgress===" + task.getVideoFileName() + "--->" + ((int) (progress * 100 / size)) + " %");
                long speed = task.getSpeed();
                String fileName = task.getVideoFileName();
                String path = task.getVideoFilePath();
                DownloadModel info = task.getVideoDownloadInfo();
                String itemIdentifier = info.roomId + "";
                ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
                int state = 0;
                Map<String, Object> dict = new HashMap<>();
                dict.put("progress", progress);
                dict.put("size", size);
                dict.put("state", state);
                dict.put("userId", userId);
                dict.put("path", path);
                dict.put("speed", getFileSizeString(speed));

                dict.put("itemIdentifier", itemIdentifier);
                dict.put("fileName", fileName);
                try {
                    channel.invokeMethod("notifyChange", dict);
                } catch (Exception e) {
                }
            }
        };
    }

    static String getFileSizeString(long size) {
        if (size >= 1024 * 1024.0)//大于1M，则转化成M单位的字符串
        {
            return String.format(Locale.CHINA, "%1.2fM", (size / 1024.0 / 1024.0));
        } else if (size >= 1024.0 && size < 1024.0 * 1024.0) //不到1M,但是超过了1KB，则转化成KB单位
        {
            return String.format(Locale.CHINA, "%1.2fK", size / 1024.0);
        } else//剩下的都是小于1K的，则转化成B单位
        {
            return String.format(Locale.CHINA, "%1.2fB", (double) size);
        }
    }

    static void pauseAllDownloadQueue(MethodChannel channel, MethodChannel.Result result, DownloadManager downloadManager, boolean pause, String userId) {
        Log.e(BJYLOG, "pauseAllDownloadQueue:");
        List<DownloadTask> tasks = downloadManager.getAllTasks();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DownloadTask task : tasks) {
            double progress = task.getDownloadedLength();
            double size = task.getTotalLength();
            String fileName = task.getVideoFileName();
            DownloadModel info = task.getVideoDownloadInfo();
            String itemIdentifier = info.roomId + "";
            String path = task.getVideoFilePath();
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            Map<String, Object> dict = new HashMap<>();
            dict.put("progress", progress);
            dict.put("size", size);
            dict.put("userId", userId);
            dict.put("path", path);
            dict.put("speed", "0K");
            dict.put("itemIdentifier", itemIdentifier);
            dict.put("fileName", fileName);
            if (task.getTaskStatus() != TaskStatus.Error && task.getTaskStatus() != TaskStatus.Finish) {
                if (pause) {
                    task.setDownloadListener(null);
                    pauseTask(task);
                    dict.put("state", 2);
                } else {
                    bindListener(channel, result, userId, task);
                    task.start();
                    dict.put("state", 0);
                }
            } else if (task.getTaskStatus() == TaskStatus.Error) {
                dict.put("state", 3);
            } else if (task.getTaskStatus() == TaskStatus.Finish) {
                dict.put("state", 1);
            }
            list.add(dict);
        }

        Map<String, Object> dict = new HashMap<>();
        if (pause) {
            dict.put("code", 1);
            dict.put("msg", "暂停成功");
            dict.put("data", list);
            try {
                result.success(dict);
            } catch (Exception e) {
            }
        } else {
            dict.put("code", 1);
            dict.put("msg", "恢复成功");
            dict.put("data", list);
            try {
                result.success(dict);
            } catch (Exception e) {
            }
        }
    }

    static void pauseDownloadQueue(MethodChannel channel, MethodChannel.Result result, String userId, DownloadManager downloadManager, String roomId, boolean pause) {
        Log.e(BJYLOG, "pauseAllDownloadQueue:" + Long.parseLong(roomId));
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(roomId),//roomId
                0
        );
        Map<String, Object> dict = new HashMap<>();
        if (pause) {
            task.setDownloadListener(null);
            pauseTask(task);
            dict.put("code", 1);
            dict.put("msg", "暂停成功");
            try {
                result.success(dict);
            } catch (Exception e) {
            }
        } else {
            bindListener(channel, result, userId, task);
            task.start();
            dict.put("code", 1);
            dict.put("msg", "恢复成功");
            try {
                result.success(dict);
            } catch (Exception e) {
            }
        }
    }

    static void pauseTask(DownloadTask task) {


        task.pause();
        try {
            Log.e("getCanonicalName",""+task.getClass().getCanonicalName());
            Class<?> f = Class.forName("a.a.a.f");
            Field bField = f.getDeclaredField("b");
            bField.setAccessible(true);
            DownloadModel b = (DownloadModel) bField.get(task);
            DownloadModel var1=null;
            while (var1!=null?(var1=b)!=null:(var1=var1.nextModel)!=null) {
                var1.status=TaskStatus.Pause;
            }

            Field gField = f.getDeclaredField("g");
            gField.setAccessible(true);
            gField.set(task, TaskStatus.Pause);
            Field cField = f.getDeclaredField("c");
            cField.setAccessible(true);
            Object c=cField.get(task);
            Field ccField= c.getClass().getField("c");
            ccField.setAccessible(true);
            ccField.set(c, true);
        } catch (Exception ignored) {
            ignored.printStackTrace();
            Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
        }
        task.pause();
    }

    static void queryDownloadQueue(MethodChannel.Result result, DownloadManager downloadManager, String userId) {
        Log.e(BJYLOG, "queryDownloadQueue:");
        List<DownloadTask> arr = downloadManager.getAllTasks();
        List<Map<String, Object>> list = new ArrayList<>();
        for (DownloadTask element : arr) {
            float progress = element.getProgress();
            double size = element.getTotalLength();
            String fileName = element.getVideoFileName();
            DownloadModel info = element.getVideoDownloadInfo();
            String itemIdentifier = info.roomId + "";
            String path = element.getVideoFilePath();
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            int state = (info.status == TaskStatus.Finish) ? 1 : ((info.status == TaskStatus.Error || info.status == TaskStatus.Cancel) ? 3 :
                    ((info.status == TaskStatus.Pause) ? 2 : 0));
            Map<String, Object> dict = new HashMap<>();
            dict.put("progress", progress);
            dict.put("size", size);
            dict.put("state", state);
            dict.put("path", path);
            dict.put("userId", userId);
            dict.put("itemIdentifier", itemIdentifier);
            dict.put("fileName", fileName);
            dict.put("speed", getFileSizeString(element.getSpeed()));

            list.add(dict);
        }
        try {
            result.success(list);
        } catch (Exception e) {
        }
    }

    static void removeDownloadQueue(MethodChannel channel, MethodChannel.Result result, DownloadManager downloadManager, String roomId, String userId) {
        Log.e(BJYLOG, "removeDownloadQueue:" + roomId);
        DownloadTask task = downloadManager.getTaskByRoom(
                Long.parseLong(roomId),//roomId
                0
        );
        Log.e(BJYLOG, "getTaskByRoom:" + roomId + "  task:" + task);
        Map<String, Object> dict_ = new HashMap<>();
        if (task != null) {

            double progress = task.getDownloadedLength();
            double size = task.getTotalLength();
            Log.e("视频下载", "视频下载 onProgress===" + task.getVideoFileName() + "--->" + ((int) (progress * 100 / size)) + " %");
            String fileName = task.getVideoFileName();
            String path = task.getVideoFilePath();
            DownloadModel info = task.getVideoDownloadInfo();
            String itemIdentifier = info.roomId + "";
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            int state = 3;//此状态为删除,因为sdk onDelete回调不调用,会延时300ms查询数据库是否还存在
            Map<String, Object> dict = new HashMap<>();
            dict.put("progress", progress);
            dict.put("size", size);
            dict.put("state", state);
            dict.put("userId", userId);
            dict.put("path", path);
            dict.put("speed", "0K");

            dict.put("itemIdentifier", itemIdentifier);
            dict.put("fileName", fileName);
//            try {
//                Class<?> f = Class.forName("com.baijiayun.download.f");
//                try {
//                    Field uField = f.getDeclaredField("u");
//                    uField.setAccessible(true);
//                    uField.set(task, TaskStatus.Pause);
//                } catch (Exception ignored) {
//                    ignored.printStackTrace();
//                    Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//                }
//                try {
//                    Field tField = f.getDeclaredField("t");
//                    tField.setAccessible(true);
//                    if ((Boolean) tField.get(task)) {
//                        tField.set(task, false);
//                    }
//                } catch (Exception ignored) {
//                    ignored.printStackTrace();
//                    Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//                }
//                try {
//                    Field rField = f.getDeclaredField("r");
//                    rField.setAccessible(true);
//                    AsyncTask c = (AsyncTask) rField.get(task);
//                    if (c != null) {
//                        c.cancel(true);
//                    }
//                } catch (Exception ignored) {
//                    ignored.printStackTrace();
//                    Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//                }
//                try {
//                    Field gField = f.getDeclaredField("g");
//                    Field sField = f.getDeclaredField("s");
//                    Class<?> e = Class.forName("com.baijiayun.download.e");
//                    gField.setAccessible(true);
//                    sField.setAccessible(true);
//
//                    DownloadModel g = (DownloadModel) gField.get(task);
//                    if (g != null) {
//                        DownloadModel var1 = g;
//                        FileUtils.safeDeleteFile(new File(var1.targetFolder, "/subtitle/" + var1.videoId));
//                        try {
//                            task.cancel();
//                        }catch (Exception ignored){
//                            ignored.printStackTrace();
//                            Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//                        }
//                        while(true) {
//                            while(var1 != null) {
//                                if (!TextUtils.isEmpty(var1.targetFolder) && !TextUtils.isEmpty(var1.targetName)) {
//                                    File var2 = new File(var1.targetFolder, var1.targetName);
//                                    FileUtils.safeDeleteFile(var2);
//                                    var1 = var1.nextModel;
//                                } else {
//                                    var1 = var1.nextModel;
//                                }
//                            }
//                            g.downloadLength = 0L;
//                            g.status = TaskStatus.Pause;
//                            Object s=sField.get(task);
//                            if(s!=null){
//                                Method method = e.getMethod("g", DownloadModel.class);
//                                method.setAccessible(true);
//                                method.invoke(s,g);
//                            }
//                            break;
//                        }
//                    }
//                } catch (Exception ignored) {
//                    ignored.printStackTrace();
//                    Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//                }
//            } catch (Exception ignored) {
//                ignored.printStackTrace();
//                Log.e(BJYLOG, "getMessage:" + ignored.getMessage());
//            }

            downloadManager.deleteTask(task);
            task.setDownloadListener(null);
            task.cancel();
            pauseTask(task);
            Log.e(BJYLOG, "deleteTask:" + roomId);
            dict_.put("code", 1);
            dict_.put("msg", "删除成功");
            try {
                result.success(dict_);
            } catch (Exception ignored) {
            }
            try {
                channel.invokeMethod("notifyChange", dict);
            } catch (Exception ignored) {
            }
        } else {
            dict_.put("code", 0);
            dict_.put("msg", "删除失败");
            try {
                result.success(dict_);
            } catch (Exception ignored) {
            }
        }
    }

    // 进度回调
    static void onPlayRateOfProgress(int currentTime, int duration) {
        if (videoProgressListener != null) {
            videoProgressListener.onPlayRateOfProgress(currentTime, duration);
        }
    }


}

class LiveRoomUserModel implements IUserModel {

    String userName;
    String userAvatar;
    String userNumber;
    LPConstants.LPUserType userType;

    public LiveRoomUserModel(@NonNull String userName, @Nullable String userAvatar, @Nullable String userNumber, @NonNull LPConstants.LPUserType userType) {
        this.userName = userName;
        this.userAvatar = userAvatar;
        this.userNumber = userNumber;
        this.userType = userType;
    }

    @Override
    public String getUserId() {
        return null;
    }

    @Override
    public String getNumber() {
        return userNumber;
    }

    @Override
    public String getReplaceNumber() {
        return null;
    }

    @Override
    public String getUserNumberReplaceMe() {
        return null;
    }

    @Override
    public boolean canReplaceOtherUser(IUserModel iUserModel) {
        return false;
    }



    @Override
    public LPConstants.LPUserType getType() {
        return userType;
    }

    @Override
    public String getName() {
        return userName;
    }

    @Override
    public String getAvatar() {
        return userAvatar;
    }

    @Override
    public int getGroup() {
        return 0;
    }

    @Override
    public LPConstants.LPEndType getEndType() {
        return LPConstants.LPEndType.Android;
    }

    //        @Override
    public Map<String, Object> getWebRTCInfo() {
        return new HashMap<>();
    }
}
