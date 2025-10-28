package net.ib.mn.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import com.scottyab.rootbeer.RootBeer
import java.io.File
import java.util.Locale

/**
 * Created by parkboo on 2017. 8. 29..
 * VM (Bluestack, Nox 등 가상머신과 루팅여부 검출기)
 */
class VMDetector {
    private var context: Context? = null
    private var hasRootManagementApps = false
    private var hasDangerousApps = false
    private var hasTestKeys = false
    private var hasBusyBoxBinary = false
    private var hasSuBinaryPaths = false
    private var hasSu = false
    private var hasRWPaths = false
    private var hasDangerousProps = false
    private var isNativeSuDetected = false
    private var hasRootCloakingApps = false
    private var hasBluestacksFile = false
    private var isVM = false
    private var isX86 = false
    private val mListPackageName = ArrayList<String>()
    private var detectedPkg: String? = null
    fun addPackageName(pkgName: String) {
        mListPackageName.add(pkgName)
    }

    fun checkPackageName(): Boolean {
        try {
            if (mListPackageName.isEmpty()) {
                return false
            }
            for (pkgName in mListPackageName) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context?.packageManager?.getApplicationInfo(
                            pkgName,
                            PackageManager.ApplicationInfoFlags.of(0)
                        )
                    } else {
                        context?.packageManager?.getApplicationInfo(
                            pkgName,
                            0)
                    }
                    detectedPkg = pkgName
                    return true
                } catch (_: PackageManager.NameNotFoundException) {
                }
            }
            return false
        } catch (_: Exception) {
        }
        return false
    }

    val isRooted: Boolean
        get() {
            val check = RootBeer(context)
            hasRootManagementApps = check.detectRootManagementApps()
            hasDangerousApps = check.detectPotentiallyDangerousApps()
            hasTestKeys = check.detectTestKeys()
            hasBusyBoxBinary = check.checkForBusyBoxBinary()
            hasSuBinaryPaths = check.checkForSuBinary()
            hasSu = check.checkSuExists()
            hasRWPaths = check.checkForRWPaths()
            hasDangerousProps = check.checkForDangerousProps()
            isNativeSuDetected = check.checkForRootNative()
            hasRootCloakingApps = check.detectRootCloakingApps()
            return hasRootManagementApps or hasDangerousApps or hasTestKeys or
                    hasBusyBoxBinary or hasSuBinaryPaths or hasSu or hasRWPaths or hasDangerousProps or
                    isNativeSuDetected or hasRootCloakingApps
        }

    fun isVM(): Boolean {
        var result =
            Build.FINGERPRINT.startsWith("generic") || Build.MODEL.contains("google_sdk") ||
                    Build.MODEL.lowercase(Locale.getDefault())
                        .contains("droid4x") || Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") || Build.MANUFACTURER.contains(
                "Genymotion"
            ) || Build.HARDWARE == "goldfish" || Build.HARDWARE == "vbox86" || Build.PRODUCT == "sdk" || Build.PRODUCT == "google_sdk" || Build.PRODUCT == "sdk_x86" || Build.PRODUCT == "vbox86p" ||
                    Build.BOARD.lowercase(Locale.getDefault())
                        .contains("nox") || Build.BOOTLOADER.lowercase(
                Locale.getDefault()
            ).contains("nox") ||
                    Build.HARDWARE.lowercase(Locale.getDefault())
                        .contains("nox") || Build.PRODUCT.lowercase(
                Locale.getDefault()
            ).contains("nox") ||
                    Build.SERIAL.lowercase(Locale.getDefault()).contains("nox")
        result = result || hasBluestacksFile()
        result = result || checkPackageName()
        isVM = result
        return result
    }

    /**
     * Returns true if device is Android port to x86
     */
    fun isx86Port(): Boolean {
        isX86 = false
        val kernelVersion = System.getProperty("os.version")
        if (kernelVersion != null && kernelVersion.contains("x86")) // for BlueStacks returns "2.6.38-android-x86+"
            isX86 = true
        return isX86
    }

    fun hasBluestacksFile(): Boolean {
        val test = File("/data/Bluestacks.prop")
        hasBluestacksFile = test.exists()
        val sharedFolder0 = File(
            "sdcard"
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )
        val sharedFolder1 = File(
            "/storage/emulated/0"
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )
        val sharedFolder2 = File(
            "/storage/sdcard0"
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )

        //원본 버전 용.
        val sharedFolder = File(
            Environment
                .getExternalStorageDirectory().toString()
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )

        //베타버전용.
        val sharedFolderBasic = File(
            File.separatorChar
                .toString() + "mnt"
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )

        //베타버전용. -> 셀럽  블루스택 베타버전 5용
        val sharedFolderBasic1 = File(
            "mnt"
                    + File.separatorChar
                    + "windows"
                    + File.separatorChar
                    + "BstSharedFolder"
        )
        if (sharedFolder.exists()
            || sharedFolderBasic.exists()
            || sharedFolder0.exists()
            || sharedFolder1.exists()
            || sharedFolder2.exists()
            || sharedFolderBasic1.exists()
        ) {
            hasBluestacksFile = true
        }
        return hasBluestacksFile
    }

    val hWInfo: String
        get() = "FINGERPRINT:" + Build.FINGERPRINT + " " +
                "MODEL:" + Build.MODEL + " " +
                "MANUFACTURER:" + Build.MANUFACTURER + " " +
                "HARDWARE:" + Build.HARDWARE + " " +
                "PRODUCT:" + Build.PRODUCT + " " +
                "BOARD:" + Build.BOARD + " " +
                "BOOTLOADER:" + Build.BOOTLOADER + " " +
                "SERIAL:" + Build.SERIAL

    override fun toString(): String {
        return "VMDetector{" +
                "hasRootManagementApps=" + hasRootManagementApps +
                ", hasDangerousApps=" + hasDangerousApps +
                ", hasTestKeys=" + hasTestKeys +
                ", hasBusyBoxBinary=" + hasBusyBoxBinary +
                ", hasSuBinaryPaths=" + hasSuBinaryPaths +
                ", hasSu=" + hasSu +
                ", hasRWPaths=" + hasRWPaths +
                ", hasDangerousProps=" + hasDangerousProps +
                ", isNativeSuDetected=" + isNativeSuDetected +
                ", hasRootCloakingApps=" + hasRootCloakingApps +
                ", hasBluestacksFile=" + hasBluestacksFile +
                ", isVM=" + isVM +
                ", kernelVersion=" + System.getProperty("os.version") +
                ", detectedPkg=" + detectedPkg +
                ", pkgs=" + mListPackageName.toString() +
                '}'
    }

    companion object {
        @JvmStatic
        fun getInstance(context: Context?): VMDetector {
            val detector = VMDetector()
            detector.context = context
            return detector
        }
    }
}
