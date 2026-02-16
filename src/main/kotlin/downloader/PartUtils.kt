package com.chingis.downloader

import com.chingis.util.splitToRanges
import okhttp3.OkHttpClient
import java.io.File

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