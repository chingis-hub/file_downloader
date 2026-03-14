package com.chingis

import downloader.downloadFile
import io.ktor.client.*
import java.io.File

suspend fun runCli(args: Array<String>, client: HttpClient) {
    if (args.isEmpty() || args.contains("--help") || args.contains("-h")) {
        println("""
            Usage: jb_file_downloader <URL> [OUTPUT_FILE] [CHUNKS_PER_PART]

            Arguments:
              URL                The URL of the file to download (required)
              OUTPUT_FILE        The name of the file to save (optional, default: extracted from URL or 'downloaded_file')
              CHUNKS_PER_PART    Number of parallel chunks to download (optional, default: 4)

            Options:
              --help, -h         Show this help message
        """.trimIndent())
        return
    }

    val url = args[0]

    val defaultFileName = url.substringAfterLast('/').substringBefore('?').ifEmpty { "downloaded_file" }
    val outputFileName = args.getOrNull(1) ?: defaultFileName
    val outputFile = File(outputFileName)

    val chunksPerPart = args.getOrNull(2)?.toIntOrNull() ?: 4

    println("Starting download of $url")
    println("Output file: ${outputFile.absolutePath}")
    println("Parallel chunks: $chunksPerPart")

    val success = downloadFile(
        client = client,
        url = url,
        outputFile = outputFile,
        partSizeBytes = 1 * 1024 * 1024, // 1 MB initial part size (will adapt)
        chunksPerPart = chunksPerPart
    )

    if (success) {
        println("\nDownload completed successfully")
    } else {
        println("\nDownload failed")
    }
}
