package unit_tests

import com.chingis.getFileInfo
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class GetFileInfoTests {

    @Test
    fun `getFileInfo returns correct length and supports range`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Length", "1024")
                .setHeader("Accept-Ranges", "bytes")
        )

        val client = OkHttpClient()
        val result = getFileInfo(client, server.url("/").toString())

        assertNotNull(result)
        // Use !! because already check non-null
        assertEquals(1024L, result!!.length)
        assertTrue(result.supportsRange)

        server.shutdown()
    }

    @Test
    fun `getFileInfo returns supportsRange false when header missing`() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setHeader("Content-Length", "2048")
            // no Accept-Ranges
        )

        val client = OkHttpClient()
        val result = getFileInfo(client, server.url("/").toString())

        assertNotNull(result)
        assertEquals(2048L, result!!.length)
        assertFalse(result.supportsRange)

        server.shutdown()
    }

}
