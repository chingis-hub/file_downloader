# File Downloader

A Kotlin application for efficiently downloading files in chunks with parallel processing support.

## Features

- Download large files in manageable parts
- Parallel downloading with multiple threads
- Automatic detection of server range support
- Resume interrupted downloads
- Progress tracking during download

## Requirements

- Java 21 or higher
- Kotlin 2.1.10
- OkHttp 5.0.0-alpha.14

## Installation

1. Clone the repository:
   ```
   git clone git@github.com:chingis-hub/file_downloader.git
   cd file_downloader
   ```

2. Build the project:
   ```
   ./gradlew build
   ```

## Usage

Modify the `main` function in `Main.kt` to specify your download URL and output file:

```kotlin
val outputFile = File("downloaded_file.pdf")
```

### Configuration Options

- `partSizeBytes`: Size of each download part in bytes (default: 10MB)
- `chunksPerPart`: Number of parallel chunks to download for each part (default: 4)

## How It Works

The downloader operates in the following steps:

1. Sends a HEAD request to get file information (size and range support)
2. Divides the file into parts based on the specified part size
3. For each part, creates multiple chunks for parallel downloading
4. Downloads all chunks in parallel using separate threads
5. Assembles the chunks into the final file

### Key Components

- `getFileInfo()`: Retrieves file size and checks if the server supports range requests
- `splitToRanges()`: Divides content into downloadable ranges
- `downloadAllChunks()`: Manages parallel downloading of chunks
- `downloadPart()`: Downloads a specific part of the file
- `downloadFileInChunks()`: Main function that orchestrates the entire download process

## License

[MIT License](LICENSE)