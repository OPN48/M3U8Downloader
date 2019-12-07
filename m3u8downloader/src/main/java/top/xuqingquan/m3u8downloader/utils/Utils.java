package top.xuqingquan.m3u8downloader.utils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.MessageDigest;

import kotlin.text.Charsets;

/**
 * Created by 许清泉 on 2019-12-07 22:17
 */
public class Utils {

    public static String md5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes(Charsets.UTF_8));
            byte[] secretBytes = md.digest();
            StringBuilder md5code = new StringBuilder(new BigInteger(1, secretBytes).toString(16));
            for (int i = 0; i < 32 - md5code.length() - 1; i++) {
                md5code.insert(0, "0");
            }
            return md5code.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 格式化文件大小
     *
     * @param size file.length() 获取文件大小
     */
    public static String formatFileSize(double size) {
        if (size < 0) {
            return "0KB";
        }
        double kiloByte = size / 1024;
        if (kiloByte < 1) {
            return size + "B";
        }
        double megaByte = kiloByte / 1024;
        if (megaByte < 1) {
            BigDecimal result1 = new BigDecimal(kiloByte + "");
            return result1.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "0KB";
        }
        double gigaByte = megaByte / 1024;
        if (gigaByte < 1) {
            BigDecimal result2 = new BigDecimal(megaByte + "");
            return result2.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "MB";
        }
        double teraBytes = gigaByte / 1024;
        if (teraBytes < 1) {
            BigDecimal result3 = new BigDecimal(gigaByte + "");
            return result3.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "GB";
        }
        BigDecimal result4 = new BigDecimal(teraBytes);
        return result4.setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString() + "TB";
    }

}
