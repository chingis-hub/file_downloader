package downloader

import utils.withRetry
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

private const val BUFFER_SIZE = 256 * 1024

suspend fun downloadFullFile(
    client: HttpClient,
    url: String,
    outputFile: File
) = withRetry(maxRetries = 3, initialDelayMs = 1000) {
    withContext(Dispatchers.IO) {// transferring execution to the IO thread pool in order not to block the main thread
        client.prepareGet(url).execute { response -> // HTTP GET request via Ktor
            if (!response.status.isSuccess()) {
                throw IOException("Server returned error: ${response.status}")
            }

            val channel = response.bodyAsChannel() // async Ktor channel for reading data from HTTP
            outputFile.outputStream().use { outputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (!channel.isClosedForRead) {
                    val bytesRead = channel.readAvailable(buffer)
                    if (bytesRead == -1) break
                    if (bytesRead > 0) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
}

suspend fun performFallbackDownload(
    client: HttpClient,
    url: String,
    outputFile: File
): Boolean = runCatching {
    downloadFullFile(client, url, outputFile)
    println("\nFile successfully downloaded to $outputFile (Single Stream)")
    true
}.getOrElse { e ->
    println("\nFallback download failed: ${e.message}")
    false
}
