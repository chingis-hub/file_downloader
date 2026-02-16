package com.chingis.downloader

import com.chingis.model.FileInfo
import okhttp3.OkHttpClient
import okhttp3.Request

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