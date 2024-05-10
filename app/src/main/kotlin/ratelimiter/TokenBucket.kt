package ratelimiter


import arrow.core.*
import arrow.core.raise.either
import kotlin.time.Duration
import kotlin.math.*
import arrow.fx.coroutines.unit


sealed interface Error {
    data object BucketEmpty:Error
    data object RequestTsInvalid:Error
    data object BucketNotFound:Error
    data object SavingError:Error
}

typealias Res<T> = Either<Error, T>

data class Bucket(val size:Int, val refill:Duration, val count:Int, val ts:Long) {
    fun request(ts:Long):Res<Bucket> {
        val tokens = (ts - this.ts) % refill.inWholeMilliseconds
        val count = min(size, (this.count + tokens).toInt())
        return when {
            ts < this.ts -> Error.RequestTsInvalid.left()
            count == 0 -> Error.BucketEmpty.left()
            else -> this.copy(count = count -1, ts = ts).right()
        }
    }
}

data class Hash(val value:String)

interface TokenBucket {
    suspend fun request(hash:Hash, ts:Long):Res<Bucket>
    suspend fun get(hash:Hash):Res<Bucket>
    suspend fun delete(hash:Hash):Res<Unit>
    suspend fun set(hash:Hash, bucket:Bucket):Res<Bucket>
}

class InMemoryTokenBuket:TokenBucket{

    private val map = mutableMapOf<Hash, Bucket>()
    private fun _get(hash: Hash):Res<Bucket>{
        return map.get(hash)
            ?.right()
            ?:Error.BucketNotFound.left()
    }
    override suspend fun request(hash: Hash, ts: Long):Res<Bucket> {
        return _get(hash).flatMap { it.request(ts).flatMap { v -> set(hash, v) } }
    }

    override suspend fun get(hash: Hash): Res<Bucket> {
        return _get(hash)
    }

    override suspend fun delete(hash: Hash): Res<Unit> {
        map.remove(hash)
        return Unit.right()
    }

    override suspend fun set(hash: Hash, bucket: Bucket): Res<Bucket> {
       return map.put(hash, bucket)
           ?.right()
           ?:Error.SavingError.left()
    }

}