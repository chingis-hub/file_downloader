# JB File Downloader

A lightweight, high-performance file downloader written in Kotlin, featuring parallel chunked downloading and adaptive throughput logic.

## Features

- **Parallel Downloading**: Splits large files into multiple chunks and downloads them concurrently using Kotlin Coroutines and Ktor.
- **Adaptive Throughput**: Automatically adjusts chunk sizes based on real-time network conditions for optimal performance.
- **Retry Mechanism**: Built-in retry logic with exponential backoff for handling transient network failures and improving download reliability.
- **Smart Chunk Management**: Enforces minimum chunk sizes (64KB) to prevent excessive fragmentation and optimize performance.
- **Direct Disk Streaming**: Uses `RandomAccessFile` and NIO `FileChannel` to stream data directly to disk, minimizing memory overhead.
- **Command Line Interface (CLI)**: Easy-to-use CLI for downloading any URL with configurable options.
- **Smart Filename Detection**: Automatically extracts the filename from the URL if not explicitly provided.
- **Robust Fallback System**: Automatically falls back to standard single-stream downloading if the server does not support `Range` requests or content length is unavailable.

## Technical Details

### Default Configuration
- **Default Parallel Chunks**: 4 concurrent download streams
- **Initial Part Size**: 1 MB (automatically adapts based on network performance)
- **Minimum Chunk Size**: 64 KB (prevents excessive fragmentation)
- **Retry Attempts**: Up to 3 retries with exponential backoff (1s, 2s, 4s delays)
- **Supported Protocols**: HTTP and HTTPS

### Error Handling
- Automatic retry on transient network failures (IOException)
- Graceful fallback to single-stream download when servers don't support range requests
- Smart detection of missing content-length headers
- Comprehensive error reporting and logging

## Requirements

- **Java Development Kit (JDK)**: Version 21 or higher.
- **Internet Connection**: For downloading files.

## How to Build

Use the Gradle wrapper to build the project:

```powershell
.\gradlew.bat build
```

## How to Run

You can run the application directly using Gradle:

### Show Help
```powershell
.\gradlew.bat run --args="--help"
```

### Basic Download
```powershell
.\gradlew.bat run --args="https://example.com/file.zip"
```

### Download with Custom Filename
```powershell
.\gradlew.bat run --args="https://example.com/file.zip my_file.zip"
```

### Download with Configurable Parallelism
Download using 8 parallel chunks:
```powershell
.\gradlew.bat run --args="https://example.com/file.zip my_file.zip 8"
```

## Running Tests

The project includes a comprehensive suite of unit and integration tests (including CLI tests).

To run all tests:
```powershell
.\gradlew.bat test
```

## Architecture

The JB File Downloader follows a modular, layered architecture designed for high performance, reliability, and maintainability:

### Core Components

#### 1. **Entry Layer**
- **`Main.kt`**: Application entry point that initializes the HTTP client and delegates to CLI runner
- **`CliRunner.kt`**: Command-line interface handler that parses arguments and orchestrates downloads

#### 2. **Coordination Layer**
- **`DownloadCoordinator.kt`**: Main orchestrator that determines download strategy based on server capabilities
  - Fetches file metadata using HEAD requests
  - Decides between parallel or fallback download modes
  - Handles server compatibility (range support, content-length availability)

#### 3. **Download Engine Layer**
- **`ParallelDownloader.kt`**: High-performance parallel download engine
  - Implements adaptive chunking with real-time throughput optimization
  - Uses `RandomAccessFile` and NIO `FileChannel` for direct disk streaming
  - Manages concurrent coroutines for parallel chunk downloads
  - Features adaptive part sizing (64KB - 10MB) based on network performance
- **`FallbackDownloader.kt`**: Single-stream download fallback for incompatible servers
- **`FileInfoFetcher.kt`**: Metadata retrieval service using HTTP HEAD requests

#### 4. **Utility Layer**
- **`RangeSplitter.kt`**: Smart range calculation utility
  - Splits content into optimal chunks while respecting minimum chunk sizes
  - Prevents excessive fragmentation for small files
- **`RetryUtils.kt`**: Resilience utility providing exponential backoff retry mechanism
  - Handles transient network failures (IOException)
  - Configurable retry attempts and delay factors

#### 5. **Data Models**
- **`FileInfo.kt`**: Simple data class encapsulating file metadata (size, range support)

### Design Patterns & Principles

- **Strategy Pattern**: Download strategy selection (parallel vs. fallback)
- **Adapter Pattern**: HTTP client abstraction for different download modes
- **Coroutine-based Concurrency**: Structured concurrency with proper resource management
- **Streaming I/O**: Direct-to-disk streaming to minimize memory footprint
- **Adaptive Algorithms**: Dynamic part sizing based on real-time performance metrics
- **Fail-Fast with Graceful Degradation**: Quick fallback to compatible download modes

### Data Flow

1. **Initialization**: CLI parses arguments → HTTP client setup
2. **Discovery**: HEAD request fetches file metadata (size, range support)
3. **Strategy Selection**: Coordinator chooses parallel or fallback mode
4. **Parallel Mode**: File split into adaptive parts → concurrent chunk downloads → direct disk streaming
5. **Fallback Mode**: Single-stream download for incompatible servers
6. **Resilience**: Automatic retries with exponential backoff on failures

## Testing

The project includes a comprehensive testing suite covering multiple testing levels:

### Test Structure

#### 1. **Unit Tests**
- **`DownloadCoordinatorTest.kt`**: Tests download strategy selection logic
- **`FileInfoFetcherTest.kt`**: Tests metadata retrieval and error handling
- **`ParallelDownloaderTest.kt`**: Tests parallel download mechanics and adaptive algorithms
- **`RangeSplitterTest.kt`**: Tests range calculation algorithms and edge cases
- **`RetryTest.kt`**: Tests retry mechanism with various failure scenarios

#### 2. **Integration Tests**
- **`DownloaderIntegrationTest.kt`**: End-to-end tests using embedded HTTP server
  - Tests complete download flow with real HTTP interactions
  - Verifies range request handling and partial content responses
  - Tests adaptive chunking behavior with different file sizes

#### 3. **CLI Tests**
- **`CliTest.kt`**: Tests command-line interface parsing and validation
  - Argument parsing and validation
  - Help message generation
  - Error handling for invalid inputs


### Test Technologies

- **Kotlin Test**: Primary testing framework with coroutine support
- **Embedded Ktor Server**: For realistic HTTP server simulation in integration tests
- **Temporary File Management**: Safe test file creation and cleanup
- **Coroutine Testing**: Proper async/await testing patterns

## Technologies Used

- **Kotlin**: Core programming language with coroutine support.
- **Ktor Client**: For handling HTTP requests and streaming.
- **Kotlin Coroutines**: For high-concurrency management and structured concurrency.
- **NIO FileChannel**: For high-performance direct disk I/O.
- **Kotlin Test & JUnit 5**: For comprehensive unit and integration testing.
- **Embedded Ktor Server**: For realistic integration testing.
- **Gradle**: Build automation system with Kotlin DSL.
