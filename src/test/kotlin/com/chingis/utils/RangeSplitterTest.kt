package com.chingis.utils

import utils.splitToRanges
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RangeSplitterTest {

    @Test
    fun `test splitting small file`() {
        val contentLength = 512 * 1024L // 512 KB
        val ranges = splitToRanges(contentLength, 4, minChunkSize = 1024 * 1024L)
        
        // Small file should only have 1 range due to 1MB minimum chunk size
        assertEquals(1, ranges.size)
        assertEquals(0L..524287L, ranges[0]) // 512 * 1024 - 1
    }

    @Test
    fun `test splitting large file with exact parts`() {
        val contentLength = 4 * 1024 * 1024L // 4 MB
        val ranges = splitToRanges(contentLength, 4, minChunkSize = 1024 * 1024L)
        
        assertEquals(4, ranges.size)
        assertEquals(0L..1048575L, ranges[0])
        assertEquals(1048576L..2097151L, ranges[1])
        assertEquals(2097152L..3145727L, ranges[2])
        assertEquals(3145728L..4194303L, ranges[3])
    }

    @Test
    fun `test splitting large file with more parts than allowed`() {
        val contentLength = 2 * 1024 * 1024L // 2 MB
        val ranges = splitToRanges(contentLength, 10, minChunkSize = 1024 * 1024L)
        
        // With 1MB min size, it should only split into 2 parts
        assertEquals(2, ranges.size)
        assertEquals(0L..1048575L, ranges[0])
        assertEquals(1048576L..2097151L, ranges[1])
    }

    @Test
    fun `test zero length`() {
        val ranges = splitToRanges(0L, 4)
        assertTrue(ranges.isEmpty())
    }

    @Test
    fun `test negative length`() {
        val ranges = splitToRanges(-100L, 4)
        assertTrue(ranges.isEmpty())
    }
}
