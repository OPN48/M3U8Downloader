package top.xuqingquan.m3u8downloader.demo

import java.util.*

/**
 * Created by 许清泉 on 2019-10-25 15:54
 */
private val units = arrayOf("B", "KB", "MB", "GB", "TB")
/**
 * 单位转换
 */
fun getSizeUnit(size: Double): String {
    var sizeUnit = size
    var index = 0
    while (sizeUnit > 1024 && index < 4) {
        sizeUnit /= 1024.0
        index++
    }
    return String.format(Locale.getDefault(), "%.2f %s", sizeUnit, units[index])
}