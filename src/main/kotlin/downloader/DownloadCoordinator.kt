package downloader

import io.ktor.client.*
import kotlinx.coroutines.*
import java.io.File

// The main function is to download the file
suspend fun downloadFile(
    client: HttpClient,
    url: String,
    outputFile: File,
    partSizeBytes: Long,
    chunksPerPart: Int
): Boolean = coroutineScope {

    // Get information about the file
    val fileInfo = getFileInfo(client, url) ?: run {
        println("Failed to get file info")
        return@coroutineScope false
    }

    val result = when {
        !fileInfo.supportsRange -> {
            println("Server does not support partial downloads. Falling back to single-stream download...")
            performFallbackDownload(client, url, outputFile)
        }
        fileInfo.length == null -> {
            println("Content length is missing and range support is claimed. Falling back to single-stream.")
            performFallbackDownload(client, url, outputFile)
        }
        else -> {
            println("Total file size: ${fileInfo.length} bytes")
            runAdaptiveParallelDownload(client, url, outputFile, fileInfo.length, partSizeBytes, chunksPerPart)
        }
    }

    return@coroutineScope result
}
