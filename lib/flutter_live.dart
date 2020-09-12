import 'dart:async';
import 'dart:collection';
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

  StreamController<FlutterLiveDownloadModel> _streamController =
      StreamController.broadcast();


  Map<String,StreamSink<FlutterLiveDownloadModel>> _bindStreamSinks =new HashMap();

  FlutterLive._internal() {
    _createDatabase();
    _channel = const MethodChannel('flutter_live');

    _channel.setMethodCallHandler(_methodCallHandler);
  }

  Future<dynamic> _methodCallHandler(MethodCall call) async {
    if (call.method == "video_progress") {
    } else if (call.method == "notifyChange") {
      notifyChange(call);
    }
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
  void startPlayBackLocalActivity(String userId,String userName, String userNum,String identifier) {
    _channel.invokeMethod("startLocalBack", {
      'userId': userId,
      'userName': userName,
      'userNum': userNum,
      'identifier': identifier
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
    _streamController.stream.listen((event) {
      _bindStreamSinks.values.forEach((element) {
        element.add(event);
      });
    });
    return;
  }
  Future bindSink(StreamSink sink,String tag) async {
    if(!_bindStreamSinks.containsKey(tag)){
      _bindStreamSinks[tag]=sink;
    }
  }

  void dispose(String tag){
    if(_bindStreamSinks.containsKey(tag)){
      _bindStreamSinks.remove(tag);
    }
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
    print("_channel.invokeMethod(addingDownloadQueue)===${map.toString()}");
    final int code = int.tryParse(map["code"].toString());
    if (code == 1 || code == 2) {
      map["roomId"] = classID;
      map["userId"] = userId;
      map["token"] = token;
      map["className"] = className;
      map["courseName"] = courseName;
      map["classImage"] = coverImageUrl;
      if (code == 2) {
        // final model = await queryDownloadEntity(userId, identifier);
        // insertModel(model);
        // return model;
      } else {
        final model = parseModel(map);
        insertOrUpdateModel(model);
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
        insertOrUpdateModel(model);
        return true;
      } catch (e) {
        return false;
      }
    } else {
      return false;
    }
  }

  //开始或暂停下载任务队列
  Future<bool> pauseAllDownload(String userId, bool pause) async {
    final dynamic map = await _channel.invokeMethod("pauseAllDownloadQueue", {
      'userId': userId,
      'pause': pause,
    });
    final int code = int.tryParse(map["code"].toString());
    if (code == 1) {
      try {
        final stream =
            Stream.fromFutures((map["data"] as List).map((element) async {
          final identifier = element["itemIdentifier"];

          ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
          final model = await queryDownloadEntity(userId, identifier);
          if (model != null) {
            model.state = element["state"];
            insertOrUpdateModel(model);
          }
          return model;
        }).toList());
        await for (var s in stream) {
          print("model::${s.toString()}");
        }
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
        .where((value) => value != null)
        .toList();
    return list;
  }

  ///查询下载队列任务
  Future<List<FlutterLiveDownloadModel>> queryDownloadQueueForRoomIds(
      String userId, List<String> roomIds) async {
    final list = (await database.query("BJYDownload",
            where:
                "userId= ? and roomId IN (${roomIds.map((e) => "?").join(", ")})",
            whereArgs: [userId]..addAll(roomIds)))
        .map((map) {
          try {
            return parseModel(map);
          } catch (e) {
            return null;
          }
        })
        .where((value) => value != null)
        .toList();
    return list;
  }

  ///查询单个下载任务
  Future<FlutterLiveDownloadModel> queryDownloadEntity(
      String userId, String itemIdentifier) async {
    if (userId == null || itemIdentifier == null) return null;
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
        .where((value) => value != null)
        .toList()
        .last;
    return model;
  }

  ///查询下载队列任务
  Future notifyChange(MethodCall call) async {
    final dynamic map = await call.arguments;
    if (map is Map) {
      Map<String, dynamic> second = {};
      final keys = map.keys.toList()
        ..sort()
        ..reversed;
      keys.forEach((element) {
        second[element as String] = map[element as String];
      });
      print("notifyChange===map:::${second.toString()}");
      String userId = map["userId"];
      try {
        if (userId == null) return;
        final model = await queryDownloadEntity(userId, map["itemIdentifier"]);
        if (model != null) {
          final oldState=model.state;
          mergeModel(model, map);
          if(oldState==model.state&&oldState!=0)return;
          print("oldState:${oldState}  model.state:${model.state}  roomId:${model.roomId}");
          ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
          if (model.state == 3) {
            await Future.delayed(Duration(milliseconds: 400));
            bool exist= await updateIfExist(model);
            print("model:${model.toString()} updateIfExist:${exist}");
            if(exist){
              model.removeFlag=true;
            }
          }else{
            insertOrUpdateModel(model);
          }
          _streamController.sink.add(model);
          print("streamController.add(${model.toString()})");
        }
      } catch (e) {
        print("streamController.adde.toString()(${e.toString()})");
      }
    } else {}
  }

  FlutterLiveDownloadModel parseModel(Map map) {
    return FlutterLiveDownloadModel(
        roomId: safeToInt(map["roomId"]),
        fileName: safeToString(map["fileName"]),
        userId: safeToInt(map["userId"]),
        path: safeToString(map["path"]),
        className: safeToString(map["className"]),
        courseName: safeToString(map["courseName"]),
        coverImageUrl: safeToString(map["classImage"]),
        itemIdentifier: safeToString(map["itemIdentifier"]),
        progress: safeToInt(map["progress"]),
        token: safeToString(map["token"]),
        size: safeToInt(map["size"]),
        state: safeToInt(map["state"]),
        speed: safeToString(map["speed"]));
  }

  ///删除下载队列任务
  Future<void> removeDownloadQueue(String userId, String identifier) async {
    print("removeDownloadQueue 1");
    final dynamic map = await _channel.invokeMethod("removeDownloadQueue", {
      'identifier': identifier,
      'userId': userId,
    });
    print("removeDownloadQueue 2");
    await database.rawDelete("DELETE FROM  BJYDownload WHERE itemIdentifier = ? and userId = ?",[identifier, userId]);
    print("removeDownloadQueue 3");
    return;
  }

  Future<void> _createDatabase() async {
    Sqflite.devSetDebugModeOn(true);
    // Sqflite.setDebugModeOn(const bool.fromEnvironment("dart.vm.product"));
    // Get a location using getDatabasesPath
    var databasesPath = await getDatabasesPath();
    String path = join(databasesPath, 'fltbjydb.db');
    database = await openDatabase(path, version: 2,
        onCreate: (Database db, int version) async {
      // When creating the db, create the table
      await db.execute(
          "CREATE TABLE BJYDownload (roomId INTEGER PRIMARY KEY, itemIdentifier TEXT,userId TEXT, path TEXT, className TEXT,courseName TEXT, "
          "classImage TEXT, state INTEGER, size INTEGER, progress INTEGER,token TEXT)");
    });
  }

  Future<bool> updateIfExist(FlutterLiveDownloadModel model) async {
    await database.execute("INSERT OR REPLACE INTO BJYDownload(roomId, itemIdentifier,userId, path, className,courseName, classImage, state, size, progress,token) "
        " WHERE ("
        "select exists( select 1  from BJYDownload  where userId= ? and roomId = ? )"
        ")   VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        [ model.roomId,
          model.itemIdentifier,
          model.userId,
          model.path,
          model.className,
          model.courseName,
          model.coverImageUrl,
          model.state,
          model.size,
          model.progress,
          model.token,model.userId,model.roomId]);
    return Sqflite.firstIntValue(await database.rawQuery("select count(*)  from BJYDownload  where userId= ? and roomId = ?",[model.userId,model.roomId]))==1;
  }

  Future<void> insertOrUpdateModel(FlutterLiveDownloadModel model) async {
    // Insert some records in a transaction // Update some record
    await database.transaction((txn) async {
      int id2 = await txn.rawInsert(
          'INSERT OR REPLACE INTO BJYDownload(roomId, itemIdentifier,userId, path, className,courseName, classImage, state, size, progress,token) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)',
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
            model.progress,
            model.token
          ]);
      print('inserted2: $id2');
    });
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
  String token;
  int size;
  int state;
  String speed;

  bool removeFlag=false;

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
      this.token,
      this.size,
      this.state,
      this.speed});

  @override
  String toString() {
    return 'FlutterLiveDownloadModel{roomId: $roomId, fileName: $fileName, userId: $userId, path: $path, className: $className, courseName: $courseName, '
        'coverImageUrl: $coverImageUrl, itemIdentifier: $itemIdentifier,'
        ' progress: $progress,'
        ' token: $token,'
        ' size: $size, state: $state, speed: $speed}';
  }


}
