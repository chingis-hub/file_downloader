package utils

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Executes a suspending block with a retry mechanism and exponential backoff.
 * 
 * @param maxRetries The maximum number of retry attempts.
 * @param initialDelayMs The initial delay between retries.
 * @param factor The multiplier for the exponential backoff.
 * @param block The suspending block of code to execute.
 * @return The result of the block if successful.
 * @throws Exception The last exception encountered if all retry attempts fail.
 */
suspend fun <T> withRetry(
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelayMs
    var lastException: Exception? = null

    repeat(maxRetries + 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            lastException = e
            if (attempt == maxRetries) {
                throw e
            }
            
            // Only retry on potential transient errors
            if (e !is IOException && e !is CancellationException) {
                throw e
            }
            
            if (e is CancellationException) {
                throw e
            }

            println("Attempt ${attempt + 1} failed: ${e.message}. Retrying in ${currentDelay}ms...")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong()
        }
    }
    throw lastException ?: IOException("Unknown error during retry")
}
