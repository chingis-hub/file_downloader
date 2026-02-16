package com.chingis.downloader

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

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