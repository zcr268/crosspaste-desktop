package com.crosspaste.paste

import com.crosspaste.dto.pull.PullFilesKey
import com.crosspaste.paste.item.PasteFiles
import com.crosspaste.path.UserDataPathProvider
import com.crosspaste.presist.FilesIndex
import com.crosspaste.presist.FilesIndexBuilder
import com.crosspaste.realm.paste.PasteRealm
import com.crosspaste.task.PullFileTaskExecutor
import com.crosspaste.utils.DateUtils
import com.crosspaste.utils.DateUtils.toLocalDateTime

interface CacheManager {

    val dateUtils: DateUtils

    val pasteRealm: PasteRealm

    val userDataPathProvider: UserDataPathProvider

    suspend fun getFilesIndex(pullFilesKey: PullFilesKey): FilesIndex?

    fun loadKey(key: PullFilesKey): FilesIndex {
        val appInstanceId = key.appInstanceId
        val pasteId = key.pasteId
        pasteRealm.getPasteData(appInstanceId, pasteId)?.let { pasteData ->
            val dateString = dateUtils.getYMD(pasteData.createTime.toLocalDateTime())
            val filesIndexBuilder = FilesIndexBuilder(PullFileTaskExecutor.CHUNK_SIZE)
            val fileItems = pasteData.getPasteAppearItems().filter { it is PasteFiles }
            for (pasteAppearItem in fileItems) {
                val pasteFiles = pasteAppearItem as PasteFiles
                userDataPathProvider.resolve(appInstanceId, dateString, pasteId, pasteFiles, false, filesIndexBuilder)
            }
            return filesIndexBuilder.build()
        }
        throw IllegalStateException("paste data not found: $appInstanceId, $pasteId")
    }
}
