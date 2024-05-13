package ratelimiter

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class TokenBucketTest {

    @Test
    fun consume_success() {
        val tb = TokenBucket(2, 1.seconds, 0, 10 * 1000)
        val result = tb.consume(2, 12 * 1000)
        assertTrue(result.isRight())
    }

    @Test
    fun consume_fail() {
        val tb = TokenBucket(2, 1.seconds, 0, 10 * 1000)
        val result = tb.consume(2, 11 * 1000)
        assertTrue(result.isLeft())
    }
}