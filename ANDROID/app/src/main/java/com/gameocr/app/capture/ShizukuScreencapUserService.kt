package com.gameocr.app.capture

import android.os.ParcelFileDescriptor
import android.util.Log
import java.util.concurrent.Executors

/** Runs in the Shizuku shell/root process, not in the app process. */
class ShizukuScreencapUserService : IShizukuScreencapService.Stub() {
    private val executor = Executors.newSingleThreadExecutor()

    override fun capturePng(): ParcelFileDescriptor? {
        val (readSide, writeSide) = ParcelFileDescriptor.createPipe()
        executor.execute {
            try {
                val process = ProcessBuilder("/system/bin/screencap", "-p").start()
                ParcelFileDescriptor.AutoCloseOutputStream(writeSide).use { output ->
                    process.inputStream.use { input -> input.copyTo(output) }
                }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    Log.w(TAG, "screencap exit=$exitCode stderr=${process.errorStream.bufferedReader().readText().take(300)}")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "screencap failed", t)
                runCatching { writeSide.close() }
            }
        }
        return readSide
    }

    override fun destroy() {
        executor.shutdownNow()
        System.exit(0)
    }

    private companion object { const val TAG = "ShizukuScreencap" }
}
