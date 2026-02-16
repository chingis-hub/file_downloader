import okhttp3.OkHttpClient
import org.junit.jupiter.api.Test
import java.io.File
import java.security.MessageDigest
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import com.chingis.downloader.downloadFileInChunks

class FileDownloadIntegrationTest {

    private val client = OkHttpClient()
    private val testUrl = "http://localhost:8080/test2_file.pdf"
    private val outputFile = File("test_downloaded_file.pdf")
    private val originalFile = File("src/test/resources/test2_file.pdf") // local copy for checking

    private fun fileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8 * 1024)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    @Test
    fun `downloadFileInChunks downloads full file correctly`() {
        if (outputFile.exists()) outputFile.delete()

        val success = downloadFileInChunks(
            client = client,
            url = testUrl,
            outputFile = outputFile,
            partSizeBytes = 5 * 1024 * 1024, // 5 MB
            chunksPerPart = 5
        )

        assertTrue(success, "Download should complete successfully")
        assertTrue(outputFile.exists(), "Output file should exist")

        val originalHash = fileSha256(originalFile)
        val downloadedHash = fileSha256(outputFile)

        assertEquals(originalHash, downloadedHash, "Downloaded file should match original file")
    }
}
