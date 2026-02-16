package unit_tests

import com.chingis.downloadChunk
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.IOException

class DownloadChunkTests {

    // lateinit — because initialize them in @beforeEach
    private lateinit var mockServer: MockWebServer
    private lateinit var client: OkHttpClient

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        client = OkHttpClient()
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `downloadChunk returns bytes for 206 Partial Content`() {
        val expectedText = "Hello Partial"
        val expectedBytes = expectedText.toByteArray()

        // Server return 206
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setBody(expectedText)
        )

        val url = mockServer.url("/file").toString()
        // Server will return "Hello Partial" regardless of the set range
        val range = 0L..10L
        val result = downloadChunk(client, url, range)

        assertArrayEquals(expectedBytes, result)
    }

    @Test
    // Server ignored the Range and returned the entire file
    fun `downloadChunk throws IOException for 200 OK`() {
        // Server return 200 instead of 206
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("Hello Full")
        )

        val url = mockServer.url("/file").toString()
        val range = 0L..10L

        val exception = assertThrows<IOException> {
            downloadChunk(client, url, range)
        }

        assertTrue(exception.message!!.contains("Server does not support a partial content"))
    }

    @Test
    fun `downloadChunk throws IOException for empty body`() {
        // Server return 206 but the body is empty
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(206)
        )

        val url = mockServer.url("/file").toString()
        val range = 0L..10L

        val exception = assertThrows<IOException> {
            downloadChunk(client, url, range)
        }

        assertTrue(exception.message!!.contains("Empty response"))
    }
}