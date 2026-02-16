package com.chingis

import com.chingis.downloader.downloadFileInChunks
import okhttp3.OkHttpClient
import java.io.File


fun main() {
    val client = OkHttpClient()
    val url = "http://localhost:8080/test2_file.pdf"
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