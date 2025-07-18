package com.crosspaste.paste

import com.crosspaste.app.AppFileType
import com.crosspaste.db.paste.PasteDao
import com.crosspaste.db.paste.PasteData
import com.crosspaste.exception.PasteException
import com.crosspaste.exception.StandardErrorCode
import com.crosspaste.notification.MessageType
import com.crosspaste.notification.NotificationManager
import com.crosspaste.paste.item.PasteFiles
import com.crosspaste.path.UserDataPathProvider
import com.crosspaste.utils.DateUtils
import com.crosspaste.utils.getCodecsUtils
import com.crosspaste.utils.getCompressUtils
import com.crosspaste.utils.getFileUtils
import com.crosspaste.utils.ioDispatcher
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okio.BufferedSink
import okio.Path

class PasteExportService(
    private val notificationManager: NotificationManager,
    private val pasteDao: PasteDao,
    private val userDataPathProvider: UserDataPathProvider,
) {
    private val logger = KotlinLogging.logger { }

    private val codecsUtils = getCodecsUtils()

    private val compressUtils = getCompressUtils()

    private val fileUtils = getFileUtils()

    private val ioCoroutineDispatcher = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val mutex = Mutex()

    fun export(
        pasteExportParam: PasteExportParam,
        updateProgress: (Float) -> Unit,
    ) {
        ioCoroutineDispatcher.launch {
            mutex.withLock {
                doExport(pasteExportParam, updateProgress)
            }
        }
    }

    private fun doExport(
        pasteExportParam: PasteExportParam,
        updateProgress: (Float) -> Unit,
    ) {
        var exportTempPath: Path? = null
        runCatching {
            val tempDir = userDataPathProvider.resolve(appFileType = AppFileType.TEMP)
            val epochMilliseconds = DateUtils.nowEpochMilliseconds()
            val basePath = tempDir.resolve("export-$epochMilliseconds", true)
            exportTempPath = basePath
            userDataPathProvider.autoCreateDir(basePath)
            val pasteDataFile = basePath.resolve("paste.data")
            var count = 1L
            fileUtils.writeFile(pasteDataFile) { sink ->
                runCatching {
                    val exportCount = pasteDao.getExportNum(pasteExportParam)
                    pasteDao.batchReadPasteData(
                        readPasteDataList = { id, limit ->
                            pasteDao.getExportPasteData(id, limit, pasteExportParam)
                        },
                        dealPasteData = { pasteData ->
                            count += exportPasteData(basePath, pasteExportParam, count, pasteData, sink)
                            val currentProgress =
                                if (count != exportCount) {
                                    count.toFloat() / exportCount.toFloat()
                                } else {
                                    0.99f
                                }
                            updateProgress(currentProgress)
                        },
                    )
                }.onFailure { e ->
                    logger.error(e) { "read pasteData list fail" }
                }
            }
            val exportFileName = "crosspaste-export-$epochMilliseconds.data"
            if (count > 0L) {
                val countFile = basePath.resolve("$count.count")
                fileUtils.createFile(countFile)
                compressExportFile(basePath, pasteExportParam, exportFileName)
                notificationManager.sendNotification(
                    title = { it.getText("export_successful") },
                    message = { exportFileName },
                    messageType = MessageType.Success,
                    duration = null,
                )
            } else {
                notificationManager.sendNotification(
                    title = { it.getText("no_data_found") },
                    messageType = MessageType.Warning,
                )
            }
            updateProgress(1f)
        }.onFailure { e ->
            // to set export failed
            updateProgress(-1f)
            logger.error(e) { "export pasteData failed" }
            notificationManager.sendNotification(
                title = { it.getText("export_fail") },
                messageType = MessageType.Error,
            )
        }.apply {
            exportTempPath?.let {
                fileUtils.deleteFile(it)
            }
        }
    }

    private fun exportPasteData(
        basePath: Path,
        pasteExportParam: PasteExportParam,
        index: Long,
        pasteData: PasteData,
        sink: BufferedSink,
    ): Long =
        runCatching {
            val pasteFilesList =
                pasteData
                    .getPasteAppearItems()
                    .filterIsInstance<PasteFiles>()
                    .filter { pasteData ->
                        pasteExportParam.maxFileSize?.let {
                            pasteData.size <= pasteExportParam.maxFileSize
                        } != false
                    }

            for (pasteFiles in pasteFilesList) {
                copyResource(basePath, index, pasteData, pasteFiles)
            }

            val json = pasteData.toJson()
            val base64 = codecsUtils.base64Encode(json.encodeToByteArray())
            sink.write(base64.encodeToByteArray())
            sink.writeUtf8("\n")
            1L
        }.getOrElse { e ->
            logger.error(e) { "export pasteData fail, id = ${pasteData.id}" }
            0L
        }

    private fun copyResource(
        basePath: Path,
        index: Long,
        pasteData: PasteData,
        pasteFiles: PasteFiles,
    ) {
        val path =
            basePath
                .resolve(pasteData.appInstanceId)
                .resolve(index.toString())

        userDataPathProvider.autoCreateDir(path)

        for (filePath in pasteFiles.getFilePaths(userDataPathProvider)) {
            fileUtils.copyPath(filePath, path.resolve(filePath.name))
        }
    }

    private fun compressExportFile(
        basePath: Path,
        pasteExportParam: PasteExportParam,
        exportFileName: String,
    ) {
        pasteExportParam.exportBufferedSink(exportFileName)?.let { bufferedSink ->
            compressUtils
                .zipDir(basePath, bufferedSink)
                .onSuccess {
                    runCatching {
                        bufferedSink.flush()
                        bufferedSink.close()
                    }
                }.onFailure {
                    logger.error { "compress export file fail" }
                    throw PasteException(
                        StandardErrorCode.EXPORT_FAIL.toErrorCode(),
                        "compress export file fail",
                    )
                }
        } ?: run {
            logger.error { "cant to write fail" }
            throw PasteException(
                StandardErrorCode.EXPORT_FAIL.toErrorCode(),
                "cant to write fail",
            )
        }
    }
}
