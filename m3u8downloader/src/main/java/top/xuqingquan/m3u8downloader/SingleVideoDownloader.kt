package top.xuqingquan.m3u8downloader

import android.util.Log
import com.liulishuo.okdownload.DownloadTask
import com.liulishuo.okdownload.SpeedCalculator
import com.liulishuo.okdownload.core.cause.EndCause
import com.liulishuo.okdownload.core.cause.ResumeFailedCause
import com.liulishuo.okdownload.core.listener.DownloadListener1
import com.liulishuo.okdownload.core.listener.assist.Listener1Assist
import top.xuqingquan.m3u8downloader.entity.*
import java.io.File

/**
 * Created by 许清泉 on 2019-10-16 09:54
 */
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