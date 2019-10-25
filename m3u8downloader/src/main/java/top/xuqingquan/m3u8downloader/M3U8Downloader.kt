package top.xuqingquan.m3u8downloader

import android.util.Log
import com.liulishuo.okdownload.DownloadContext
import com.liulishuo.okdownload.DownloadContextListener
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import top.xuqingquan.m3u8downloader.entity.*
import java.io.File

/**
 * Created by 许清泉 on 2019-10-14 16:51
 */
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