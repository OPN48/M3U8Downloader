<center><iframe height=480 width=270 src="art/m3u8Downloader_2.mp4" allowfullscreen></iframe></center>
> 前段时间由于业务需要，需要做一个视频下载的功能，包括m3u8视频和mp4视频等，于是在Github上找了几个相关的下载库，发现要不是太久没有更新了，要不就是不太符合我们的需求，所以干脆就手撸了一个`M3U8Downloader`

`Github`地址：[https://github.com/xuqingquan1995/M3U8Downloader](https://github.com/xuqingquan1995/M3U8Downloader)

`Gitee`地址：[https://gitee.com/xuqingquan/M3U8Downloader](https://gitee.com/xuqingquan/M3U8Downloader)

## M3U8文件结构
开始撸代码之前，先预备一下相关知识，M3U8视频其实主要就一个文件，文件里面写明了视频片段ts的地址，我们获得这个m3u8文件就可以通过文件内的内容，分析出世纪的ts，然后下载相对应的ts文件，就可以做到下载m3u8视频了
### 最直接的m3u8文件
> [https://135zyv5.xw0371.com/2018/10/29/X05c7CG3VB91gi1M/playlist.m3u8](https://135zyv5.xw0371.com/2018/10/29/X05c7CG3VB91gi1M/playlist.m3u8)
这个链接的m3u8文件下载后内容如下
```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-MEDIA-SEQUENCE:0
#EXT-X-ALLOW-CACHE:YES
#EXT-X-TARGETDURATION:19
#EXTINF:12.640000,
out000.ts
#EXTINF:7.960000,
out001.ts
#EXTINF:12.280000,
out002.ts
#EXTINF:7.520000,
out003.ts
#EXTINF:10.240000,
out004.ts
#EXTINF:15.520000,
out005.ts
#EXTINF:8.600000,
out006.ts
#EXTINF:7.440000,
out007.ts
#EXTINF:8.240000,
out008.ts
#EXTINF:10.000000,
out009.ts
#EXTINF:13.120000,
out010.ts
。。。。。。。
```
可以很直观的看出，其实这个文件里面是一系列的ts文件
### 需要重定向的m3u8
还有例如以下这两个链接的m3u8文件下载后内容如下,只有简单的一行
>[http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8](http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8)
```
#EXTM3U
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=800000,RESOLUTION=1080x608
1000k/hls/index.m3u8
```
>[https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8](https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8)
```
#EXTM3U
#EXT-X-STREAM-INF:PROGRAM-ID=1,BANDWIDTH=1000000,RESOLUTION=1280x720
/ppvod/1F94756C565EC42C5735D57272032622.m3u8
```
对于这一类的m3u8文件，其实是需要重定向的，重定向后可以获得真实的m3u8地址，从而获取到对应的ts地址

根据url规则，以上两个m3u8的实际地址为：

[http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8](http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/index.m3u8) 转为：[http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/1000k/hls/index.m3u8](http://youku.cdn-iqiyi.com/20180523/11112_b1fb9d8b/1000k/hls/index.m3u8)

[https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8](https://v8.yongjiu8.com/20180321/V8I5Tg8p/index.m3u8) 转为：[https://v8.yongjiu8.com/ppvod/1F94756C565EC42C5735D57272032622.m3u8](https://v8.yongjiu8.com/ppvod/1F94756C565EC42C5735D57272032622.m3u8)

### ts文件分析
对于获取到的ts文件主要有以下几种类型：

* 只有文件名
```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:9
#EXT-X-MEDIA-SEQUENCE:0
#EXTINF:4.276000,
65f7a658c87000.ts
#EXTINF:4.170000,
65f7a658c87001.ts
#EXTINF:5.754600,
65f7a658c87002.ts
#EXTINF:4.170000,
65f7a658c87003.ts
#EXTINF:4.170000,
```
* 带有路径的
```
#EXTM3U
#EXT-X-VERSION:3
#EXT-X-TARGETDURATION:10
#EXT-X-MEDIA-SEQUENCE:0
#EXTINF:10,
/20180321/V8I5Tg8p/1000kb/hls/bdmhnU1119000.ts
#EXTINF:10,
/20180321/V8I5Tg8p/1000kb/hls/bdmhnU1119001.ts
#EXTINF:10,
/20180321/V8I5Tg8p/1000kb/hls/bdmhnU1119002.ts
#EXTINF:10,
/20180321/V8I5Tg8p/1000kb/hls/bdmhnU1119003.ts
#EXTINF:7.8,
/20180321/V8I5Tg8p/1000kb/hls/bdmhnU1119004.ts
```
其实也是根据url规则进行替换，对于只有文件名的ts文件，只要把它对应的m3u8地址最后的文件名替换成ts文件名就行了，对于带有路径的，根据url规则，如果以/开头的，则代表是在域名根目录下的，不是/开头的，则代表是在当前目录下的，进行相应替换就可以得到ts文件的url地址了

## 技术选型
既然是下载，免不了的是涉及到网络请求的实现，其实就是具体的下载怎么去做，在`Github`上有找到一个[okdownload](https://github.com/lingochamp/okdownload)这个库，之所以选择它，一方面是他是下载库star最多的[FileDownloader](https://github.com/lingochamp/FileDownloader)的升级版,另一方面是它的批下载功能符合我下载m3u8这样多个ts文件的场景

## 代码实现
### 数据类型准备
`VideoDownloadEntity`主要是存储过程中的数据，并且方便之后操作的
```kotlin
const val NO_START = 0
const val PREPARE = 1
const val DOWNLOADING = 2
const val PAUSE = 3
const val COMPLETE = 4
const val ERROR = 5
const val DELETE = -1

class VideoDownloadEntity(
    var originalUrl: String,//原始下载链接
    var name: String = "",//视频名称
    var subName: String = "",//视频子名称
    var redirectUrl: String = "",//重定向后的下载链接
    var fileSize: Long = 0,//文件总大小
    var currentSize: Long = 0,//当前已下载大小
    var currentProgress: Double = 0.0,//当前进度
    var currentSpeed: String = "",//当前速率
    var tsSize: Int = 0,//ts的数量
    var createTime: Long = System.currentTimeMillis()//创建时间
) : Parcelable, Comparable<VideoDownloadEntity> {

    //状态
    var status: Int = NO_START
        set(value) {
            if (field != DELETE) {
                field = value
            }
            if (value == DELETE) {
                startDownload = null
                downloadContext?.stop()
                downloadTask?.cancel()
            }
        }

    var downloadContext: DownloadContext? = null
    var downloadTask: DownloadTask? = null
    var startDownload: (() -> Unit)? = null

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readLong(),
        parcel.readLong(),
        parcel.readDouble(),
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readLong()
    ) {
        this.status = parcel.readInt()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(originalUrl)
        parcel.writeString(name)
        parcel.writeString(subName)
        parcel.writeString(redirectUrl)
        parcel.writeLong(fileSize)
        parcel.writeLong(currentSize)
        parcel.writeDouble(currentProgress)
        parcel.writeString(currentSpeed)
        parcel.writeInt(tsSize)
        parcel.writeLong(createTime)
        parcel.writeInt(status)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoDownloadEntity> {
        override fun createFromParcel(parcel: Parcel): VideoDownloadEntity {
            return VideoDownloadEntity(parcel)
        }

        override fun newArray(size: Int): Array<VideoDownloadEntity?> {
            return arrayOfNulls(size)
        }
    }

    override fun toString(): String {
        val json = JSONObject()
        json.put("originalUrl", originalUrl)
        json.put("name", name)
        json.put("subName", subName)
        json.put("redirectUrl", redirectUrl)
        json.put("fileSize", fileSize)
        json.put("currentSize", currentSize)
        json.put("currentProgress", currentProgress)
        json.put("currentSpeed", currentSpeed)
        json.put("tsSize", tsSize)
        json.put("createTime", createTime)
        json.put("status", status)
        return json.toString()
    }

    fun toFile() {
        val path = FileDownloader.getDownloadPath(originalUrl)
        val config = File(path, "video.config")
        if (!config.exists() && this.createTime == 0L) {
            this.createTime = System.currentTimeMillis()
        }
        config.writeText(toString())
    }

    override fun compareTo(other: VideoDownloadEntity) =
        (other.createTime - this.createTime).toInt()
}

fun parseJsonToVideoDownloadEntity(jsonString: String): VideoDownloadEntity? {
    if (jsonString.isEmpty()) {
        return null
    }
    return try {
        val json = JSONObject(jsonString)
        val entity = VideoDownloadEntity(
            json.getString("originalUrl"),
            json.getString("name"),
            json.getString("subName"),
            json.getString("redirectUrl"),
            json.getLong("fileSize"),
            json.getLong("currentSize"),
            json.getDouble("currentProgress"),
            json.getString("currentSpeed"),
            json.getInt("tsSize"),
            json.getLong("createTime")
        )
        entity.status = json.getInt("status")
        entity
    } catch (t: Throwable) {
        t.printStackTrace()
        null
    }
}
```
### 获取真实ts路径
下载m3u8文件，最开始是获取到真实的ts文件，那么先创建一个`M3U8ConfigDownloader`进行配置文件的获取
```kotlin
internal object M3U8ConfigDownloader {

    private val downloadList = arrayListOf<String>()
    private val TAG = "M3U8ConfigDownloader"

    //清楚所有任务，
    fun clear() {
        downloadList.clear()
    }

    /**
     * @return 如果返回空则不需要下载，如果返回的文件存在了，则开始下载，否则等待下载完成
     */
    fun start(entity: VideoDownloadEntity): File? {
        if (entity.status == DELETE) {
            return null
        }
        if (downloadList.contains(entity.originalUrl)) {
            return null
        }
        if (entity.createTime == 0L) {
            entity.createTime = System.currentTimeMillis()
        }
        entity.redirectUrl = ""
        val path = FileDownloader.getDownloadPath(entity.originalUrl)
        val config = FileDownloader.getConfigFile(entity.originalUrl)
        val realEntity = if (!config.exists()) {
            entity.toFile()
            entity
        } else {
            parseJsonToVideoDownloadEntity(config.readText()) ?: entity
        }
        if (entity.status == DELETE) {
            path.deleteRecursively()
            return null
        }
        val m3u8ListFile = File(path, "m3u8.list")
        return if (realEntity.status != COMPLETE) {//没有完成的才有必要下载
            Log.d(TAG, "init")
            if (m3u8ListFile.exists()) {
                Log.d(TAG, "从文件下载")
            } else {
                Log.d(TAG, "从0开始下载")
                realEntity.status = PREPARE
                FileDownloader.downloadCallback.postValue(realEntity)
                entity.toFile()
                //进入下载m3u8
                downloadM3U8File(path, realEntity)
            }
            m3u8ListFile
        } else {
            null
        }
    }


    /**
     * 下载单个文件
     */
    private fun downloadM3U8File(path: File, entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        val fileName: String
        val url = if (entity.redirectUrl.isNotEmpty()) {//如果有了重定向的url
            fileName = "real.m3u8"
            entity.redirectUrl
        } else {//否则就用初始的url
            fileName = "original.m3u8"
            entity.originalUrl
        }
        Log.d(TAG, "downloadM3U8File-url=$url,fileName=$fileName")
        val downloadFile = File(path, fileName)
        DownloadTask.Builder(url, downloadFile.parentFile)
            .setFilename(downloadFile.name)
            .build()
            .enqueue(object : DownloadListener1() {
                override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                    if (entity.downloadTask == null) {
                        entity.downloadTask = task
                    }
                    Log.d(TAG, "taskStart-->")
                    downloadList.add(task.url)
                }

                override fun taskEnd(
                    task: DownloadTask, cause: EndCause, realCause: Exception?,
                    model: Listener1Assist.Listener1Model
                ) {
                    if (entity.downloadTask == null) {
                        entity.downloadTask = task
                    }
                    Log.d(TAG, "taskEnd-->${cause.name},${realCause?.message}")
                    if (cause == EndCause.COMPLETED) {
                        getFileContent(path, entity)
                    } else {
                        entity.status = ERROR
                        downloadList.remove(entity.originalUrl)
                        entity.startDownload = {
                            start(entity)
                        }
                        entity.toFile()
                        FileDownloader.downloadCallback.postValue(entity)
                    }
                }

                override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                    if (entity.downloadTask == null) {
                        entity.downloadTask = task
                    }
                }

                override fun connected(
                    task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
                ) {
                    if (entity.downloadTask == null) {
                        entity.downloadTask = task
                    }
                    Log.d(TAG, "connected-->")
                }

                override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                    if (entity.downloadTask == null) {
                        entity.downloadTask = task
                    }
                }
            })
    }

    /**
     * 分析文件内容
     */
    private fun getFileContent(path: File, entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        Log.d(TAG, "getFileContent---$entity")
        val url = if (entity.redirectUrl.isNotEmpty()) {//如果有了重定向的url
            entity.redirectUrl
        } else {//否则就用初始的url
            entity.originalUrl
        }
        val uri = Uri.parse(url)
        val realM3U8File = File(path, "real.m3u8")
        var file = realM3U8File
        if (!file.exists()) {//直接判断真实的m3u8文件是否存在，存在则读取
            file = File(path, "original.m3u8")
        }
        Log.d(TAG, "getFileContent---${file.name}")
        val list = file.readLines().filter { !it.startsWith("#") }//读取m3u8文件
        if (list.size > 1) {//直接的m3u8的ts链接
            entity.tsSize = list.size
            entity.toFile()
            if (file != realM3U8File) {
                file.copyTo(realM3U8File)
            }
            val m3u8ListFile = File(path, "m3u8.list")
            list.forEach {
                val ts = if (!it.startsWith("/")) {
                    url.substring(0, url.lastIndexOf("/") + 1) + it
                } else {
                    "${uri.scheme}://${uri.host}$it"
                }
                m3u8ListFile.appendText("$ts\n")
            }
            val localPlaylist = File(path, "localPlaylist.m3u8")
            file.readLines().forEach {
                var str = it
                if (!str.startsWith("#")) {
                    str = if (str.contains("/")) {
                        ".ts${it.substring(it.lastIndexOf("/"))}"
                    } else {
                        ".ts/$it"
                    }
                }
                localPlaylist.appendText("$str\n")
            }
            Log.d(TAG, "start--->$entity")
        } else {//重定向
            val newUrl = list[0]
            entity.redirectUrl = if (newUrl.startsWith("/")) {
                "${uri.scheme}://${uri.host}$newUrl"
            } else {
                url.substring(0, url.lastIndexOf("/") + 1) + newUrl
            }
            entity.toFile()
            downloadM3U8File(path, entity)
        }
    }

}
```
在以上代码中，从一个最初始的url开始，下载对应的m3u8文件，分析如果这个m3u8是最终的ts流，将ts流的完整url写入`m3u8.list`这个文件，之后下载的都从这个文件进行下,如果这个m3u8需要重定向，那么就重组链接，再一次下载，以此循环得到最终的ts流，同时，在获取到最终ts流到时候，会构造一个本地可以播放到m3u8文件`localPlaylist.m3u8`，当视频下载完成之后就可以通过这个文件打开本地的播放器进行播放

### 下载ts文件
之前已经获取到真实的ts路径了，并且将这些路径保存在`m3u8.list`文件里面了，所以之后就是通过这个文件里面的路径，使用`okdownload`进行批量下载了，具体实现如下
```kotlin
internal object M3U8Downloader {
    private val downloadList = arrayListOf<String>()
    private const val TAG = "---M3U8Downloader---"

    //清楚所有任务
    fun clear() {
        downloadList.clear()
    }

    //批下载
    fun bunchDownload(path: File) {
        val config = FileDownloader.getConfigFile(path)
        Log.d(TAG, "config==>${config.readText()}")
        val entity = parseJsonToVideoDownloadEntity(config.readText())
        if (entity == null) {//获取到的实体类为空的忽略
            Log.d(TAG, "entity==null${config.readText()}")
            return
        }
        //如果状态是删除的就忽略
        if (entity.status == DELETE) {
            path.deleteRecursively()
            return
        }
        //避免重复进入下载
        if (downloadList.contains(entity.originalUrl)) {
            Log.d(TAG, "contains")
            return
        }
        var lastCallback = 0L
        val CURRENT_PROGRESS = entity.originalUrl.hashCode()
        val speedCalculator = SpeedCalculator()
        val listener = object : DownloadListener1() {
            override fun taskStart(
                task: DownloadTask, model: Listener1Assist.Listener1Model
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
            }

            override fun taskEnd(
                task: DownloadTask, cause: EndCause, realCause: Exception?,
                model: Listener1Assist.Listener1Model
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
            }

            override fun progress(
                task: DownloadTask, currentOffset: Long, totalLength: Long
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                val preOffset = (task.getTag(CURRENT_PROGRESS) as Long?) ?: 0
                speedCalculator.downloading(currentOffset - preOffset)
                val now = System.currentTimeMillis()
                if (now - lastCallback > 1000) {
                    entity.currentSpeed = speedCalculator.speed() ?: ""
                    entity.status = DOWNLOADING
                    entity.toFile()
                    FileDownloader.downloadCallback.postValue(entity)
                    lastCallback = now
                }
                task.addTag(CURRENT_PROGRESS, currentOffset)
            }

            override fun connected(
                task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
            }
        }

        Log.d(TAG, "bunchDownload")
        val m3u8ListFile = File(path, "m3u8.list")
        var urls = m3u8ListFile.readLines()
        var times = 5
        while (times > 0 && urls.size != entity.tsSize) {//如果还有重试机会且ts数量还不完全对的话,等待100ms
            urls = m3u8ListFile.readLines()
            times--
            Thread.sleep(100)
        }
        val tsDirectory = File(path, ".ts")
        if (!tsDirectory.exists()) {
            tsDirectory.mkdir()
        }
        val builder = DownloadContext.QueueSet()
            .setParentPathFile(tsDirectory)
            .setMinIntervalMillisCallbackProcess(1000)
            .setPassIfAlreadyCompleted(true)
            .commit()
        Log.d(TAG, "ts.size===>${urls.size}")
        urls.forEachIndexed { index, url ->
            builder.bind(url).addTag(1, index)
        }
        val downloadContext = builder.setListener(object : DownloadContextListener {
            override fun taskEnd(
                context: DownloadContext, task: DownloadTask, cause: EndCause,
                realCause: Exception?, remainCount: Int
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                if (entity.downloadContext == null) {
                    entity.downloadContext = context
                }
                if (context.isStarted && cause == EndCause.COMPLETED) {
                    val progress = 1 - (remainCount * 1.0) / urls.size
                    entity.status = DOWNLOADING
                    entity.currentProgress = progress
                    entity.fileSize += task.file?.length() ?: 0
                    entity.currentSize += task.file?.length() ?: 0
                    val now = System.currentTimeMillis()
                    if (now - lastCallback > 1000) {
                        FileDownloader.downloadCallback.postValue(entity)
                        lastCallback = now
                    }
                    entity.toFile()
                }
            }

            override fun queueEnd(context: DownloadContext) {
                Log.d(TAG, "queueEnd")
                if (entity.downloadContext == null) {
                    entity.downloadContext = context
                }
                when (entity.currentProgress) {
                    1.0 -> entity.status = COMPLETE
                    0.0 -> entity.status = ERROR
                    else -> entity.status = PAUSE
                }
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
                FileDownloader.subUseProgress(entity.originalUrl)//已使用的线程数减少
            }
        }).build()
        entity.downloadContext = downloadContext
        entity.startDownload = { downloadContext.startOnSerial(listener) }
        downloadContext.startOnSerial(listener)
        FileDownloader.addUseProgress(entity.originalUrl)//已使用的线程数增加
        downloadList.add(entity.originalUrl)
    }
}
```
通过以上代码就可以进行批量下载的实现了

## MP4下载
既然对于复杂的m3u8都能下载,那么单个文件的mp4之类的肯定要支持下载的，以下为mp4的下载方案
```kotlin
internal object SingleVideoDownloader {
    private val downloadList = arrayListOf<String>()
    private const val TAG = "SingleVideoDownloader"

    //清理所有任务
    fun clear() {
        downloadList.clear()
    }

    //下载任务的初始化
    fun initConfig(entity: VideoDownloadEntity): File {
        val config = FileDownloader.getConfigFile(entity.originalUrl)
        if (!config.exists()) {
            if (entity.createTime == 0L) {
                entity.createTime = System.currentTimeMillis()
            }
            entity.status = PREPARE
            entity.fileSize = 0
            entity.currentSize = 0
            entity.toFile()
            Log.d(TAG, "config==>${config.readText()}")
            FileDownloader.downloadCallback.postValue(entity)
        }
        return config
    }

    //下载任务的入口
    fun fileDownloader(entity: VideoDownloadEntity) {
        val path = FileDownloader.getDownloadPath(entity.originalUrl)
        if (entity.status == DELETE) {//如果是删除状态的则忽略
            path.deleteRecursively()
            return
        }
        if (downloadList.contains(entity.originalUrl)) {//避免重复下载
            Log.d(TAG, "contains---${entity.originalUrl},${entity.name}")
            return
        }
        entity.status = PREPARE
        entity.fileSize = 0
        entity.currentSize = 0
        FileDownloader.downloadCallback.postValue(entity)
        var lastCallback = 0L
        val CURRENT_PROGRESS = entity.originalUrl.hashCode()
        val speedCalculator = SpeedCalculator()

        Log.d(TAG, "fileDownloader")

        val fileName = if (entity.name.isNotEmpty()) {//主标题有
            if (entity.subName.isNotEmpty()) {//副标题也有
                "${entity.name}-${entity.subName}.mp4"
            } else {//只有主标题
                "${entity.name}.mp4"
            }
        } else {//没有主标题
            if (entity.subName.isNotEmpty()) {//只有副标题
                "${entity.subName}.mp4"
            } else {//标题都没有
                "index.mp4"
            }
        }
        val downloadFile = File(path, fileName)
        Log.d(TAG, "downloadFile===>${downloadFile.absolutePath}")
        val task = DownloadTask.Builder(entity.originalUrl, downloadFile.parentFile)
            .setFilename(downloadFile.name)
            .setPassIfAlreadyCompleted(true)
            .setMinIntervalMillisCallbackProcess(1000)
            .setConnectionCount(3)
            .build()
        task.enqueue(object : DownloadListener1() {
            override fun taskStart(task: DownloadTask, model: Listener1Assist.Listener1Model) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                Log.d(TAG, "taskStart-->")
                entity.status = PREPARE
                entity.fileSize = 0
                entity.currentSize = 0
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
            }

            override fun taskEnd(
                task: DownloadTask, cause: EndCause, realCause: Exception?,
                model: Listener1Assist.Listener1Model
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                Log.d(TAG, "taskEnd-->${cause.name},${realCause?.message}")
                when (cause) {
                    EndCause.COMPLETED -> entity.status = COMPLETE
                    EndCause.CANCELED -> {
                        entity.status = PAUSE
                        entity.startDownload = {
                            fileDownloader(entity)
                        }
                    }
                    else -> {
                        entity.status = ERROR
                        entity.startDownload = {
                            fileDownloader(entity)
                        }
                    }
                }
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
                downloadList.remove(entity.originalUrl)
                FileDownloader.subUseProgress(task.url)//已使用的线程数减少
            }

            override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                val preOffset = (task.getTag(CURRENT_PROGRESS) as Long?) ?: 0
                speedCalculator.downloading(currentOffset - preOffset)
                entity.currentSize = currentOffset
                val now = System.currentTimeMillis()
                if (now - lastCallback > 1000) {
                    entity.currentProgress = (currentOffset * 1.0) / (totalLength * 1.0)
                    entity.currentSpeed = speedCalculator.speed() ?: ""
                    entity.status = DOWNLOADING
                    entity.toFile()
                    FileDownloader.downloadCallback.postValue(entity)
                    lastCallback = now
                }
                task.addTag(CURRENT_PROGRESS, currentOffset)
            }

            override fun connected(
                task: DownloadTask, blockCount: Int, currentOffset: Long, totalLength: Long
            ) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
                entity.currentSize += currentOffset
                entity.fileSize += totalLength
                entity.toFile()
                FileDownloader.downloadCallback.postValue(entity)
            }

            override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
                if (entity.downloadTask == null) {
                    entity.downloadTask = task
                }
            }
        })
        entity.downloadTask = task
        downloadList.add(entity.originalUrl)
        FileDownloader.addUseProgress(entity.originalUrl)//已使用的线程数增加
    }
}
```
## 多任务管理
以上代码出现了不少的`FileDownloader`这个类，这个类的主要作用是进行多任务的管理，实现顺序任务下载，限制同时下载数量等功能，具体代码如下：
```kotlin
object FileDownloader {

    private val TAG = "FileDownloader"

    val downloadCallback = MutableLiveData<VideoDownloadEntity>()//下载进度回调

    private var MAX_PROGRESS = -1
        //最终计算结果至少为1
        get() {
            if (field == -1) {
                field = Runtime.getRuntime().availableProcessors() / 2//可用线程数的一半
                if (Build.VERSION.SDK_INT < 23) {//如果小于Android6的，可用线程数再减2
                    field -= 2
                }
            }
            if (field > 5) {//最多只能有5个并行
                field = 5
            }
            if (field <= 0) {//最少也要有1个任务
                field = 1
            }
            return field
        }
    private var useProgress = 0
        //已使用的线程数,始终大于0
        set(value) {
            if (value >= 0) {
                field = value
            }
        }
    private var downloadingList = arrayListOf<String>()//下载中的列表，为统计线程使用
    private var waitDownloadList = arrayListOf<String>()//等待下载的url列表
    private val downloadList = arrayListOf<VideoDownloadEntity>()//排队列表
    private val waitList = arrayListOf<VideoDownloadEntity>()//等待下载的队列
    private var wait = false//m3u8等待状态

    /**
     * 停止全部任务
     */
    fun clearAllDownload() {
        OkDownload.with().downloadDispatcher().cancelAll()
        downloadingList.clear()
        waitDownloadList.clear()
        downloadList.clear()
        waitList.clear()
        M3U8ConfigDownloader.clear()
        M3U8Downloader.clear()
        SingleVideoDownloader.clear()
        MAX_PROGRESS = -1
        useProgress = 0
    }

    /**
     * 减少已使用线程数
     */
    fun subUseProgress(url: String) {
        if (downloadingList.contains(url)) {
            useProgress--
            downloadingList.remove(url)
            Log.d(TAG, "释放线程---$useProgress")
            if (downloadList.isNotEmpty()) {
                Log.d(TAG, "subUseProgress---新增任务")
                waitDownloadList.removeAt(0)
                downloadVideo(downloadList.removeAt(0))
            }
        }
    }

    /**
     * 增加使用线程数
     */
    fun addUseProgress(url: String) {
        if (!downloadingList.contains(url)) {
            useProgress++
            downloadingList.add(url)
        }
    }

    /**
     * 获取最顶层的下载目录
     */
    @JvmStatic
    fun getBaseDownloadPath(): File {
        val file = File(Environment.getExternalStorageDirectory(), "m3u8Downloader")
        if (!file.exists()) {
            file.mkdirs()
        }
        return file
    }

    /**
     * 获取根据链接得到的下载存储路径
     */
    @JvmStatic
    fun getDownloadPath(url: String): File {
        val file = File(getBaseDownloadPath(), md5(url))
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    /**
     * 获取相关配置文件
     */
    @JvmStatic
    fun getConfigFile(url: String): File {
        val path = getDownloadPath(url)
        return File(path, "video.config")
    }

    /**
     * 获取相关配置文件
     */
    @JvmStatic
    fun getConfigFile(path: File): File {
        return File(path, "video.config")
    }

    /**
     * 下载的入口
     */
    @JvmStatic
    fun downloadVideo(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            return
        }
        if (entity.originalUrl.endsWith(".m3u8")) {
            downloadM3U8File(entity)
        } else {
            downloadSingleVideo(entity)
        }
    }

    /**
     * 下载但文件入口
     */
    @JvmStatic
    private fun downloadSingleVideo(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {//删除状态的忽略
            Log.d(TAG, "downloadSingleVideo---DELETE")
            return
        }
        if (useProgress < MAX_PROGRESS) {//还有可用的线程数
            SingleVideoDownloader.fileDownloader(entity)//进入下载
            Log.d(TAG, "-----useProgress===>$useProgress")
        } else {//没有可用线程的时候就添加到等待队列
            SingleVideoDownloader.initConfig(entity)//初始化一下下载任务
            //不是下载中的内容,且没有在等待
            if (!downloadingList.contains(entity.originalUrl) && !waitDownloadList.contains(entity.originalUrl)) {
                downloadList.add(entity)
                waitDownloadList.add(entity.originalUrl)
                Log.d(TAG, "addDownloadList---${entity.originalUrl}")
                entity.status = PREPARE
                downloadCallback.postValue(entity)
            } else {
                if (entity.status == NO_START || entity.status == ERROR || entity.status == PAUSE) {
                    //如果要下载的内容是等待中的，但是状态还没有修正过来，则修正状态
                    entity.status = PREPARE
                    downloadCallback.postValue(entity)
                }
                Log.d(TAG, "下载中或等待中的文件")
            }
        }
    }

    @JvmStatic
    private fun downloadM3U8File(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {//删除状态的忽略
            Log.d(TAG, "downloadM3U8File---DELETE")
            return
        }
        Log.d(TAG, "$wait--downloadM3U8File--${entity.originalUrl}")
        thread {
            if (wait) {//如果有在获取真实ts的内容则添加到等待队列
                Log.d(TAG, "addWaiting")
                waitList.add(entity)
                return@thread
            }
            wait = true
            val file = M3U8ConfigDownloader.start(entity)//准备下载列表
            if (useProgress < MAX_PROGRESS) {//还有可用的线程数
                if (file != null) {//需要下载
                    var times = 50
                    Log.d(TAG, "file.exists()==>${file.exists()}")
                    while (!file.exists() && times > 0) {//如果文件还不存在则等待100ms
                        Log.d(TAG, "waiting...")
                        Thread.sleep(100)
                        times--
                    }
                    if (file.exists()) {//如果文件存在了则开始下载
                        M3U8Downloader.bunchDownload(getDownloadPath(entity.originalUrl))
                    }
                    Log.d(TAG, "${file.exists()}-----useProgress===>$useProgress")
                } else {
                    Log.d(TAG, "file===null")
                }
            } else {//没有可用线程的时候就添加到等待队列
                //不是下载中的内容,且没有在等待
                if (!downloadingList.contains(entity.originalUrl) &&
                    !waitDownloadList.contains(entity.originalUrl)
                ) {//添加到任务队列
                    downloadList.add(entity)
                    waitDownloadList.add(entity.originalUrl)
                    Log.d(TAG, "addDownloadList---${entity.originalUrl}")
                    entity.status = PREPARE
                    downloadCallback.postValue(entity)
                } else {
                    Log.d(TAG, "下载中或等待中的文件")
                    if (entity.status == NO_START || entity.status == ERROR || entity.status == PAUSE) {
                        //如果要下载的内容是等待中的，但是状态还没有修正过来，则修正状态
                        entity.status = PREPARE
                        downloadCallback.postValue(entity)
                    }
                }
            }
            wait = false
            if (waitList.isNotEmpty()) {
                //有等待获取真实ts流的则继续回调
                Log.d(TAG, "removeWaiting")
                downloadM3U8File(waitList.removeAt(0))
            }
        }
    }
}
```

## 使用测试
编写完下载库，下面就进行测试了
### 下载列表的item
![item](art/m3u8Downloader_1.png)
具体代码如下：
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:paddingStart="15dp"
    android:paddingTop="8dp"
    android:paddingEnd="15dp"
    android:paddingBottom="8dp">

    <TextView
        android:id="@+id/download"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:background="@drawable/shape_download_prepare"
        android:paddingStart="15dp"
        android:paddingTop="5dp"
        android:paddingEnd="15dp"
        android:paddingBottom="5dp"
        android:text="@string/btn_download"
        android:textColor="@color/blue"
        android:textSize="12sp"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintTop_toTopOf="parent" />

    <TextView
        android:id="@+id/title"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_marginStart="15dp"
        android:ellipsize="end"
        android:maxLines="1"
        android:textSize="18sp"
        app:layout_constraintEnd_toStartOf="@id/download"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        tools:text="@string/app_name" />

    <TextView
        android:id="@+id/current_size"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginTop="5dp"
        android:textSize="12sp"
        app:layout_constraintStart_toStartOf="@id/title"
        app:layout_constraintTop_toBottomOf="@id/title"
        tools:text="201.37MB" />

    <TextView
        android:id="@+id/speed"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_marginStart="15dp"
        android:textSize="12sp"
        app:layout_constraintBottom_toBottomOf="@id/current_size"
        app:layout_constraintStart_toEndOf="@id/current_size"
        tools:text="90.5%|251.37kB/s" />

    <TextView
        android:id="@+id/url"
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:ellipsize="end"
        android:maxLines="2"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="@id/title"
        app:layout_constraintTop_toBottomOf="@id/speed"
        tools:text="https://qq.com-ok-qq.com/20191015/24619_fc6ad1d6/index.m3u8" />


</androidx.constraintlayout.widget.ConstraintLayout>
```
### Adapter的编写
```kotlin
class VideoDownloadAdapter(private val list: MutableList<VideoDownloadEntity>) :
    RecyclerView.Adapter<VideoDownloadAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_download_list, parent, false
            )
        )
    }

    override fun getItemCount() = list.size

    /**
     * 避免出现整个item闪烁
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNullOrEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.updateProgress(list[position])
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setData(list[position])
    }

    class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.title)
        private val currentSize = view.findViewById<TextView>(R.id.current_size)
        private val speed = view.findViewById<TextView>(R.id.speed)
        private val url = view.findViewById<TextView>(R.id.url)
        private val download = view.findViewById<TextView>(R.id.download)

        /**
         * 设置数据
         */
        @SuppressLint("SetTextI18n")
        fun setData(data: VideoDownloadEntity?) {
            if (data == null) {
                return
            }
            val context = view.context
            url.text = data.originalUrl
            val name = if (data.name.isNotEmpty()) {
                if (data.subName.isNotEmpty()) {
                    "${data.name}(${data.subName})"
                } else {
                    data.name
                }
            } else {
                if (data.subName.isNotEmpty()) {
                    "${context.getString(R.string.unknow_movie)}(${data.subName})"
                } else {
                    context.getString(R.string.unknow_movie)
                }
            }
            title.text = name
            updateProgress(data)
        }

        /**
         * 进度更新
         */
        @SuppressLint("SetTextI18n")
        fun updateProgress(data: VideoDownloadEntity) {
            if (data.originalUrl.endsWith(".m3u8") || data.status == COMPLETE) {
                currentSize.text =
                    getSizeUnit(data.currentSize.toDouble())
            } else {
                currentSize.text =
                    "${getSizeUnit(data.currentSize.toDouble())}/${getSizeUnit(
                        data.fileSize.toDouble()
                    )}"
            }
            speed.text =
                "${DecimalFormat("#.##%").format(data.currentProgress)}|${data.currentSpeed}"
            val context = view.context
            //状态逻辑处理
            when (data.status) {
                NO_START -> {
                    download.setTextColor(ContextCompat.getColor(context, R.color.blue))
                    download.background =
                        ContextCompat.getDrawable(context, R.drawable.shape_download_prepare)
                    download.setText(R.string.btn_download)
                    download.isVisible = true
                    speed.isVisible = false
                    currentSize.isVisible = false
                    currentSize.setText(R.string.wait_download)
                    download.setOnClickListener {
                        if (data.startDownload != null) {
                            data.startDownload!!.invoke()
                        } else {
                            FileDownloader.downloadVideo(data)
                        }
                    }
                }
                DOWNLOADING -> {
                    currentSize.isVisible = true
                    speed.isVisible = true
                    speed.setTextColor(ContextCompat.getColor(speed.context, R.color.blue))
                    download.isVisible = true
                    download.setText(R.string.pause)
                    download.setOnClickListener {
                        data.downloadContext?.stop()
                        data.downloadTask?.cancel()
                    }
                    download.setTextColor(ContextCompat.getColor(context, R.color.white))
                    download.background =
                        ContextCompat.getDrawable(context, R.drawable.shape_blue_btn)
                }
                PAUSE -> {
                    currentSize.isVisible = true
                    download.setTextColor(ContextCompat.getColor(context, R.color.white))
                    download.background =
                        ContextCompat.getDrawable(context, R.drawable.shape_blue_btn)
                    download.isVisible = true
                    download.setText(R.string.go_on)
                    download.setOnClickListener {
                        if (data.startDownload != null) {
                            data.startDownload!!.invoke()
                        } else {
                            FileDownloader.downloadVideo(data)
                        }
                    }
                    speed.isVisible = true
                    speed.setText(R.string.already_paused)
                    speed.setTextColor(ContextCompat.getColor(speed.context, R.color.red))
                }
                COMPLETE -> {
                    currentSize.isVisible = true
                    download.isVisible = false
                    speed.isVisible = false
                }
                PREPARE -> {
                    currentSize.isVisible = true
                    download.setText(R.string.prepareing)
                    currentSize.setText(R.string.wait_download)
                    download.isVisible = true
                    download.setOnClickListener {
                        if (data.startDownload != null) {
                            data.startDownload!!.invoke()
                        } else {
                            FileDownloader.downloadVideo(data)
                        }
                    }
                    download.setTextColor(ContextCompat.getColor(context, R.color.blue))
                    download.background =
                        ContextCompat.getDrawable(context, R.drawable.shape_download_prepare)
                    speed.isVisible = false
                }
                ERROR -> {
                    currentSize.isVisible = false
                    speed.isVisible = false
                    download.isVisible = true
                    download.setText(R.string.retry)
                    download.setOnClickListener {
                        if (data.startDownload != null) {
                            data.startDownload!!.invoke()
                        } else {
                            FileDownloader.downloadVideo(data)
                        }
                    }
                    download.setTextColor(ContextCompat.getColor(context, R.color.white))
                    download.background =
                        ContextCompat.getDrawable(context, R.drawable.shape_blue_btn)
                }
            }
        }
    }

}
```
由于是下载列表，如果频繁刷新是会导致整个item不断闪烁的，所以在下载库那边也有处理了1秒钟才发出一次进度更新，而在接收的时候一定要注意，需要重写`onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>)`这个函数，通知adapter更新的时候应该调用`notifyItemChanged(int position, @Nullable Object payload)`这样可以避免整个item闪烁，实现只更新局部控件的效果
### Activity的实现
```kotlin
@RuntimePermissions
class MainActivity : AppCompatActivity() {

    private lateinit var adapter: VideoDownloadAdapter
    private val videoList = arrayListOf<VideoDownloadEntity>()
    private val tempList = arrayListOf<String>()
    private val gson = GsonBuilder().create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initListView()
        initListWithPermissionCheck()
        //接收进度通知
        FileDownloader.downloadCallback.observe(this, Observer {
            onProgress(it)
        })
        //新建下载
        add.setOnClickListener {
            newDownload()
        }
    }


    private fun initListView() {
        adapter = VideoDownloadAdapter(videoList)
        list.adapter = adapter
    }

    @NeedsPermission(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun initList() {
        thread {//在线程中处理，防止ANR
            FileDownloader.getBaseDownloadPath().listFiles().forEach {
                val file = File(it, "video.config")
                if (file.exists()) {
                    val text = file.readText()
                    if (text.isNotEmpty()) {
                        val data = gson.fromJson<VideoDownloadEntity>(
                            text,
                            VideoDownloadEntity::class.java
                        )
                        if (data != null) {
                            if (data.status == DELETE) {
                                it.deleteRecursively()
                            } else if (!tempList.contains(data.originalUrl)) {
                                videoList.add(data)
                                tempList.add(data.originalUrl)
                            }
                        }
                    }
                }
            }
            runOnUiThread {
                //主线程通知刷新布局
                adapter.notifyDataSetChanged()
            }
            videoList.sort()
            //依次添加下载队列
            videoList.filter { it.status == DOWNLOADING }.forEach {
                FileDownloader.downloadVideo(it)
            }
            videoList.filter { it.status == PREPARE }.forEach {
                FileDownloader.downloadVideo(it)
            }
            videoList.filter { it.status == NO_START }.forEach {
                FileDownloader.downloadVideo(it)
            }
        }
    }

    @OnPermissionDenied(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    fun onDenied() {
        toast(R.string.need_permission_tips)
    }

    private fun toast(@StringRes msg: Int) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }
    
    private fun onProgress(entity: VideoDownloadEntity) {
        for ((index, item) in videoList.withIndex()) {
            if (item.originalUrl == entity.originalUrl) {
                videoList[index].status = entity.status
                videoList[index].currentSize = entity.currentSize
                videoList[index].currentSpeed = entity.currentSpeed
                videoList[index].currentProgress = entity.currentProgress
                videoList[index].fileSize = entity.fileSize
                videoList[index].tsSize = entity.tsSize
                videoList[index].downloadContext = entity.downloadContext
                videoList[index].downloadTask = entity.downloadTask
                videoList[index].startDownload = entity.startDownload
                adapter.notifyItemChanged(index, 0)
                break
            }
        }
    }

    private fun newDownload() {
        val editText = EditText(this)
        editText.setHint(R.string.please_input_download_address)
        val downloadDialog = AlertDialog.Builder(this)
            .setView(editText)
            .setTitle(R.string.new_download)
            .setPositiveButton(R.string.ok) { dialog, _ ->
                if (editText.text.isNullOrEmpty()) {
                    toast(R.string.please_input_download_address)
                    return@setPositiveButton
                }
                val url = editText.text.toString()
                if (tempList.contains(url)) {
                    toast(R.string.already_download)
                    dialog.dismiss()
                    return@setPositiveButton
                }
                val name = if (url.contains("?")) {
                    url.substring(url.lastIndexOf("/") + 1, url.indexOf("?"))
                } else {
                    url.substring(url.lastIndexOf("/") + 1)
                }
                val entity = VideoDownloadEntity(url, name)
                entity.toFile()
                videoList.add(0, entity)
                adapter.notifyItemInserted(0)
                FileDownloader.downloadVideo(entity)
            }
            .setNegativeButton(R.string.cancle) { dialog, _ ->
                dialog.dismiss()
            }.create()
        downloadDialog.show()
    }
}
```