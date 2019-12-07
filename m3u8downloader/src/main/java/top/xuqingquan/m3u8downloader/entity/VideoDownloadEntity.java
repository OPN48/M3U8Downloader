package top.xuqingquan.m3u8downloader.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.File;
import java.io.FileOutputStream;

import kotlin.text.Charsets;
import top.xuqingquan.m3u8downloader.AriaFileDownload;

/**
 * Created by 许清泉 on 2019-12-07 13:56
 */
public class VideoDownloadEntity implements Parcelable, Comparable<VideoDownloadEntity> {
    public static final int NO_START = 0;
    public static final int PREPARE = 1;
    public static final int DOWNLOADING = 2;
    public static final int PAUSE = 3;
    public static final int COMPLETE = 4;
    public static final int ERROR = 5;
    public static final int DELETE = -1;

    private String originalUrl;//原始下载链接
    private String name;//视频名称
    private String subName;//视频子名称
    private String redirectUrl;//重定向后的下载链接
    private String pictureUrl;//海报图片的url
    private long fileSize;//文件总大小
    private long currentSize;//当前已下载大小
    private double currentProgress;//当前进度
    private String currentSpeed;//当前速率
    private int tsSize;//ts的数量
    private long createTime;//创建时间
    private int status;//状态

    private long taskId;


    public VideoDownloadEntity() {
    }

    public VideoDownloadEntity(String originalUrl, String name, String subName, String redirectUrl, String pictureUrl, long fileSize, long currentSize, double currentProgress, String currentSpeed, int tsSize, long createTime, int status) {
        this.originalUrl = originalUrl;
        this.name = name;
        this.subName = subName;
        this.redirectUrl = redirectUrl;
        this.pictureUrl = pictureUrl;
        this.fileSize = fileSize;
        this.currentSize = currentSize;
        this.currentProgress = currentProgress;
        this.currentSpeed = currentSpeed;
        this.tsSize = tsSize;
        this.createTime = createTime;
        this.status = status;
    }

    protected VideoDownloadEntity(Parcel in) {
        originalUrl = in.readString();
        name = in.readString();
        subName = in.readString();
        redirectUrl = in.readString();
        pictureUrl = in.readString();
        fileSize = in.readLong();
        currentSize = in.readLong();
        currentProgress = in.readDouble();
        currentSpeed = in.readString();
        tsSize = in.readInt();
        createTime = in.readLong();
        status = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(originalUrl);
        dest.writeString(name);
        dest.writeString(subName);
        dest.writeString(redirectUrl);
        dest.writeString(pictureUrl);
        dest.writeLong(fileSize);
        dest.writeLong(currentSize);
        dest.writeDouble(currentProgress);
        dest.writeString(currentSpeed);
        dest.writeInt(tsSize);
        dest.writeLong(createTime);
        dest.writeInt(status);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<VideoDownloadEntity> CREATOR = new Creator<VideoDownloadEntity>() {
        @Override
        public VideoDownloadEntity createFromParcel(Parcel in) {
            return new VideoDownloadEntity(in);
        }

        @Override
        public VideoDownloadEntity[] newArray(int size) {
            return new VideoDownloadEntity[size];
        }
    };

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubName() {
        return subName;
    }

    public void setSubName(String subName) {
        this.subName = subName;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getCurrentSize() {
        return currentSize;
    }

    public void setCurrentSize(long currentSize) {
        this.currentSize = currentSize;
    }

    public double getCurrentProgress() {
        return currentProgress;
    }

    public void setCurrentProgress(double currentProgress) {
        this.currentProgress = currentProgress;
    }

    public String getCurrentSpeed() {
        return currentSpeed;
    }

    public void setCurrentSpeed(String currentSpeed) {
        this.currentSpeed = currentSpeed;
    }

    public int getTsSize() {
        return tsSize;
    }

    public void setTsSize(int tsSize) {
        this.tsSize = tsSize;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int compareTo(VideoDownloadEntity o) {
        return (int) (o.createTime - this.createTime);
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @NonNull
    @Override
    public String toString() {
        return "{" + "\"originalUrl\":\"" +
                originalUrl + '\"' +
                ",\"name\":\"" +
                name + '\"' +
                ",\"subName\":\"" +
                subName + '\"' +
                ",\"redirectUrl\":\"" +
                redirectUrl + '\"' +
                ",\"pictureUrl\":\"" +
                pictureUrl + '\"' +
                ",\"fileSize\":" +
                fileSize +
                ",\"currentSize\":" +
                currentSize +
                ",\"currentProgress\":" +
                currentProgress +
                ",\"currentSpeed\":" +
                currentSpeed +
                ",\"tsSize\":" +
                tsSize +
                ",\"createTime\":" +
                createTime +
                ",\"status\":" +
                status +
                '}';
    }

    public void toFile() {
        if (status == DELETE) {
            return;
        }
        File path = AriaFileDownload.getDownloadPath(originalUrl);
        File config = new File(path, "video.config");
        if (!config.exists() && this.createTime == 0L) {
            this.createTime = System.currentTimeMillis();
        }
        try {
            FileOutputStream fos = new FileOutputStream(config);
            fos.write(toString().getBytes(Charsets.UTF_8));
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
