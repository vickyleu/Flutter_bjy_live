import 'dart:async';

import 'package:flutter/services.dart';

/// Created  on 2019/10/12.
/// @author grey
/// Function :  直接跳转百家云平台

typedef OnVideoProgressCallback = Function(int, int);
class FlutterLiveDownloadModel{
  String fileName;
  String coverImageUrl;
  String itemIdentifier;
  int progress;
  int size;
  int state;
  String speed;
  FlutterLiveDownloadModel({this.fileName, this.coverImageUrl, this.itemIdentifier, this.progress, this.size, this.state, this.speed});
}
class FlutterLive {
  factory FlutterLive() => _getInstance();

  static FlutterLive get instance => _getInstance();
  static FlutterLive _instance;

  static FlutterLive _getInstance() {
    if (_instance == null) {
      _instance = new FlutterLive._internal();
    }
    return _instance;
  }


  StreamController<FlutterLiveDownloadModel> streamController=StreamController.broadcast();

  FlutterLive._internal() {
    _channel = const MethodChannel('flutter_live');
    _channel.setMethodCallHandler(_methodCallHandler);
  }

  Future<dynamic> _methodCallHandler(MethodCall call) async {
    if (call.method == "video_progress") {}
  }

  MethodChannel _channel;

  // 跳转直播
  void startLiveActivity(String userName, String userNum, String userAvatar,
      String sign, String roomId) {
    _channel.invokeMethod("startLive", {
      'userName': userName,
      'userNum': userNum,
      'userAvatar': userAvatar,
      'sign': sign,
      'roomId': roomId,
    });
  }

  // 跳转在线回放
  void startPlayBackActivity(String roomId, String token, String sessionId,String userName,String userNum) {
    _channel.invokeMethod("startBack", {
      'roomId': roomId,
      'token': token,
      'sessionId': sessionId,
      'userName': userName,
      'userNum': userNum,
    });
  }

  // 跳转在线点播
  Future<double> startVideoActivity(String userName, String userId,
      String token, String videoId, String title) async {
    final dynamic map = await _channel.invokeMethod("startVideo", {
      'videoId': videoId,
      'token': token,
      'userName': userName,
      'userId': userId,
      'title': title,
    });

    if (map is Map) {
      int progress = map["progress"] ?? 0;
      int totalProgress = map["totalProgress"] ?? 0;
      if (totalProgress == 0) {
        return 0;
      }
      double rate = (progress / totalProgress) * 100;
      return rate;
    }
    return 0;
  }

  // 添加下载任务队列
  Future register() async {
    final dynamic map = await _channel.invokeMethod("register", {});
    return ;
  }
  // 添加下载任务队列
  Future<FlutterLiveDownloadModel> addingDownloadQueue(
      String classID, String userId, String token) async {
    final dynamic map = await _channel.invokeMethod("addingDownloadQueue", {
      'classID': classID,
      'userId': userId,
      'token': token,
    });
    final int code=int.tryParse(map["code"].toString());
    if(code==1 || code ==2){
      final model=FlutterLiveDownloadModel(
          fileName:safeToString(map["fileName"]),coverImageUrl:safeToString(map["coverImageUrl"]),
          progress:safeToInt(map["progress"]), size:safeToInt(map["size"]),itemIdentifier:safeToString(map["itemIdentifier"]),
          state:safeToInt(map["state"]),speed:safeToString(map["speed"]));
      return model;
    }
    return null;
  }

  //开始或暂停下载任务队列
  Future<Map> pauseDownloadQueue(
      String userId, String identifier, bool pause) async {
    final dynamic map = await _channel.invokeMethod("pauseDownloadQueue", {
      'identifier': identifier,
      'userId': userId,
      'pause': pause,
    });
    return map;
  }

  ///查询下载队列任务
  Future<List> queryDownloadQueue(String userId) async {
    final dynamic map = await _channel.invokeMethod("queryDownloadQueue", {
      'userId': userId,
    });
    return map["data"] as List;
  }

  ///查询下载队列任务
  Future notifyChange(MethodCall call) async {
    final dynamic map = await call.arguments;
    print("notifyChange===map:::${map.toString()}");
    streamController.add(null);
  }

  ///删除下载队列任务
  void removeDownloadQueue(String userId, String identifier) async {
    final dynamic map = await _channel.invokeMethod("removeDownloadQueue", {
      'identifier': identifier,
      'userId': userId,
    });
    return;
  }

  int safeToInt(dynamic field) {
    if (field == null) return 0;
    if (field is int) return field;
    int f = 0;
    try {
      if (field.toString().contains(".")) {
        f = double.parse(field.toString()).toInt();
      } else {
        f = int.parse(field.toString());
      }
    } catch (e) {}
    return f;
  }

  String safeToString(dynamic field) {
    if (field == null) return "";
    if (field is int) return field.toString();
    return field.toString();
  }

}
