package utils

fun splitToRanges(contentLength: Long, parts: Int, minChunkSize: Long = 64 * 1024L): List<LongRange> {
    if (contentLength <= 0) return emptyList()
    
    val effectiveParts = minOf(parts.toLong(), (contentLength + minChunkSize - 1) / minChunkSize).toInt().coerceAtLeast(1)

    val chunkSize = contentLength / effectiveParts
    val result = mutableListOf<LongRange>()
    
    for (i in 0 until effectiveParts) {
        val start = i * chunkSize
        val end = if (i == effectiveParts - 1) contentLength - 1 else (i + 1) * chunkSize - 1
        result.add(start..end)
    }
    return result
}
