package com.chingis.downloader

import downloader.downloadRangesInParallel
import downloader.downloadRangeToFileChannel
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.test.*

/**
 * Unit tests for low-level parallel download functions.
 * Tests individual functions in isolation using mocked HTTP responses.
 */
class ParallelDownloaderTest {

    @Test
    fun `test downloadRangeToFileChannel`() = runBlocking {
        val testData = "Hello, World!"
        val testFile = Files.createTempFile("test_chunk", ".tmp").toFile()

        try {
            val mockEngine = MockEngine { request ->
                val rangeHeader = request.headers[HttpHeaders.Range]
                assertNotNull(rangeHeader)
                assertTrue(rangeHeader.startsWith("bytes="))

                respond(
                    content = testData,
                    status = HttpStatusCode.PartialContent
                )
            }
            val client = HttpClient(mockEngine)

            RandomAccessFile(testFile, "rw").use { raf ->
                raf.setLength(testData.length.toLong())
                val channel = raf.channel
                downloadRangeToFileChannel(
                    client = client,
                    url = "http://example.com/file",
                    range = 0L..testData.length.toLong() - 1,
                    fileChannel = channel,
                    filePosition = 0L
                )
            }

            val downloadedData = testFile.readText()
            assertEquals(testData, downloadedData)

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `test downloadRangesInParallel`() = runBlocking {
        val chunk1 = "Part1"
        val chunk2 = "Part2"
        val testFile = Files.createTempFile("test_multi_chunk", ".tmp").toFile()

        try {
            val mockEngine = MockEngine { request ->
                val rangeHeader = request.headers[HttpHeaders.Range]
                val responseContent = if (rangeHeader == "bytes=0-4") chunk1 else chunk2
                respond(
                    content = responseContent,
                    status = HttpStatusCode.PartialContent
                )
            }
            val client = HttpClient(mockEngine)

            RandomAccessFile(testFile, "rw").use { raf ->
                raf.setLength(10L)
                val channel = raf.channel
                downloadRangesInParallel(
                    client = client,
                    url = "http://example.com/file",
                    ranges = listOf(0L..4L, 5L..9L),
                    fileChannel = channel,
                    startPosition = 0L
                )
            }

            val downloadedData = testFile.readText()
            assertEquals("Part1Part2", downloadedData)

        } finally {
            testFile.delete()
        }
    }

    @Test
    fun `test downloadRangeToFileChannel with offset`() = runBlocking {
        val testData = "Data"
        val testFile = Files.createTempFile("test_offset", ".tmp").toFile()

        try {
            val mockEngine = MockEngine { request ->
                respond(
                    content = testData,
                    status = HttpStatusCode.PartialContent
                )
            }
            val client = HttpClient(mockEngine)

            RandomAccessFile(testFile, "rw").use { raf ->
                raf.setLength(10L)
                val channel = raf.channel
                downloadRangeToFileChannel(
                    client = client,
                    url = "http://example.com/file",
                    range = 0L..3L,
                    fileChannel = channel,
                    filePosition = 5L // Write starting at index 5
                )
            }

            val fileBytes = testFile.readBytes()
            assertEquals(testData[0].code.toByte(), fileBytes[5])
            assertEquals(testData[3].code.toByte(), fileBytes[8])

        } finally {
            testFile.delete()
        }
    }
}
