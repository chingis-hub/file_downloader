package com.chingis.downloader

import downloader.getFileInfo
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FileInfoFetcherTest {

    @Test
    fun `getFileInfo returns correct info on successful head request`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentLength to listOf("1024"),
                    HttpHeaders.AcceptRanges to listOf("bytes")
                )
            )
        }
        val client = HttpClient(mockEngine)
        
        val fileInfo = getFileInfo(client, "http://example.com/file")
        
        assertNotNull(fileInfo)
        assertEquals(1024L, fileInfo.length)
        assertTrue(fileInfo.supportsRange)
    }

    @Test
    fun `getFileInfo returns null on failing head request`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.NotFound
            )
        }
        val client = HttpClient(mockEngine)
        
        val fileInfo = getFileInfo(client, "http://example.com/file")
        
        assertNull(fileInfo)
    }

    @Test
    fun `getFileInfo correctly reports missing range support`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentLength to listOf("1024")
                )
            )
        }
        val client = HttpClient(mockEngine)
        
        val fileInfo = getFileInfo(client, "http://example.com/file")
        
        assertNotNull(fileInfo)
        assertEquals(1024L, fileInfo.length)
        assertEquals(false, fileInfo.supportsRange)
    }
}
