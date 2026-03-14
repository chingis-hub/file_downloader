package com.chingis.utils

import downloader.getFileInfo
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import utils.withRetry
import java.io.IOException
import kotlin.test.assertFailsWith

class RetryTest {

    @Test
    fun `withRetry actually retries given number of times`() = runBlocking {
        var attempts = 0

        assertFailsWith<IOException> {
            withRetry(maxRetries = 2, initialDelayMs = 10) {
                attempts++
                throw IOException("Test failure")
            }
        }

        assertEquals(3, attempts, "Should have attempted 3 times (1 initial + 2 retries)")
    }

    @Test
    fun `withRetry returns value on success`() = runBlocking {
        var attempts = 0
        val result = withRetry(maxRetries = 2, initialDelayMs = 10) {
            attempts++
            if (attempts < 2) throw IOException("Fail once")
            "Success"
        }
        
        assertEquals("Success", result)
        assertEquals(2, attempts)
    }

    @Test
    fun `getFileInfo retries on failure`() = runBlocking {
        var attempts = 0
        val mockEngine = MockEngine { request ->
            attempts++
            if (attempts < 2) {
                respondError(HttpStatusCode.InternalServerError)
            } else {
                respond(
                    content = "",
                    status = HttpStatusCode.OK,
                    headers = headersOf(
                        HttpHeaders.ContentLength to listOf("1024"),
                        HttpHeaders.AcceptRanges to listOf("bytes")
                    )
                )
            }
        }
        val client = HttpClient(mockEngine)
        
        val fileInfo = getFileInfo(client, "http://example.com/file")
        
        assertNotNull(fileInfo)
        assertEquals(2, attempts, "Should have attempted 2 times")
        assertEquals(1024L, fileInfo?.length)
    }

    @Test
    fun `getFileInfo eventually fails after max retries`() = runBlocking {
        var attempts = 0
        val mockEngine = MockEngine { request ->
            attempts++
            respondError(HttpStatusCode.InternalServerError)
        }
        val client = HttpClient(mockEngine)
        
        val fileInfo = getFileInfo(client, "http://example.com/file")
        
        // getFileInfo uses maxRetries = 2, which means 1 initial + 2 retries = 3 attempts total
        assertEquals(3, attempts, "Should have attempted 3 times (1 initial + 2 retries)")
        assertEquals(null, fileInfo)
    }
}
