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

fun downloadAllChunks(client: OkHttpClient, url: String, ranges: List<LongRange>): Array<ByteArray?> {

    val results = arrayOfNulls<ByteArray>(ranges.size)

    // Threads logic
    val threads = ranges.mapIndexed { index, range ->
        Thread {
            val chunk = downloadChunk(client, url, range)
            results[index] = chunk
            println("Downloaded subchunk $index: ${range.first}-${range.last}")
        }
    }

    threads.forEach { it.start() }
    threads.forEach { it.join() }

    return results
}

// Download one part of a file
fun downloadPart(client: OkHttpClient, url: String, outputFile: File, startByte: Long, partSize: Long, chunksPerPart: Int) {
    val partRanges = splitToRanges(partSize, chunksPerPart)
        // .map shifts ranges to the actual location of the file
        .map { it.first + startByte..it.last + startByte }

    val partResults = downloadAllChunks(client, url, partRanges)

    outputFile.outputStream().use { output ->
        output.channel.position(startByte)
        partResults.forEachIndexed { i, chunk ->
            chunk?.let { output.write(it) }
        }
    }
}

// The main function is to download the file in parts
fun downloadFileInChunks(client: OkHttpClient, url: String, outputFile: File, partSizeBytes: Long , chunksPerPart: Int): Boolean {

    // Get information about the file
    val fileInfo = getFileInfo(client, url) ?: run {
        println("Failed to get file info")
        return false
    }

    if (!fileInfo.supportsRange) {
        println("Server does not support partial downloads")
        return false
    }

    println("Total file size: ${fileInfo.length} bytes")

    // Create/reset the file
    outputFile.outputStream().use { /* just create */ }

    var startByte = 0L
    var partNumber = 1
    // Pretend that the file is slightly larger, so any remainder is guaranteed to turn into a new part)
    val totalParts = (fileInfo.length + partSizeBytes - 1) / partSizeBytes

    while (startByte < fileInfo.length) {
        val size = minOf(partSizeBytes, fileInfo.length - startByte)
        println("Downloading part $partNumber/$totalParts (bytes $startByte-${startByte + size - 1})")
        downloadPart(client, url, outputFile, startByte, size, chunksPerPart)
        startByte += size
        partNumber++
    }

    println("File successfully downloaded to $outputFile")
    return true
}

fun main() {
    val client = OkHttpClient()
    val url = "http://localhost:8080/test3_file.pdf"
    val outputFile = File("downloaded_file.pdf")

    val success = downloadFileInChunks(
        client = client,
        url = url,
        outputFile = outputFile,
        partSizeBytes = 10 * 1024 * 1024, // 10 MB per part
        chunksPerPart = 4
    )

    if (success) {
        println("Download completed successfully")
    } else {
        println("Download failed")
    }

}