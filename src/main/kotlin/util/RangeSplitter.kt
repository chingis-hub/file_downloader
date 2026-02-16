package com.chingis.util

fun splitToRanges(contentLength: Long, parts: Int): List<LongRange> {
    require(parts > 0) { "Parts must be greater than zero" }
    require(contentLength > 0) { "Content length must be greater than zero" }

    val result = mutableListOf<LongRange>()
    val chunkSize = contentLength / parts
    val remainder = contentLength % parts
    var start = 0L

    for (i in 0 until parts) {
        // the end of each range
        var end = start + chunkSize - 1
        // the last part gets the remainder
        if (i == parts - 1) {
            end += remainder
        }
        // add the range to the list
        result.add(start..end)

        // shift the beginning of the next part
        start = end + 1
    }
    return result
}
