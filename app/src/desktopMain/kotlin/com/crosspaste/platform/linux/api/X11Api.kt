package com.crosspaste.platform.linux.api

import com.crosspaste.app.DesktopAppWindowManager.Companion.MAIN_WINDOW_TITLE
import com.crosspaste.app.DesktopAppWindowManager.Companion.SEARCH_WINDOW_TITLE
import com.crosspaste.app.LinuxAppInfo
import com.crosspaste.platform.linux.api.WMCtrl.getActiveWindow
import com.sun.jna.Native
import com.sun.jna.NativeLong
import com.sun.jna.platform.unix.X11
import com.sun.jna.platform.unix.X11.Display
import com.sun.jna.platform.unix.X11.Window
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.nio.file.Path
import javax.imageio.ImageIO

interface X11Api : X11 {

    companion object {

        private val logger = KotlinLogging.logger {}

        val INSTANCE: X11Api = Native.load("X11", X11Api::class.java)

        private var mainWindow: Window? = null

        private var searchWindow: Window? = null

        fun getActiveWindow(): LinuxAppInfo? {
            val display = INSTANCE.XOpenDisplay(null) ?: return null
            return try {
                getActiveWindow(display)?.let { previousWindow ->
                    WMCtrl.getWindowClass(display, previousWindow)?.let {
                        LinuxAppInfo(previousWindow, it.second)
                    }
                }
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        fun saveAppIcon(
            window: Window,
            iconPath: Path,
        ) {
            val display = INSTANCE.XOpenDisplay(null) ?: return
            try {
                WMCtrl.getPropertyAsIcon(display, window)?.let { buffer ->
                    ImageIO.write(buffer, "png", iconPath.toFile())
                }
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        fun bringToFront(windowTitle: String): LinuxAppInfo? {
            val display = INSTANCE.XOpenDisplay(null) ?: return null
            return try {
                val linuxAppInfo: LinuxAppInfo? =
                    getActiveWindow(display)?.let { previousWindow ->
                        WMCtrl.getWindowClass(display, previousWindow)?.let {
                            LinuxAppInfo(previousWindow, it.second)
                        }
                    }

                getWindow(windowTitle)?.let { window ->
                    WMCtrl.activeWindow(display, window)
                }
                linuxAppInfo
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        fun bringToBack(prevLinuxAppInfo: LinuxAppInfo?) {
            val display = INSTANCE.XOpenDisplay(null) ?: return
            try {
                prevLinuxAppInfo?.let {
                    WMCtrl.activeWindow(display, it.window)
                }
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        suspend fun bringToBack(
            prevLinuxAppInfo: LinuxAppInfo?,
            keyCodes: List<Int>,
        ) {
            val display = INSTANCE.XOpenDisplay(null) ?: return
            try {
                prevLinuxAppInfo?.let {
                    WMCtrl.activeWindow(display, it.window)
                    toPaste(display, keyCodes)
                }
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        private suspend fun toPaste(
            display: Display,
            keyCodes: List<Int>,
        ) {
            val xTest = X11.XTest.INSTANCE
            runCatching {
                for (keyCode in keyCodes) {
                    xTest.XTestFakeKeyEvent(display, keyCode, true, NativeLong(0))
                }
                delay(100)

                for (keyCode in keyCodes.reversed()) {
                    xTest.XTestFakeKeyEvent(display, keyCode, false, NativeLong(0))
                }
            }.onFailure { e ->
                logger.error(e) { "toPaste error" }
            }
        }

        suspend fun toPaste(keyCodes: List<Int>) {
            val display = INSTANCE.XOpenDisplay(null) ?: return
            try {
                toPaste(display, keyCodes)
            } finally {
                INSTANCE.XCloseDisplay(display)
            }
        }

        @Synchronized
        private fun getWindow(windowTitle: String): Window? {
            return if (windowTitle == MAIN_WINDOW_TITLE) {
                findMainWindow()
            } else {
                findSearchWindow()
            }
        }

        private fun findMainWindow(): Window? {
            if (mainWindow == null) {
                val display = INSTANCE.XOpenDisplay(null) ?: return null
                try {
                    val rootWindow = INSTANCE.XDefaultRootWindow(display)
                    WMCtrl.findWindowByTitle(display, rootWindow, MAIN_WINDOW_TITLE)?.let {
                        mainWindow = it
                    }
                } finally {
                    INSTANCE.XCloseDisplay(display)
                }
            }
            return mainWindow
        }

        private fun findSearchWindow(): Window? {
            if (searchWindow == null) {
                val display = INSTANCE.XOpenDisplay(null) ?: return null
                try {
                    val rootWindow = INSTANCE.XDefaultRootWindow(display)
                    WMCtrl.findWindowByTitle(display, rootWindow, SEARCH_WINDOW_TITLE)?.let {
                        searchWindow = it
                    }
                } finally {
                    INSTANCE.XCloseDisplay(display)
                }
            }
            return searchWindow
        }
    }
}
