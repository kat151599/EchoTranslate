package com.gameocr.app.capture

import android.content.ComponentName
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.IBinder
import android.os.ParcelFileDescriptor
import com.gameocr.app.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku
import timber.log.Timber

/** Shizuku UserService screenshot backend using a PNG pipe for large frames. */
class ShizukuScreenshotter : Screenshotter {
    private val released = AtomicBoolean(false)
    private val userService = ShizukuUserServiceClient()

    override val isReady: Boolean
        get() = !released.get() && runCatching { Shizuku.pingBinder() }.getOrDefault(false)

    override suspend fun capture(): Bitmap? = withContext(Dispatchers.IO) {
        if (!isReady) {
            Timber.w("SHIZUKU USERSERVICE FAILED reason=Shizuku is not ready")
            return@withContext null
        }
        val bitmap = runCatching {
            userService.capturePng()?.let { descriptor ->
                ParcelFileDescriptor.AutoCloseInputStream(descriptor).use(BitmapFactory::decodeStream)
            }
        }.onFailure { Timber.w(it, "SHIZUKU USERSERVICE FAILED reason=%s", it.message) }
            .getOrNull()
        if (bitmap == null) {
            Timber.w("SHIZUKU USERSERVICE FAILED reason=empty or undecodable PNG")
        } else {
            Timber.i("SHIZUKU SCREENSHOT OK")
        }
        bitmap
    }

    override fun release() {
        released.set(true)
        userService.release()
    }
}

private class ShizukuUserServiceClient {
    private val mutex = Mutex()
    private var service: IShizukuScreencapService? = null
    private var connectWaiter: CompletableDeferred<IShizukuScreencapService?>? = null

    private val args = Shizuku.UserServiceArgs(
        ComponentName(BuildConfig.APPLICATION_ID, ShizukuScreencapUserService::class.java.name)
    ).processNameSuffix("shizuku_screencap").tag("gameocr-screencap").version(1).daemon(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val connected = IShizukuScreencapService.Stub.asInterface(binder)
            service = connected
            connectWaiter?.complete(connected)
            Timber.i("SHIZUKU USERSERVICE CONNECTED")
        }

        override fun onServiceDisconnected(name: ComponentName) {
            service = null
            connectWaiter?.complete(null)
            Timber.w("SHIZUKU USERSERVICE FAILED reason=binder disconnected")
        }
    }

    suspend fun capturePng() = withContext(Dispatchers.IO) {
        val remote = ensureConnected() ?: return@withContext null
        runCatching { remote.capturePng() }
            .onFailure {
                service = null
                Timber.w(it, "SHIZUKU USERSERVICE FAILED reason=%s", it.message)
            }
            .getOrNull()
    }

    private suspend fun ensureConnected(): IShizukuScreencapService? = mutex.withLock {
        service?.let { return@withLock it }
        if (!runCatching { Shizuku.pingBinder() }.getOrDefault(false)) return@withLock null
        val waiter = CompletableDeferred<IShizukuScreencapService?>()
        connectWaiter = waiter
        runCatching { Shizuku.bindUserService(args, connection) }
            .onFailure {
                Timber.w(it, "SHIZUKU USERSERVICE FAILED reason=%s", it.message)
                waiter.complete(null)
            }
        withTimeoutOrNull(5_000L) { waiter.await() }
            ?: run {
                Timber.w("SHIZUKU USERSERVICE FAILED reason=connection timeout")
                null
            }
    }

    fun release() {
        service = null
        connectWaiter?.cancel()
        connectWaiter = null
        runCatching { Shizuku.unbindUserService(args, connection, true) }
            .onFailure { Timber.w(it, "SHIZUKU USERSERVICE FAILED reason=unbind failed: %s", it.message) }
    }
}
