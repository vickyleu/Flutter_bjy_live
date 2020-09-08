import 'dart:async';

import 'package:flutter/services.dart';
import 'package:path/path.dart';
import 'package:sqflite/sqflite.dart';

/// Created  on 2019/10/12.
/// @author grey
/// Function :  直接跳转百家云平台

typedef OnVideoProgressCallback = Function(int, int);

class FlutterLive {
  Database database;

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

  factory FlutterLive() => _getInstance();

  static FlutterLive get instance => _getInstance();
  static FlutterLive _instance;

  static FlutterLive _getInstance() {
    if (_instance == null) {
      _instance = new FlutterLive._internal();
    }
    return _instance;
  }

  StreamController<FlutterLiveDownloadModel> streamController =
      StreamController.broadcast();

  FlutterLive._internal() {
    _createDatabase();
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
  void startPlayBackActivity(String roomId, String token, String sessionId,
      String userName, String userNum) {
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
    return;
  }

  // 添加下载任务队列
  Future<FlutterLiveDownloadModel> addingDownloadQueue(
      String classID,
      String userId,
      String token,
      String courseName,
      String className,
      String coverImageUrl) async {
    final dynamic map = await _channel.invokeMethod("addingDownloadQueue", {
      'classID': classID,
      'userId': userId,
      'token': token,
    });
    final int code = int.tryParse(map["code"].toString());
    if (code == 1 || code == 2) {
      map["roomId"] = classID;
      map["userId"] = userId;
      map["className"] = className;
      map["courseName"] = className;
      map["coverImageUrl"] = coverImageUrl;
      if (code == 2) {
        // final model = await queryDownloadEntity(userId, identifier);
        // insertModel(model);
        // return model;
      } else {
        final model = parseModel(map);
        insertModel(model);
        return model;
      }
    }
    return null;
  }

  //开始或暂停下载任务队列
  Future<bool> pauseDownloadQueue(
      String userId, String identifier, bool pause) async {
    final dynamic map = await _channel.invokeMethod("pauseDownloadQueue", {
      'identifier': identifier,
      'userId': userId,
      'pause': pause,
    });
    final int code = int.tryParse(map["code"].toString());
    if (code == 1) {
      try {
        ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
        final model = await queryDownloadEntity(userId, identifier);
        model.state = pause ? 2 : 0;
        updateModel(model);
        return true;
      } catch (e) {
        return false;
      }
    } else {
      return false;
    }
  }

  ///查询下载队列任务
  Future<List<FlutterLiveDownloadModel>> queryDownloadQueue(
      String userId) async {
    final list = (await database
            .rawQuery("SELECT * FROM BJYDownload where userId= ? ", [userId]))
        .map((map) {
          try {
            return parseModel(map);
          } catch (e) {
            return null;
          }
        })
        .skipWhile((value) => value == null)
        .toList();
    return list;
  }

  ///查询下载队列任务
  Future<List<FlutterLiveDownloadModel>> queryDownloadQueueForRoomIds(
      String userId, List<String> roomIds) async {
    final list = (await database.rawQuery(
            "SELECT * FROM BJYDownload where userId= ? and roomId in ?",
            [userId, "( ${roomIds.join(",").toString()} )"]))
        .map((map) {
          try {
            return parseModel(map);
          } catch (e) {
            return null;
          }
        })
        .skipWhile((value) => value == null)
        .toList();
    return list;
  }

  ///查询单个下载任务
  Future<FlutterLiveDownloadModel> queryDownloadEntity(
      String userId, String itemIdentifier) async {
    final model = (await database.rawQuery(
            "SELECT * FROM BJYDownload where userId= ? and  itemIdentifier= ? ",
            [userId, itemIdentifier]))
        .map((map) {
          try {
            return parseModel(map);
          } catch (e) {
            return null;
          }
        })
        .skipWhile((value) => value == null)
        .toList()
        .last;
    return model;
  }

  ///查询下载队列任务
  Future notifyChange(MethodCall call) async {
    final dynamic map = await call.arguments;
    print("notifyChange===map:::${map.toString()}");
    final model =
        await queryDownloadEntity(map["userId"], map["itemIdentifier"]);
    mergeModel(model, map);
    updateModel(model);
    streamController.add(model);
  }

  FlutterLiveDownloadModel parseModel(Map map) {
    return FlutterLiveDownloadModel(
        roomId: safeToInt(map["roomId"]),
        fileName: safeToString(map["fileName"]),
        userId: safeToInt(map["userId"]),
        path: safeToString(map["path"]),
        className: safeToString(map["className"]),
        courseName: safeToString(map["courseName"]),
        coverImageUrl: safeToString(map["coverImageUrl"]),
        itemIdentifier: safeToString(map["itemIdentifier"]),
        progress: safeToInt(map["progress"]),
        size: safeToInt(map["size"]),
        state: safeToInt(map["state"]),
        speed: safeToString(map["speed"]));
  }

  ///删除下载队列任务
  void removeDownloadQueue(String userId, String identifier) async {
    final dynamic map = await _channel.invokeMethod("removeDownloadQueue", {
      'identifier': identifier,
      'userId': userId,
    });
// Delete a record
    await database.rawDelete(
        'DELETE FROM BJYDownload WHERE identifier = ? and userId = ?',
        [identifier, userId]);
    return;
  }

  Future<void> _createDatabase() async {
    // Get a location using getDatabasesPath
    var databasesPath = await getDatabasesPath();
    String path = join(databasesPath, 'fltbjydb.db');
    database = await openDatabase(path, version: 1,
        onCreate: (Database db, int version) async {
      // When creating the db, create the table
      await db.execute(
          "CREATE TABLE BJYDownload (roomId INTEGER PRIMARY KEY, itemIdentifier TEXT,userId TEXT, path TEXT, className TEXT,courseName TEXT, "
          "classImage TEXT, state INTEGER, size INTEGER, progress INTEGER)");
    });
  }

  Future<void> insertModel(FlutterLiveDownloadModel model) async {
    // Insert some records in a transaction
    await database.transaction((txn) async {
      int id2 = await txn.rawInsert(
          'INSERT INTO BJYDownload(roomId, itemIdentifier,userId, path, className,courseName, classImage, state, size, progress) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
          [
            model.roomId,
            model.itemIdentifier,
            model.userId,
            model.path,
            model.className,
            model.courseName,
            model.coverImageUrl,
            model.state,
            model.size,
            model.progress
          ]);
      print('inserted2: $id2');
    });
  }

  Future<void> updateModel(FlutterLiveDownloadModel model) async {
    // Update some record
    await database.rawUpdate(
        "UPDATE  SET BJYDownload(path, state, size, progress) VALUES(?, ?, ?, ?, ?, ?)  WHERE roomId = ? and userId = ?",
        [
          model.path,
          model.state,
          model.size,
          model.progress,
          model.roomId,
          model.userId
        ]);
  }

  ///查询下载总缓存
  Future<int> queryDownloadTotal(String userId) async {
// Count the records
    return Sqflite.firstIntValue(await database
        .rawQuery("SELECT COUNT(*) FROM BJYDownload where userId= '$userId' "));
  }

  void mergeModel(FlutterLiveDownloadModel model, Map map) {
    model.path = safeToString(map["path"]);
    model.progress = safeToInt(map["progress"]);
    model.size = safeToInt(map["size"]);
    model.state = safeToInt(map["state"]);
    model.speed = safeToString(map["speed"]);
  }
}

class FlutterLiveDownloadModel {
  int roomId;
  String fileName;
  int userId;
  String path;
  String className;
  String courseName;
  String coverImageUrl;
  String itemIdentifier;
  int progress;
  int size;
  int state;
  String speed;

  FlutterLiveDownloadModel(
      {this.roomId,
      this.fileName,
      this.userId,
      this.path,
      this.className,
      this.courseName,
      this.coverImageUrl,
      this.itemIdentifier,
      this.progress,
      this.size,
      this.state,
      this.speed});
}
