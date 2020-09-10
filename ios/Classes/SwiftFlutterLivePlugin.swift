import Flutter
import UIKit

import BJLiveUI
import BJPlaybackUI
import BJVideoPlayerCore

public class SwiftFlutterLivePlugin: NSObject, FlutterPlugin,BJVRequestTokenDelegate,BJLDownloadManagerDelegate {
    var downloadManager: BJVDownloadManager?;
    var handler:DownloadHandler?;
    
    let channel: FlutterMethodChannel?;
    
    
    // 重写父类的构造函数
    init(channel: FlutterMethodChannel) {
        self.channel = channel
        super.init()
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "flutter_live", binaryMessenger: registrar.messenger())
        let instance = SwiftFlutterLivePlugin(channel: channel)
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    public func downloadManager(_ downloadManager: BJLDownloadManager, downloadItem: BJLDownloadItem,
                                didChange change: BJLPropertyChange<AnyObject>){
        self.handler?.downloadManager(self.channel, downloadManager, downloadItem: downloadItem, didChange: change)
    }
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        
        if (call.method == "register") {
            let dic = call.arguments as! Dictionary<String, Any>
            result(true)
        }else  if (call.method == "startLive") {
            
            let dic = call.arguments as! Dictionary<String, Any>
            
            //开启直播
            let name = dic["userName"] as! String
            let num = dic["userNum"] as! String
            let avatar = dic["userAvatar"] as! String
            let sign = dic["sign"] as! String
            let roomId = dic["roomId"] as! String
            
            startLive(name: name, num: num, avatar: avatar, sign: sign, roomId: roomId)
            
            result(true)
        } else if (call.method == "startBack") {
            
            let dic = call.arguments as! Dictionary<String, Any>
            
            //开启回放
            let roomId = dic["roomId"] as! String
            let token = dic["token"] as! String
            let sessionId = dic["sessionId"] as! String
            
            
            startBack(roomId: roomId, token: token, sessionId: sessionId)
            
            result(true)
        } else if (call.method == "startVideo") {
            
            let dic = call.arguments as! Dictionary<String, Any>
            
            //开启点播
            let videoId = dic["videoId"] as! String
            let token = dic["token"] as! String
            let userName = dic["userName"] as! String
            let userId = dic["userId"] as! String
            let title = dic["title"] as! String
            
            
            startVideo(videoId: videoId, token: token, userName: userName, userId: userId, title: title, result: result)
            
            
        } else if (call.method == "addingDownloadQueue") {
            let dic = call.arguments as! Dictionary<String, Any>
            //开启点播
            let classID = dic["classID"] as! String
            let userId = dic["userId"] as! String
            let token = dic["token"] as! String
            addingDownloadQueue(classID: classID, userId: userId,token:token, result: result)
        } else if (call.method == "pauseDownloadQueue") {
            let dic = call.arguments as! Dictionary<String, Any>
            //开启点播
            let identifier = dic["identifier"] as! String
            let userId = dic["userId"] as! String
            let pause = dic["pause"] as! Bool
            pauseDownloadQueue(identifier: identifier, userId: userId, pause: pause, result: result)
        } else if (call.method == "pauseAllDownloadQueue") {
            let dic = call.arguments as! Dictionary<String, Any>
            //开启点播
            let userId = dic["userId"] as! String
            let pause = dic["pause"] as! Bool
            pauseAllDownloadQueue(userId: userId, pause: pause, result: result)
        } else if (call.method == "queryDownloadQueue") {
            let dic = call.arguments as! Dictionary<String, Any>
            //开启点播
            let userId = dic["userId"] as! String
            queryDownloadQueue(userId: userId, result: result)
        } else if (call.method == "removeDownloadQueue") {
            let dic = call.arguments as! Dictionary<String, Any>
            //删除下载任务,并且删除已下载成功的文件
            let identifier = dic["identifier"] as! String
            let userId = dic["userId"] as! String
            removeDownloadQueue(userId: userId, identifier: identifier, result: result)
        }
        
    }
    
    
    public func startLive(name: String, num: String, avatar: String, sign: String, roomId: String) {
        
        
        let bjuser = BJLUser.init(number: num, name: name, groupID: 0, avatar: avatar, role: BJLUserRole.student)
        
        
        let bjlrc = BJLScRoomViewController.instance(withID: roomId, apiSign: sign, user: bjuser) as! BJLScRoomViewController
        
        
        let vc = UIApplication.shared.keyWindow?.rootViewController
        
        
        vc?.present(bjlrc, animated: true, completion: nil)
    }
    
    
    public func startBack(roomId: String, token: String, sessionId: String) {
        
        
        BJVideoPlayerCore.tokenDelegate = nil
        
        let bjpvc = BJPRoomViewController.onlinePlaybackRoom(withClassID: roomId, sessionID: sessionId, token: token) as! BJPRoomViewController
        
        let vc = UIApplication.shared.keyWindow?.rootViewController
        
        vc?.present(bjpvc, animated: true, completion: nil)
        
    }
    
    
    public func startVideo(videoId: String, token: String, userName: String, userId: String, title: String, result: @escaping FlutterResult) {
        
        
        BJVideoPlayerCore.tokenDelegate = nil
        
        let bjpvc = BJYDBViewController.init()
        bjpvc.token = token
        bjpvc.videoId = videoId
        bjpvc.bjtitle = title
        
        
        bjpvc.progress = { (current, duration) in
            
            result([
                "progress": current,
                "totalProgress": duration
            ])
        }
        
        
        let nvc = UINavigationController.init(rootViewController: bjpvc)
        
        
        let vc = UIApplication.shared.keyWindow?.rootViewController
        
        vc?.present(nvc, animated: true, completion: nil)
        
        
    }
 
    
    
    public func pauseAllDownloadQueue(userId: String, pause: Bool,
                                      result: @escaping FlutterResult
    ) {
        downloadManagerCheck(userId)
        let manager = downloadManager!
        let downloadItems = manager.downloadItems as [BJLDownloadItem]
        var list:Array<Dictionary<String,Any>> = [];
        for item in downloadItems {
            let progress = item.progress.completedUnitCount;
            let size = item.totalSize;
            let file: BJLDownloadFile? = item.downloadFiles?.first;
            let fileName: String = file?.fileName ?? "未知文件";
            let itemIdentifier = item.itemIdentifier;
            let path = file?.filePath;
            var dict: Dictionary<String, Any> = [:]
            dict["progress"] = progress
            dict["size"] = size
            dict["path"] = path
            dict["userId"] = userId
            dict["itemIdentifier"] = itemIdentifier
            dict["speed"] = "0K"
            dict["fileName"] = fileName
            if(item.state == BJLDownloadItemState.completed&&item.error==nil){
                dict["state"] = 1 //下载完成
                continue
            }else if((item.state==BJLDownloadItemState.paused&&item.error != nil)||(item.state == BJLDownloadItemState.completed&&item.error != nil)){  ///下载中出错,或者下载文件丢失
                if pause {
                    dict["state"] = 3
                } else {
                    item.resume()
                    dict["state"] = 0
                }
            }else  if(item.state != BJLDownloadItemState.invalid){
                if pause {
                    item.pause()
                    dict["state"] = 2
                } else {
                    item.resume()
                    dict["state"] = 0
                }
            }else if(item.state==BJLDownloadItemState.paused){
                if pause {
                    dict["state"] = 2
                     continue
                } else {
                    item.resume()
                    dict["state"] = 0
                }
            }else {
                if pause {
                    dict["state"] = 3
                     continue
                } else {
                    item.resume()
                    dict["state"] = 0
                }
            }
            
            list.append(dict);
        }
        if pause {
            result(["code": 1, "msg": "暂停成功", "data": list])
        } else {
            result(["code": 1, "msg": "恢复成功", "data": list])
        }
    }
    
    public func pauseDownloadQueue(
        identifier: String, userId: String, pause: Bool,
        result: @escaping FlutterResult
    ) {
        downloadManagerCheck(userId)
        let manager = downloadManager!
        let downloadItems = manager.downloadItems as [BJLDownloadItem]
        for item in downloadItems {
            if item.itemIdentifier == identifier {
                if pause {
                    item.pause()
                    result(["code": 1, "msg": "暂停成功"])
                } else {
                    item.resume()
                    result(["code": 1, "msg": "恢复成功"])
                }
                return
            }
        }
        if pause {
            result(["code": 0, "msg": "暂停失败"])
        } else {
            result(["code": 0, "msg": "恢复失败"])
        }
    }
    
    
    
    public func queryDownloadQueue(
        userId: String,
        result: @escaping FlutterResult
    ) {
        downloadManagerCheck(userId)
        let manager = downloadManager!
        let downloadItems = manager.downloadItems
        var arr: Array<Dictionary<String, Any>> = []
        for element in downloadItems {
            let progress = element.progress.completedUnitCount;
            let size = element.totalSize;
            let file: BJLDownloadFile? = element.downloadFiles?.first;
            let fileName: String = file?.fileName ?? "未知文件";
            let path = file?.filePath;
            let itemIdentifier = element.itemIdentifier;
            ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
            let state: Int = DownloadHandler.queryState(element);
           
            var dict: Dictionary<String, Any> = [:]
            
            dict["progress"] = progress
            dict["size"] = size
            dict["state"] = state
            dict["userId"] = userId
            dict["path"] = path
            dict["speed"] = DownloadHandler.getFileSizeString(size: Float.init(integerLiteral: element.bytesPerSecond))
            dict["itemIdentifier"] = itemIdentifier
            dict["fileName"] = fileName
            
            arr.append(dict)
        }
        result(arr) ///查询到的所有下载项
    }
    
    
    fileprivate func downloadManagerCheck(_ userId: String) {
        let identifier = "download_Identifier_\(userId)"
        if downloadManager == nil {
            downloadManager = BJVDownloadManager.init(identifier: identifier, inCaches: true);
            handler=DownloadHandler.init(userId: userId)
            downloadManager!.delegate = self
        } else if downloadManager!.identifier != identifier {
            let manager = downloadManager!
            let downloadItems = manager.downloadItems(withStatesArray: [NSNumber(value: BJLDownloadItemState.running.rawValue), NSNumber(value: NSNotFound)]) as! [BJLDownloadItem]
            
            for element in downloadItems {
                element.pause() ///关闭所有下载中的任务
            }
            downloadManager = BJVDownloadManager.init(identifier: identifier, inCaches: true);
            handler=DownloadHandler.init(userId: userId)
            downloadManager!.delegate = self
        }
    }
    
    
    public func removeDownloadQueue(userId: String, identifier: String,
                                    result: @escaping FlutterResult
    ) {
        downloadManagerCheck(userId)
        let manager = downloadManager!
        
        manager.removeDownloadItem(withIdentifier: identifier)
        result(["code": 1, "msg": "删除成功"])
    }
    
    public func addingDownloadQueue(
        classID: String,
        userId: String,
        token: String,
        result: @escaping FlutterResult
    ) {
        downloadManagerCheck(userId)
        let manager = downloadManager!
        var dict: Dictionary<String, Any> = [:]
        BJVideoPlayerCore.tokenDelegate = nil
        if manager.validateItem(withClassID: classID, sessionID: "0") { ///可以开始下载
            
            let item = manager.addDownloadItem(withClassID:classID,sessionID:"0" ,encrypted:true,preferredDefinitionList: nil ) { (item) in
                item.token = token;
            }
            ///下载结果
            if item == nil {
                dict["code"] = 0
                dict["msg"] = "下载失败"
            } else {
                dict["code"] = 1
                dict["msg"] = "开始下载"
                dict["userId"] = userId
                dict["size"] = item!.totalSize
                dict["path"] = item!.downloadFiles?.first?.filePath
                dict["state"] = 0
                dict["speed"] = "0K"
                dict["itemIdentifier"] = item!.itemIdentifier
                dict["fileName"] = item!.downloadFiles?.first?.fileName ?? "未知"
                item!.resume()
            }
        } else {///不能下载,可能是正在下载中了,或者已经下载完成
            dict["code"] = 2
            dict["msg"] = "文件已下载或正在下载中"
        }
        result(dict) ///下载结果
    }

     public func requestToken(withClassID classID: String, sessionID: String?, completion: @escaping (String?, Error?) -> Void) {

            print("requestToken")

            let key = "\(classID)-\(String(describing: sessionID))"

            completion(key, nil)

     }

     public func requestToken(withVideoID videoID: String, completion: @escaping (String?, Error?) -> Void) {
            completion(videoID, nil)
     }
    
}

class DownloadHandler: NSObject {
    var userId: String?
    init(userId:String) {
        super.init()
        self.userId = userId
    }
    
    
    public func downloadManager(_ channel: FlutterMethodChannel?,_ downloadManager: BJLDownloadManager, downloadItem: BJLDownloadItem, didChange change: BJLPropertyChange<AnyObject>) {
        let index = downloadManager.downloadItems.firstIndex(of: downloadItem)
        if (index == NSNotFound) {
            return;
        }
        let progress = downloadItem.progress.completedUnitCount;
        let size = downloadItem.totalSize;
        let file: BJLDownloadFile? = downloadItem.downloadFiles?.first;
        let fileName: String = file?.fileName ?? "未知文件";
        let itemIdentifier = downloadItem.itemIdentifier;
        let path = file?.filePath;
        ///0 是下载中,1是下载完成,2是下载暂停,3是下载失败
        print("rawValue:::::\(downloadItem.state.rawValue)")
        let state: Int = DownloadHandler.queryState(downloadItem);
        var dict: Dictionary<String, Any> = [:]
       
        dict["progress"] = progress
        dict["size"] = size
        dict["path"] = path
        dict["userId"] = self.userId!
        dict["itemIdentifier"] = itemIdentifier
//        _waitingFor_requestDownloadFiles
        if(state==3){
            let classID=downloadItem.value(forKey: "_classID") as? String
            let sessionID=downloadItem.value(forKey: "_sessionID") as? String
            
        }
        dict["state"] = state
        print("bytesPerSecond:\(downloadItem.bytesPerSecond)  downloadItem.state:\(downloadItem.state.rawValue)")
        dict["speed"] = DownloadHandler.getFileSizeString(size: Float.init(integerLiteral: downloadItem.bytesPerSecond))
        dict["fileName"] = fileName
        channel?.invokeMethod("notifyChange", arguments: dict)
    }
    
    fileprivate static func queryState(_ item: BJLDownloadItem) -> Int {
        if(item.state == BJLDownloadItemState.completed&&item.error==nil){
            return 1 //下载完成
        }else if((item.state==BJLDownloadItemState.paused&&item.error != nil)||(item.state == BJLDownloadItemState.completed&&item.error != nil)){  ///下载中出错,或者下载文件丢失
            return  3 ///下载失败
        }else  if(item.state != BJLDownloadItemState.invalid){
            return  0 ///下载中
        }else if(item.state==BJLDownloadItemState.paused){
            return  2 ///下载暂停
        }else {
            return  3 ///下载失败
        }
    }
    
    fileprivate static func getFileSizeString(size:Float) -> String{
        let sizeCopy = size
        if(sizeCopy <= 0){
            return "0K"
        }
        if(sizeCopy >= 1024*1024)//大于1M，则转化成M单位的字符串
        {
            return String.init(format: "%1.2fM", sizeCopy / 1024 / 1024)
        }
        else if(sizeCopy >= 1024 && sizeCopy<1024*1024) //不到1M,但是超过了1KB，则转化成KB单位
        {
            return String.init(format: "%1.2fK",sizeCopy / 1024)
        }
        else//剩下的都是小于1K的，则转化成B单位
        {
            return String.init(format: "%1.2fB",sizeCopy)
        }
    }
    
}



