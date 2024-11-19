package com.crosspaste.utils

import dev.whyoleg.cryptography.operations.Hasher
import io.ktor.utils.io.core.*
import okio.Path
import okio.buffer
import okio.use

expect fun getCodecsUtils(): CodecsUtils

val HEX_DIGITS: CharArray = "0123456789abcdef".toCharArray()

val CROSS_PASTE_SEED = 13043025u

val CROSSPASTE_HASH = MurmurHash3(CROSS_PASTE_SEED)

interface CodecsUtils {

    val fileUtils: FileUtils

    val sha256: Hasher

    fun base64Encode(bytes: ByteArray): String

    fun base64Decode(string: String): ByteArray

    fun hash(bytes: ByteArray): String {
        val (hash1, hash2) = CROSSPASTE_HASH.hash128x64(bytes)
        return buildString(32) {
            appendHex(hash1)
            appendHex(hash2)
        }
    }

    fun hash(path: Path): String {
        val streamingMurmurHash3 = StreamingMurmurHash3(CROSS_PASTE_SEED)
        val bufferSize = fileUtils.fileBufferSize
        val buffer = ByteArray(bufferSize)

        fileUtils.fileSystem.source(path).buffer().use { bufferedSource ->
            while (true) {
                val bytesRead = bufferedSource.read(buffer, 0, bufferSize)
                if (bytesRead == -1) break
                streamingMurmurHash3.update(buffer, 0, bytesRead)
            }
        }

        val (hash1, hash2) = streamingMurmurHash3.finish()
        return buildString(32) {
            appendHex(hash1)
            appendHex(hash2)
        }
    }

    fun hashByString(string: String): String {
        return hash(string.toByteArray())
    }

    fun hashByArray(array: Array<String>): String

    @OptIn(ExperimentalStdlibApi::class)
    fun sha256(path: Path): String {
        val sha256Hasher = sha256.createHashFunction()
        val bufferSize = fileUtils.fileBufferSize
        val buffer = ByteArray(bufferSize)

        fileUtils.fileSystem.source(path).buffer().use { bufferedSource ->
            while (true) {
                val bytesRead = bufferedSource.read(buffer, 0, bufferSize)
                if (bytesRead == -1) break
                sha256Hasher.update(buffer, 0, bytesRead)
            }
        }
        val hash = sha256Hasher.hashToByteArray()
        return hash.toHexString()
    }

    fun StringBuilder.appendHex(value: ULong) {
        for (i in 0 until 8) {
            val byte = (value shr i * 8).toByte()
            append(HEX_DIGITS[(byte.toInt() shr 4) and 0xf])
            append(HEX_DIGITS[byte.toInt() and 0xf])
        }
    }
}
