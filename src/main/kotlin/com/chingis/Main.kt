package com.chingis

// Ktor - async framework
import io.ktor.client.*
import io.ktor.client.engine.cio.* // CIO = Coroutine I/O engine
import kotlinx.coroutines.runBlocking


fun main(args: Array<String>) = runBlocking {
    val client = HttpClient(CIO) // HTTP client is a tool that allows a program to communicate with web servers via HTTP
    try {
        runCli(args, client)
    } finally {
        client.close()
    }
}
