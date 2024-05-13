package ratelimiter


import arrow.core.Either
import arrow.core.left
import arrow.core.right
import kotlin.math.min
import kotlin.time.Duration


sealed interface Error {
    data object BucketEmpty : Error
    data object RequestTsInvalid : Error
    data object NotEnoughTokens:Error
}

typealias Res<T> = Either<Error, T>

data class TokenBucket(val size: Int, val refill: Duration, val count: Int, val ts: Long) {
    fun consume(tokens: Int, ts: Long): Res<TokenBucket> {
        val tokensToRefill = (ts - this.ts) / refill.inWholeMilliseconds
        val count = min(size, (this.count + tokensToRefill).toInt())
        return when {
            ts < this.ts -> Error.RequestTsInvalid.left()
            count == 0 -> Error.BucketEmpty.left()
            count < tokens -> Error.NotEnoughTokens.left()
            else -> this.copy(count = count - tokens, ts = ts).right()
        }
    }
}
