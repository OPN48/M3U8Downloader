package top.xuqingquan.m3u8downloader

import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.liulishuo.okdownload.OkDownload
import top.xuqingquan.m3u8downloader.entity.*
import top.xuqingquan.m3u8downloader.utils.md5
import java.io.File
import kotlin.concurrent.thread

/**
 * Created by 许清泉 on 2019-10-14 15:21
 * 下载控制
 */
object FileDownloader {

    private val TAG = "FileDownloader"

    val downloadCallback = MutableLiveData<VideoDownloadEntity>()//下载进度回调

    private var MAX_PROGRESS = -1
        //最终计算结果至少为1
        get() {
            if (field == -1) {
                field = Runtime.getRuntime().availableProcessors() / 2//可用线程数的一半
                if (Build.VERSION.SDK_INT < 23) {
                    field -= 2
                }
            }
            if (field > 5) {
                field = 5
            }
            if (field <= 0) {
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
    //排队列表
    private val downloadList = arrayListOf<VideoDownloadEntity>()
    //等待下载的队列
    private val waitList = arrayListOf<VideoDownloadEntity>()
    private var wait = false//等待状态
    var hasShowWifiTips = false
    var hasToastWifiTips = false

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
     * 获取根据链接得到的url
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

    @JvmStatic
    fun getConfigFile(path: File): File {
        return File(path, "video.config")
    }

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

    @JvmStatic
    private fun downloadSingleVideo(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            Log.d(TAG, "downloadSingleVideo---DELETE")
            return
        }
        if (useProgress < MAX_PROGRESS) {//还有可用的线程数
            SingleVideoDownloader.fileDownloader(entity)
            Log.d(TAG, "-----useProgress===>$useProgress")
        } else {//没有可用线程的时候就添加到等待队列
            SingleVideoDownloader.initConfig(entity)
            //不是下载中的内容,且没有在等待
            if (!downloadingList.contains(entity.originalUrl) && !waitDownloadList.contains(entity.originalUrl)) {
                downloadList.add(entity)
                waitDownloadList.add(entity.originalUrl)
                Log.d(TAG, "addDownloadList---${entity.originalUrl}")
                entity.status = PREPARE
                downloadCallback.postValue(entity)
            } else {
                if (entity.status == NO_START || entity.status == ERROR || entity.status == PAUSE) {
                    entity.status = PREPARE
                    downloadCallback.postValue(entity)
                }
                Log.d(TAG, "下载中或等待中的文件")
            }
        }
    }

    @JvmStatic
    private fun downloadM3U8File(entity: VideoDownloadEntity) {
        if (entity.status == DELETE) {
            Log.d(TAG, "downloadM3U8File---DELETE")
            return
        }
        Log.d(TAG, "$wait--downloadM3U8File--${entity.originalUrl}")
        thread {
            if (wait) {
                Log.d(TAG, "addWaiting")
                waitList.add(entity)
                return@thread
            }
            wait = true
            val file = M3U8ConfigDownloader.start(entity)
            if (useProgress < MAX_PROGRESS) {//还有可用的线程数
                if (file != null) {//需要下载
                    var times = 50
                    Log.d(TAG, "file.exists()==>${file.exists()}")
                    while (!file.exists() && times > 0) {//如果文件还不存在则等待100ms
                        Log.d(TAG, "waiting...")
                        Thread.sleep(100)
                        times--
                    }
                    if (file.exists()) {
                        M3U8Downloader.bunchDownload(getDownloadPath(entity.originalUrl))
                    }
                    Log.d(TAG, "${file.exists()}-----useProgress===>$useProgress")
                } else {
                    Log.d(TAG, "file===null")
                }
            } else {//没有可用线程的时候就添加到等待队列
                //不是下载中的内容,且没有在等待
                if (!downloadingList.contains(entity.originalUrl) && !waitDownloadList.contains(
                        entity.originalUrl
                    )
                ) {
                    downloadList.add(entity)
                    waitDownloadList.add(entity.originalUrl)
                    Log.d(TAG, "addDownloadList---${entity.originalUrl}")
                    entity.status = PREPARE
                    downloadCallback.postValue(entity)
                } else {
                    Log.d(TAG, "下载中或等待中的文件")
                    if (entity.status == NO_START || entity.status == ERROR || entity.status == PAUSE) {
                        entity.status = PREPARE
                        downloadCallback.postValue(entity)
                    }
                }
            }
            wait = false
            if (waitList.isNotEmpty()) {
                Log.d(TAG, "removeWaiting")
                downloadM3U8File(waitList.removeAt(0))
            }
        }
    }
}