package com.chingis.cli

import com.chingis.runCli
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.*

class CliTest {

    private lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val port = 8081
    private val testData = "test data".toByteArray()
    
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
    fun `test help message`() = runBlocking {
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))
        
        val client = HttpClient(CIO)
        try {
            runCli(arrayOf("--help"), client)
            val output = outContent.toString()
            assertTrue(output.contains("Usage: jb_file_downloader"), "Should show usage message")
            assertTrue(output.contains("--help, -h"), "Should show help option")
        } finally {
            client.close()
            System.setOut(System.`out`)
        }
    }

    @Test
    fun `test default filename detection`() = runBlocking {
        val client = HttpClient(CIO)
        val url = "http://localhost:$port/testfile"
        val expectedFile = File("testfile")
        
        try {
            // Clean up if file already exists
            if (expectedFile.exists()) expectedFile.delete()
            
            runCli(arrayOf(url), client)
            
            assertTrue(expectedFile.exists(), "File should be downloaded with name 'testfile'")
            assertContentEquals(testData, expectedFile.readBytes())
        } finally {
            client.close()
            expectedFile.delete()
        }
    }

    @Test
    fun `test manual output filename`() = runBlocking {
        val client = HttpClient(CIO)
        val url = "http://localhost:$port/testfile"
        val outputName = "custom_name.txt"
        val outputFile = File(outputName)
        
        try {
            if (outputFile.exists()) outputFile.delete()
            
            runCli(arrayOf(url, outputName), client)
            
            assertTrue(outputFile.exists(), "File should be downloaded with name '$outputName'")
            assertContentEquals(testData, outputFile.readBytes())
        } finally {
            client.close()
            outputFile.delete()
        }
    }

    @Test
    fun `test parallel chunks argument`() = runBlocking {
        val outContent = ByteArrayOutputStream()
        val originalOut = System.`out`
        System.setOut(PrintStream(outContent))
        
        val client = HttpClient(CIO)
        val url = "http://localhost:$port/testfile"
        val outputName = "test_chunks.txt"
        val outputFile = File(outputName)
        
        try {
            if (outputFile.exists()) outputFile.delete()
            
            runCli(arrayOf(url, outputName, "8"), client)
            
            val output = outContent.toString()
            assertTrue(output.contains("Parallel chunks: 8"), "Should show 8 parallel chunks")
            assertTrue(outputFile.exists())
        } finally {
            client.close()
            outputFile.delete()
            System.setOut(originalOut)
        }
    }
}
