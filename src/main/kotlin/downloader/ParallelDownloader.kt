package downloader

import utils.splitToRanges
import utils.withRetry
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

private const val BUFFER_SIZE = 256 * 1024 // reduces the number of I/O operations → speeds up the download

suspend fun downloadRangeToFileChannel(
    client: HttpClient,
    url: String,
    range: LongRange,
    fileChannel: FileChannel,
    filePosition: Long
) = withRetry(maxRetries = 3, initialDelayMs = 500) {
    client.prepareGet(url) {
        header(HttpHeaders.Range, "bytes=${range.first}-${range.last}")
    }.execute { response ->
        if (response.status != HttpStatusCode.PartialContent) {
            throw IOException("Server does not support partial content. Code: ${response.status}")
        }

        val channel = response.bodyAsChannel()
        val buffer = ByteBuffer.allocate(BUFFER_SIZE) // stores temporary bytes
        var currentChunkBytesRead = 0L
        val expectedSize = range.last - range.first + 1

        runCatching {
            while (!channel.isClosedForRead && currentChunkBytesRead < expectedSize) {
                buffer.clear()
                val remainingBytes = (expectedSize - currentChunkBytesRead).toInt()
                val bytesToRead = minOf(BUFFER_SIZE, remainingBytes)
                buffer.limit(bytesToRead)

                val bytesRead = channel.readAvailable(buffer)
                if (bytesRead <= 0) break

                buffer.flip()
                var currentWritePos = filePosition + currentChunkBytesRead
                while (buffer.hasRemaining()) {
                    val written = fileChannel.write(buffer, currentWritePos)
                    if (written == 0) break
                    currentWritePos += written
                }
                currentChunkBytesRead += bytesRead
            }
        }.onFailure { e ->
            throw IOException("Failed to download chunk ${range.first}-${range.last}: ${e.message}", e)
        }

        if (currentChunkBytesRead == 0L) {
            throw IOException("Empty response for range ${range.first}-${range.last}")
        }
    }
}

suspend fun downloadRangesInParallel(
    client: HttpClient,
    url: String,
    ranges: List<LongRange>,
    fileChannel: FileChannel,
    startPosition: Long
) = withContext(Dispatchers.IO) {
    // Use async for parallel downloading - this is the core parallel chunk downloading logic
    val jobs = ranges.map { range ->
        async {
            val chunkStartPosition = startPosition + (range.first - ranges[0].first)
            downloadRangeToFileChannel(
                client = client, 
                url = url, 
                range = range, 
                fileChannel = fileChannel, 
                filePosition = chunkStartPosition
            )
        }
    }

    // Wait for all downloads to complete simultaneously
    jobs.awaitAll()
}

// Download one part of a file using streaming with FileChannel. Returns duration in milliseconds.
suspend fun downloadAdaptiveFilePart(
    client: HttpClient,
    url: String,
    outputFile: File,
    startByte: Long,
    partSize: Long,
    chunksPerPart: Int
): Long {
    val startTime = System.currentTimeMillis()
    val partRanges = splitToRanges(partSize, chunksPerPart)
        // .map shifts ranges to the actual location of the file
        .map { it.first + startByte..it.last + startByte }

    // Use RandomAccessFile to get FileChannel for direct streaming to disk
    RandomAccessFile(outputFile, "rw").use { randomAccessFile ->
        val fileChannel = randomAccessFile.channel
        downloadRangesInParallel(client, url, partRanges, fileChannel, startByte)
    }
    return System.currentTimeMillis() - startTime
}

suspend fun runAdaptiveParallelDownload(
    client: HttpClient,
    url: String,
    outputFile: File,
    totalLength: Long,
    partSizeBytes: Long,
    chunksPerPart: Int
): Boolean = runCatching {
    // Pre-allocate the file space if it doesn't exist or has wrong size
    if (!outputFile.exists() || outputFile.length() != totalLength) {
        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(totalLength)
        }
    }

    var startByte = 0L
    var partNumber = 1
    var currentPartSize = partSizeBytes
    val minPartSize = 64 * 1024L // 64 KB
    val maxPartSize = 10 * 1024 * 1024L // 10 MB
    val targetDurationMs = 2000L // Aim for 2 seconds per part

    while (startByte < totalLength) {
        val size = minOf(currentPartSize, totalLength - startByte)
        println("Downloading part $partNumber ($startByte - ${startByte + size - 1}) size: $size bytes...")

        val durationMs = downloadAdaptiveFilePart(client, url, outputFile, startByte, size, chunksPerPart)

        // Adaptive logic: Adjust next part size based on current speed
        if (durationMs > 0) {
            val bps = (size * 1000) / durationMs
            println("Part $partNumber completed in ${durationMs}ms. Speed: ${bps / (1024 * 1024)} MB/s")

            // New part size to meet target duration
            val newPartSize = (bps * targetDurationMs) / 1000
            currentPartSize = newPartSize.coerceIn(minPartSize, maxPartSize)
        }

        startByte += size
        partNumber++
    }

    println("\nFile successfully downloaded to $outputFile")
    true
}.getOrElse { e ->
    println("\nDownload failed: ${e.message}")
    false
}
