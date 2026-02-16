package unit_tests

import com.chingis.util.splitToRanges
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class SplitToRangesTests {

    @Test
    fun `splitToRanges divides file correctly`() {
        // Given
        val contentLength = 100L
        val parts = 4

        // When
        val ranges = splitToRanges(contentLength, parts)

        // Then
        assertEquals(listOf(0L..24L, 25L..49L, 50L..74L, 75L..99L), ranges)
    }

    @Test
    fun `splitToRanges handles single part correctly`() {
        val contentLength = 100L
        val parts = 1

        val ranges = splitToRanges(contentLength, parts)

        assertEquals(listOf(0L..99L), ranges)
    }

    @Test
    fun `splitToRanges handles uneven division correctly`() {
        val contentLength = 11L
        val parts = 3

        val ranges = splitToRanges(contentLength, parts)

        assertEquals(listOf(0L..2L, 3L..5L, 6L..10L), ranges)
    }

}