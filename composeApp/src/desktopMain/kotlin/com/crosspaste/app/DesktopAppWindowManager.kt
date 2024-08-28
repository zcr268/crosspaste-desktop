package com.crosspaste.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import com.crosspaste.listen.ActiveGraphicsDevice
import com.crosspaste.listener.ShortcutKeys
import com.crosspaste.path.UserDataPathProvider
import com.crosspaste.platform.currentPlatform
import com.crosspaste.utils.Memoize
import com.crosspaste.utils.ioDispatcher
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import okio.Path
import okio.Path.Companion.toOkioPath
import java.awt.Cursor
import java.awt.Rectangle
import java.io.File
import javax.swing.JFileChooser

fun getDesktopAppWindowManager(
    lazyShortcutKeys: Lazy<ShortcutKeys>,
    activeGraphicsDevice: ActiveGraphicsDevice,
    userDataPathProvider: UserDataPathProvider,
): DesktopAppWindowManager {
    val platform = currentPlatform()
    return if (platform.isMacos()) {
        MacAppWindowManager(lazyShortcutKeys, activeGraphicsDevice, userDataPathProvider)
    } else if (platform.isWindows()) {
        WinAppWindowManager(lazyShortcutKeys, activeGraphicsDevice, userDataPathProvider)
    } else if (platform.isLinux()) {
        LinuxAppWindowManager(lazyShortcutKeys, activeGraphicsDevice, userDataPathProvider)
    } else {
        throw IllegalStateException("Unsupported platform: $platform")
    }
}

abstract class DesktopAppWindowManager : AppWindowManager {

    companion object {
        const val MAIN_WINDOW_TITLE: String = "CrossPaste"

        const val SEARCH_WINDOW_TITLE: String = "CrossPaste Search"

        // only use in Windows
        const val MENU_WINDOW_TITLE: String = "CrossPaste Menu"
    }

    protected val logger: KLogger = KotlinLogging.logger {}

    protected val ioScope = CoroutineScope(ioDispatcher + SupervisorJob())

    override var hasCompletedFirstLaunchShow by mutableStateOf(false)

    var showMainWindow by mutableStateOf(false)

    var mainWindowState: WindowState by mutableStateOf(
        WindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.PlatformDefault,
            size = DpSize(width = 480.dp, height = 740.dp),
        ),
    )

    var mainComposeWindow: ComposeWindow? by mutableStateOf(null)

    var mainFocusRequester = FocusRequester()

    override var showMainDialog by mutableStateOf(false)

    override var showFileDialog by mutableStateOf(false)

    var showSearchWindow by mutableStateOf(false)

    var searchWindowState: WindowState by mutableStateOf(
        WindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(width = 800.dp, height = 540.dp),
        ),
    )

    var searchComposeWindow: ComposeWindow? by mutableStateOf(null)

    var searchFocusRequester = FocusRequester()

    val searchWindowDetailViewDpSize = DpSize(width = 500.dp, height = 240.dp)

    protected val calPosition: (Rectangle) -> WindowPosition =
        Memoize.memoize { bounds ->
            val windowSize = searchWindowState.size
            WindowPosition(
                x = (bounds.x.dp + ((bounds.width.dp - windowSize.width) / 2)),
                y = (bounds.y.dp + ((bounds.height.dp - windowSize.height) / 2)),
            )
        }

    override fun resetMainCursor() {
        mainComposeWindow?.cursor = Cursor.getDefaultCursor()
    }

    override fun setMainCursorWait() {
        mainComposeWindow?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    }

    override fun resetSearchCursor() {
        searchComposeWindow?.cursor = Cursor.getDefaultCursor()
    }

    override fun setSearchCursorWait() {
        searchComposeWindow?.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    }

    override fun openFileChooser(
        fileChooserTitle: String,
        currentStoragePath: String,
        action: (Path) -> Unit,
        errorAction: (String) -> Unit,
    ) {
        mainComposeWindow?.let {
            showFileDialog = true
            JFileChooser().apply {
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                dialogTitle = fileChooserTitle
                currentStoragePath.let {
                    currentDirectory = File(it)
                }
                showOpenDialog(it)
                selectedFile?.let { file ->
                    val path = file.toOkioPath(normalize = true)
                    println("path: $path")
                    if (path.toString().startsWith(currentStoragePath)) {
                        errorAction("cant_select_child_directory")
                    } else if (!file.exists()) {
                        errorAction("directory_not_exist")
                    } else if (file.listFiles { it ->
                            !it.name.startsWith(".")
                        }?.isNotEmpty() == true
                    ) {
                        errorAction("directory_not_empty")
                    } else {
                        action(path)
                    }
                }
            }
            showFileDialog = false
        }
    }

    abstract fun getCurrentActiveAppName(): String?

    abstract suspend fun activeMainWindow()

    abstract suspend fun unActiveMainWindow()

    suspend fun switchMainWindow() {
        if (showMainWindow) {
            unActiveMainWindow()
        } else {
            activeMainWindow()
        }
    }

    abstract suspend fun activeSearchWindow()

    abstract suspend fun unActiveSearchWindow(preparePaste: suspend () -> Boolean)

    abstract fun getPrevAppName(): String?
}
