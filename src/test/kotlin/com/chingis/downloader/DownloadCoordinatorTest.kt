package com.chingis.downloader

import downloader.downloadFile
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

/**
 * Integration tests for the main download coordinator.
 * Tests strategy selection, fallback scenarios, and adaptive behavior.
 */
class DownloadCoordinatorTest {

    @Test
    fun `test downloadFile strategy selection`() = runBlocking {
        val testFile = Files.createTempFile("preallocation_test", ".tmp").toFile()
        val fileSize = 5 * 1024 * 1024L // 5 MB

        try {
            val mockEngine = MockEngine { request ->
                if (request.method == HttpMethod.Head) {
                    respond(
                        content = "",
                        headers = headersOf(
                            HttpHeaders.ContentLength to listOf(fileSize.toString()),
                            HttpHeaders.AcceptRanges to listOf("bytes")
                        )
                    )
                } else {
                    // For simplicity, just return empty data for GET
                    respond(
                        content = ByteArray((request.headers[HttpHeaders.Range]?.split("-")?.let { it[1].toLong() - it[0].split("=")[1].toLong() + 1 } ?: 0).toInt()),
                        status = HttpStatusCode.PartialContent
                    )
                }
            }
            val client = HttpClient(mockEngine)

            // We only care about whether it sets the length
            downloadFile(
                client = client,
                url = "http://example.com/file",
                outputFile = testFile,
                partSizeBytes = 1024 * 1024,
                chunksPerPart = 1
            )

            assertEquals(fileSize, testFile.length())

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `test downloadFile fallback when range not supported`() = runBlocking {
        val testFile = Files.createTempFile("fallback_test", ".tmp").toFile()
        val fileContent = "This is a full file content without range support"

        try {
            val mockEngine = MockEngine { request ->
                if (request.method == HttpMethod.Head) {
                    respond(
                        content = "",
                        headers = headersOf(
                            HttpHeaders.ContentLength to listOf(fileContent.length.toString())
                            // No Accept-Ranges header
                        )
                    )
                } else {
                    respond(
                        content = fileContent,
                        status = HttpStatusCode.OK
                    )
                }
            }
            val client = HttpClient(mockEngine)

            val result = downloadFile(
                client = client,
                url = "http://example.com/file",
                outputFile = testFile,
                partSizeBytes = 1024,
                chunksPerPart = 1
            )

            assertTrue(result)
            assertEquals(fileContent, testFile.readText())

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `test downloadFile fallback when length missing`() = runBlocking {
        val testFile = Files.createTempFile("fallback_no_length_test", ".tmp").toFile()
        val fileContent = "Content without length"

        try {
            val mockEngine = MockEngine { request ->
                if (request.method == HttpMethod.Head) {
                    respond(
                        content = "",
                        headers = headersOf(
                            HttpHeaders.AcceptRanges to listOf("bytes")
                            // No Content-Length header
                        )
                    )
                } else {
                    respond(
                        content = fileContent,
                        status = HttpStatusCode.OK
                    )
                }
            }
            val client = HttpClient(mockEngine)

            val result = downloadFile(
                client = client,
                url = "http://example.com/file",
                outputFile = testFile,
                partSizeBytes = 1024,
                chunksPerPart = 1
            )

            assertTrue(result)
            assertEquals(fileContent, testFile.readText())

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `test downloadFile with adaptive sizing`() = runBlocking {
        val testFile = Files.createTempFile("adaptive_test", ".tmp").toFile()
        val fileSize = 2 * 1024 * 1024L // 2 MB

        try {
            var callCount = 0
            val mockEngine = MockEngine { request ->
                if (request.method == HttpMethod.Head) {
                    respond(
                        content = "",
                        headers = headersOf(
                            HttpHeaders.ContentLength to listOf(fileSize.toString()),
                            HttpHeaders.AcceptRanges to listOf("bytes")
                        )
                    )
                } else {
                    callCount++
                    // First call takes 4 seconds for 1MB -> speed 256KB/s -> target size 512KB
                    // Second call takes 1 second for 512KB -> speed 512KB/s -> target size 1MB
                    val delayMs = if (callCount == 1) 4000L else 1000L
                    kotlinx.coroutines.delay(delayMs)

                    respond(
                        content = ByteArray((request.headers[HttpHeaders.Range]?.split("-")?.let { it[1].toLong() - it[0].split("=")[1].toLong() + 1 } ?: 0).toInt()),
                        status = HttpStatusCode.PartialContent
                    )
                }
            }
            val client = HttpClient(mockEngine)

            // Initial part size 1MB
            downloadFile(
                client = client,
                url = "http://example.com/file",
                outputFile = testFile,
                partSizeBytes = 1024 * 1024,
                chunksPerPart = 1
            )

            // We can't easily assert the internal variable, but we can verify the behavior if we had a way to intercept the logs or if we used a spy.
            // For this test, just ensuring it completes without error is a start.
            // Ideally we'd capture the output.

            assertTrue(testFile.length() == fileSize)
            assertTrue(callCount > 1) // Should have multiple calls due to adaptive sizing

        } finally {
            testFile.delete()
        }
    }
}
