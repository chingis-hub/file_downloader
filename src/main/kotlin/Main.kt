package com.chingis

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException


// File info data class
data class FileInfo(val length: Long, val supportsRange: Boolean)

// Get file size and Range support with a single HEAD request
fun getFileInfo(client: OkHttpClient, url: String): FileInfo? {
    val request = Request.Builder()
        .url(url)
        .head()
        .build()
    client.newCall(request).execute().use { response ->
        val length = response.header("Content-Length")?.toLongOrNull() ?: return null
        val supports = response.header("Accept-Ranges")?.equals("bytes", true) == true
        return FileInfo(length, supports)
    }
}

fun splitToRanges(contentLength: Long, parts: Int): List<LongRange> {
    require(parts > 0)
    require(contentLength > 0)

    val result = mutableListOf<LongRange>()
    val chunkSize = contentLength / parts
    val remainder = contentLength % parts
    var start = 0L

    for (i in 0 until parts) {
        // the end of each range
        var end = start + chunkSize - 1
        // the last part gets the remainder
        if (i == parts - 1) {
            end += remainder
        }
        // add the range to the list
        result.add(start..end)

        // shift the beginning of the next part
        start = end + 1
    }
    return result
}

fun downloadAllChunks(client: OkHttpClient, url: String, ranges: List<LongRange>): Array<ByteArray?> {

    val results = arrayOfNulls<ByteArray>(ranges.size)

    // Threads logic
    val threads = ranges.mapIndexed { index, range ->
        Thread {
            val chunk = downloadChunk(client, url, range)
            results[index] = chunk
            println("Range is downloaded $index: ${range.first}-${range.last}")
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    return results
}

fun downloadChunk(client: OkHttpClient, url: String, range: LongRange): ByteArray {
    val request = Request.Builder()
        .url(url)
        .addHeader("Range", "bytes=${range.first}-${range.last}")
        .build()

    client.newCall(request).execute().use { response ->
        if (response.code != 206) {
            throw IOException("Server does not support a partial content. Code: ${response.code}")
        }

        return response.body.bytes().takeIf { it.isNotEmpty() }
            ?: throw IOException("Empty response")
    }
}

fun combineTheChunks(outputFile: File, results: Array<ByteArray?>) {
    outputFile.outputStream().use { output ->
        results.forEach { chunk ->
            chunk?.let { output.write(it) }
        }
    }
}


fun main() {
    val client = OkHttpClient()
    val url = "http://localhost:8080/test_file.pdf"
    val outputFile = File("downloaded_file.pdf")

    // Get information about the file
    val fileInfo = getFileInfo(client, url) ?: run {
        println("Failed to get file info")
        return
    }

    // Check whether the server supports Range requests
    if (!fileInfo.supportsRange) {
        println("Server does not support partial downloads (Range requests)")
        return
    }
    // Division into ranges
    val parts = 4
    val ranges = splitToRanges(fileInfo.length, parts)
    println("Ranges of downloading: $ranges")

    // Create an array to store the results
    val results = downloadAllChunks(client, url, ranges)

    // Check if all the parts are loaded
    if (results.any { it == null }) {
        println("Not all parts were successfully downloaded")
        return
    }

    // Combine the parts into a single file
    combineTheChunks(outputFile, results)

    println("File was successfully downloaded and saved like $outputFile")

}