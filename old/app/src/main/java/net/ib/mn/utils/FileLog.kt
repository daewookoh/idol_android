package net.ib.mn.utils

import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.io.IOException

object FileLog {
    private val LOG_DIR = Environment.getExternalStorageDirectory()
        .toString() + File.separator + "temp" + File.separator + "idol"

    fun writeLog(msg: String) {
        if (!Util.isSdPresent()) return
        var f = File(LOG_DIR)
        if (!f.exists()) f.mkdirs()
        f = File(LOG_DIR, "log_" + Util.getToday("yyyyMMdd") + ".txt")
        var fw = FileWriter(f, true)
        try {
            // create directory if directory not exists
            fw.append(
                """
    ${Util.getToday("yyyy-MM-dd hh:mm:ss")}|$msg
    
    """.trimIndent()
            )
        } catch (_: Exception) {
        } finally {
            try {
                fw.close()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
    }
}
