package com.crosspaste.task

import com.crosspaste.db.paste.PasteDao
import com.crosspaste.db.task.BaseExtraInfo
import com.crosspaste.db.task.PasteTask
import com.crosspaste.db.task.TaskType
import com.crosspaste.exception.StandardErrorCode
import com.crosspaste.net.clientapi.createFailureResult
import com.crosspaste.rendering.RenderingService
import com.crosspaste.utils.TaskUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class Rtf2ImageTaskExecutor(
    lazyRtfRenderingService: Lazy<RenderingService<String>>,
    private val pasteDao: PasteDao,
) : SingleTypeTaskExecutor {

    private val logger = KotlinLogging.logger {}

    override val taskType: Int = TaskType.RTF_TO_IMAGE_TASK

    private val rtfRenderingService = lazyRtfRenderingService.value

    private val mutex = Mutex()

    override suspend fun doExecuteTask(pasteTask: PasteTask): PasteTaskResult =
        mutex.withLock(pasteTask.pasteDataId) {
            runCatching {
                pasteDao.getNoDeletePasteData(pasteTask.pasteDataId!!)?.let { pasteData ->
                    rtfRenderingService.render(pasteData)
                }
                SuccessPasteTaskResult()
            }.getOrElse {
                TaskUtils.createFailurePasteTaskResult(
                    logger = logger,
                    retryHandler = { false },
                    startTime = pasteTask.modifyTime,
                    fails = listOf(createFailureResult(StandardErrorCode.HTML_2_IMAGE_TASK_FAIL, it)),
                    extraInfo = TaskUtils.getExtraInfo(pasteTask, BaseExtraInfo::class),
                )
            }
        }
}
