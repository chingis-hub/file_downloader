package com.chingis.downloader

import downloader.downloadFile
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

/**
 * End-to-end tests for `downloadFile` using a real HTTP server.
 * Verifies full download flow, range support, and adaptive chunking.
 */
class DownloaderIntegrationTest {

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val port = 8080
    private val testData = "This is some test data for integration testing. It should be long enough to be split into multiple parts and chunks if we want to test that logic thoroughly.".toByteArray()

    @BeforeTest
    fun setup() {
        server = embeddedServer(Netty, port = port) {
            routing {
                head("/testfile") {
                    call.response.header(HttpHeaders.ContentLength, testData.size.toLong())
                    call.response.header(HttpHeaders.AcceptRanges, "bytes")
                    call.respond(HttpStatusCode.OK)
                }
                get("/testfile") {
                    val rangeHeader = call.request.headers[HttpHeaders.Range]
                    if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
                        val range = rangeHeader.removePrefix("bytes=").split("-")
                        val start = range[0].toInt()
                        val end = if (range[1].isEmpty()) testData.size - 1 else range[1].toInt()

                        val requestedData = testData.sliceArray(start..end)
                        call.respondBytes(requestedData, status = HttpStatusCode.PartialContent)
                    } else {
                        call.respondBytes(testData)
                    }
                }
            }
        }.start(wait = false)
    }

    @AfterTest
    fun tearDown() {
        server.stop(1000, 1000)
    }

    @Test
    fun `downloadFile performs full download from real server`() = runBlocking {
        val outputFile = Files.createTempFile("integration_test", ".tmp").toFile()
        val client = HttpClient(CIO)
        val url = "http://localhost:$port/testfile"

        try {
            val success = downloadFile(
                client = client,
                url = url,
                outputFile = outputFile,
                partSizeBytes = 32, // Small parts to force multiple parts
                chunksPerPart = 2
            )

            assertTrue(success, "Download should be successful")
            assertContentEquals(testData, outputFile.readBytes(), "Downloaded content should match original")
        } finally {
            client.close()
            outputFile.delete()
        }
    }

    @Test
    fun `downloadFile works with large part size`() = runBlocking {
        val outputFile = Files.createTempFile("integration_test_large", ".tmp").toFile()
        val client = HttpClient(CIO)
        val url = "http://localhost:$port/testfile"

        try {
            val success = downloadFile(
                client = client,
                url = url,
                outputFile = outputFile,
                partSizeBytes = 1024, // Larger than file size
                chunksPerPart = 4
            )

            assertTrue(success, "Download should be successful")
            assertContentEquals(testData, outputFile.readBytes(), "Downloaded content should match original")
        } finally {
            client.close()
            outputFile.delete()
        }
    }
}
