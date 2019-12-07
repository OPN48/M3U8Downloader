package top.xuqingquan.m3u8downloader;

import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.MutableLiveData;

import com.arialyy.annotations.Download;
import com.arialyy.annotations.M3U8;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.AriaManager;
import com.arialyy.aria.core.download.DownloadEntity;
import com.arialyy.aria.core.download.m3u8.M3U8VodOption;
import com.arialyy.aria.core.processor.IBandWidthUrlConverter;
import com.arialyy.aria.core.processor.IVodTsUrlConverter;
import com.arialyy.aria.core.task.DownloadTask;
import com.arialyy.aria.util.ALog;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import top.xuqingquan.m3u8downloader.entity.VideoDownloadEntity;
import top.xuqingquan.m3u8downloader.utils.Utils;

import static top.xuqingquan.m3u8downloader.entity.VideoDownloadEntity.*;

/**
 * Created by 许清泉 on 2019-12-07 22:37
 */
public class AriaFileDownload {
    private static final String TAG = "AriaFileDownload";

    private static final int MAX_PROGRESS = 4;

    private static HashMap<String, VideoDownloadEntity> downloadList = new HashMap<>();
    public static MutableLiveData<VideoDownloadEntity> downloadCallback = new MutableLiveData<>();
    private static boolean hasInit = false;
    private static AriaFileDownload instance;

    private AriaFileDownload() {
    }

    public static AriaFileDownload getInstance(){
        if (instance==null){
            instance=new AriaFileDownload();
        }
        return instance;
    }

    public void register(Context context) {
        if (hasInit) {
            return;
        }
        AriaManager manager = Aria.init(context);
        manager.getAppConfig().setNetCheck(false).setUseAriaCrashHandler(true).setNotNetRetry(true)
                .setLogLevel(BuildConfig.DEBUG ? ALog.LOG_LEVEL_VERBOSE : ALog.LOG_CLOSE);

        manager.getDownloadConfig().setMaxTaskNum(MAX_PROGRESS).setReTryNum(3);
        Aria.download(this).register();
        hasInit = true;
    }


    /**
     * 获取最顶层的下载目录
     */
    public static File getBaseDownloadPath() {
        File file = new File(Environment.getExternalStorageDirectory(), "ysdq/.video_download");
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }


    /**
     * 获取最顶层的下载目录
     */
    public static File getDownloadPath(String url) {
        File file = new File(Environment.getExternalStorageDirectory(), Utils.md5(url));
        if (!file.exists()) {
            file.mkdirs();
        }
        return file;
    }

    private static String getDownloadFilePath(VideoDownloadEntity entity) {
        String fileName;
        if (entity.getOriginalUrl().contains(".m3u8")) {
            fileName = entity.getName() + "-" + entity.getSubName() + ".mp4";
        } else {
            String path = Uri.parse(entity.getOriginalUrl()).getPath();
            if (path != null) {
                fileName = path.substring(path.lastIndexOf("/") + 1);
            } else {
                fileName = entity.getName() + "-" + entity.getSubName() + ".mp4";
            }
        }
        return new File(getDownloadPath(entity.getOriginalUrl()), fileName).getAbsolutePath();
    }

    public static void downloadVideo(VideoDownloadEntity entity) {
        if (entity.getStatus() == DELETE) {
            return;
        }
        if (!hasInit) {
            throw new ExceptionInInitializerError("没有初始化，需要调用AriaFileDownload.register(Context context)进行初始化");
        }
        String path = getDownloadFilePath(entity);
        File file = new File(path);
        if (file.exists() && file.length() > 0) {
            entity.setStatus(COMPLETE);
            entity.setCurrentProgress(1.0);
            entity.setCurrentSize(file.length());
            entity.setFileSize(file.length());
            entity.toFile();
            downloadCallback.postValue(entity);
            return;
        }
        if (entity.getCurrentProgress() == 1.0) {
            entity.setStatus(COMPLETE);
            downloadCallback.postValue(entity);
            return;
        }
        if (entity.getOriginalUrl().contains(".m3u8")) {
            getInstance().downloadM3U8File(entity);
        } else {
            getInstance().downloadSingleVideo(entity);
        }
        downloadList.put(entity.getOriginalUrl(), entity);
    }

    private  void downloadSingleVideo(VideoDownloadEntity entity) {
        DownloadEntity downloadEntity = Aria.download(this).getFirstDownloadEntity(entity.getOriginalUrl());
        if (downloadEntity == null || downloadEntity.getId() == -1L) {
            long taskId = Aria
                    .download(this)
                    .load(entity.getOriginalUrl())
                    .setFilePath(getDownloadFilePath(entity), true)
                    .create();
            entity.setTaskId(taskId);
        } else {
            entity.setTaskId(downloadEntity.getId());
            Aria.download(this).load(downloadEntity.getId()).resume();
        }
    }

    private  void downloadM3U8File(VideoDownloadEntity entity) {
        DownloadEntity downloadEntity = Aria.download(this).getFirstDownloadEntity(entity.getOriginalUrl());
        if (downloadEntity == null || downloadEntity.getId() == -1L) {
            long taskId = Aria
                    .download(this)
                    .load(entity.getOriginalUrl())
                    .setFilePath(getDownloadFilePath(entity), true)
                    .m3u8VodOption(getM3U8VodOption(entity))
                    .create();
            entity.setTaskId(taskId);
        } else {
            entity.setTaskId(downloadEntity.getId());
            Aria
                    .download(this)
                    .load(downloadEntity.getId())
                    .m3u8VodOption(getM3U8VodOption(entity))
                    .resume();
        }
    }

    private  M3U8VodOption getM3U8VodOption(final VideoDownloadEntity entity) {
        final String url = entity.getOriginalUrl();
        M3U8VodOption option = new M3U8VodOption();
        option.setVodTsUrlConvert(new IVodTsUrlConverter() {
            @Override
            public List<String> convert(String m3u8Url, List<String> tsUrls) {
                entity.setTsSize(tsUrls.size());
                Uri uri = Uri.parse(m3u8Url);
                List<String> newUrls = new ArrayList<>();
                for (String it : tsUrls) {
                    String tsUrl;
                    if (it.startsWith("http")) {
                        tsUrl = it;
                    } else if (it.startsWith("//")) {
                        tsUrl = uri.getScheme() + it;
                    } else if (it.startsWith("/")) {
                        tsUrl = uri.getScheme() + "://" + uri.getHost() + it;
                    } else {
                        String baseUrl = m3u8Url.substring(0, m3u8Url.lastIndexOf("/") + 1);
                        tsUrl = baseUrl + it;
                    }
                    newUrls.add(tsUrl);
                    Log.d(TAG, "convert: "+tsUrl);
                }
                if (entity.getRedirectUrl().isEmpty()) {
                    entity.setRedirectUrl(m3u8Url);
                }
                entity.toFile();
                return newUrls;
            }
        });
        option.setBandWidthUrlConverter(new IBandWidthUrlConverter() {
            @Override
            public String convert(String bandWidthUrl) {
                Uri uri = Uri.parse(url);
                String redirectUrl;
                if (bandWidthUrl.startsWith("http")) {
                    redirectUrl = bandWidthUrl;
                } else if (bandWidthUrl.startsWith("/")) {
                    redirectUrl = uri.getScheme() + "://" + bandWidthUrl;
                } else {
                    String baseUrl = url.substring(0, url.lastIndexOf("/") + 1);
                    redirectUrl = baseUrl + bandWidthUrl;
                }
                Log.d(TAG, "convert: "+redirectUrl);
                entity.setRedirectUrl(redirectUrl);
                entity.toFile();
                return redirectUrl;
            }
        });
        option.merge(false);
        option.setMaxTsQueueNum(4);
        return option;
    }

    public  void stopTask(VideoDownloadEntity entity) {
        if (entity.getTaskId() == -1L) {
            DownloadEntity downloadEntity=Aria.download(this).getFirstDownloadEntity(entity.getOriginalUrl());
            if (downloadEntity!=null){
                entity.setTaskId(downloadEntity.getId());
            }else{
                entity.setTaskId(-1);
            }
        }
        if (entity.getTaskId() == -1L) {
            return;
        }
        Aria.download(this).load(entity.getTaskId()).stop();
        entity.setStatus(PAUSE);
        entity.toFile();
        downloadCallback.postValue(entity);
        downloadList.remove(entity.getOriginalUrl());
    }

    //预处理的注解，在任务为开始前回调（一般在此处预处理UI界面）
    @Download.onPre
    public void onPre(DownloadTask task) {
        Log.d(TAG, "onPre: ");
        if (task == null) {
            return;
        }
        if (!downloadList.containsKey(task.getKey())) {
            task.stop();
        }
        VideoDownloadEntity entity = downloadList.get(task.getKey());
        if (entity != null) {
            entity.setStatus(PREPARE);
            entity.toFile();
            downloadCallback.postValue(entity);
        }
    }

    //任务执行时的注解，任务正在执行时进行回调
    @Download.onTaskRunning
    public void onTaskRunning(DownloadTask task) {
        Log.d(TAG, "onTaskRunning: ");
        if (task == null) {
            return;
        }
        if (!downloadList.containsKey(task.getKey())) {
            task.stop();
        }
        VideoDownloadEntity entity = downloadList.get(task.getKey());
        boolean m3u8Video = task.getKey().contains(".m3u8");
        if (entity != null) {
            if (!m3u8Video) {
                entity.setFileSize(task.getFileSize());
                entity.setCurrentSize(entity.getFileSize() * task.getPercent() / 100);
            }
            entity.setCurrentProgress(task.getPercent() / 100.0);
            entity.setCurrentSpeed(Utils.formatFileSize(task.getSpeed() * 1.0));
            entity.setStatus(DOWNLOADING);
            entity.toFile();
            downloadCallback.postValue(entity);
        }
    }

    //任务停止时的注解，任务停止时进行回调
    @Download.onTaskStop
    public void onTaskStop(DownloadTask task) {
        Log.d(TAG, "onTaskStop: ");
        if (task == null) {
            return;
        }
        if (!downloadList.containsKey(task.getKey())) {
            task.stop();
        }
        VideoDownloadEntity entity = downloadList.get(task.getKey());
        boolean m3u8Video = task.getKey().contains(".m3u8");
        if (entity != null) {
            if (!m3u8Video) {
                entity.setFileSize(task.getFileSize());
                entity.setCurrentSize(entity.getFileSize() * task.getPercent() / 100);
            }
            entity.setCurrentSpeed(Utils.formatFileSize(task.getSpeed() * 1.0));
            entity.setStatus(PAUSE);
            entity.toFile();
            downloadCallback.postValue(entity);
            downloadList.remove(entity.getOriginalUrl());
        }
    }

    //任务失败时的注解，任务执行失败时进行回调
    @Download.onTaskFail
    public void onTaskFail(DownloadTask task) {
        Log.d(TAG, "onTaskFail: ");
        if (task == null) {
            return;
        }
        if (!downloadList.containsKey(task.getKey())) {
            task.stop();
        }
        VideoDownloadEntity entity = downloadList.get(task.getKey());
        if (entity != null) {
            entity.setStatus(ERROR);
            entity.toFile();
            downloadCallback.postValue(entity);
        }
    }

    //任务完成时的注解，任务完成时进行回调
    @Download.onTaskComplete
    public void onTaskComplete(DownloadTask task) {
        Log.d(TAG, "onTaskComplete: ");
        if (task == null) {
            return;
        }
        VideoDownloadEntity entity = downloadList.get(task.getKey());
        boolean m3u8Video = task.getKey().contains(".m3u8");
        if (entity != null) {
            if (!m3u8Video) {
                entity.setFileSize(task.getFileSize());
                entity.setCurrentSize(entity.getFileSize() * task.getPercent() / 100);
            }
            entity.setCurrentProgress(1.0);
            entity.setCurrentSpeed(Utils.formatFileSize(task.getSpeed() * 1.0));
            entity.setStatus(COMPLETE);
            entity.toFile();
            downloadCallback.postValue(entity);
        }
    }

    @M3U8.onPeerComplete
    public void onPeerComplete(String m3u8Url, String peerPath, int peerIndex) {
        VideoDownloadEntity entity = downloadList.get(m3u8Url);
        File tsFile = new File(peerPath);
        if (entity != null) {
            entity.setFileSize(entity.getFileSize() + tsFile.length());
            entity.setCurrentSize(entity.getCurrentSize() + tsFile.length());
            entity.toFile();
        }
    }


}
