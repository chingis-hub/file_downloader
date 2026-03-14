package downloader

import models.FileInfo
import utils.withRetry
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.IOException

// Get file size and Range support with a single HEAD request
suspend fun getFileInfo(client: HttpClient, url: String): FileInfo? = try {
    withRetry(maxRetries = 2, initialDelayMs = 500) {
        val response = client.head(url)
        if (!response.status.isSuccess()) {
            throw IOException("Server returned error: ${response.status}")
        }

        val length = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        val supports = response.headers[HttpHeaders.AcceptRanges]?.equals("bytes", true) == true

        FileInfo(length, supports)
    }
} catch (e: Exception) {
    println("Failed to fetch file info after retries: ${e.message}")
    null
}
