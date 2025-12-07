package jp.wjg.shokkaa

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class RateLimiter @OptIn(ExperimentalTime::class) constructor(
    val interval: Duration,
    val amount: Int = 1,
    val origin: Instant = now(),
) {
    private val tokenChannel = Channel<Unit>(Channel.RENDEZVOUS)

    init {
        CoroutineScope(Dispatchers.Default).launch {
            var nextExecutionTime = origin
            while (isActive) {
                val currentTime = now()
                val passedIntervals = ((currentTime - origin) / interval + 1.0).toInt()
                nextExecutionTime = origin + interval * passedIntervals
                if (currentTime < nextExecutionTime) delay(nextExecutionTime - currentTime)
                tokenChannel.send(Unit)
            }
        }
    }

    suspend fun <T> runRateLimited(block: suspend () -> T): T {
        tokenChannel.receive()
        return block()
    }
}

fun <T> Flow<T>.rateLimited(rateLimiter: RateLimiter): Flow<T> = flow {
    collect { v -> rateLimiter.runRateLimited { emit(v) } }
}.cancellable()

@OptIn(ExperimentalCoroutinesApi::class)
fun <T> Flow<T>.throttle(rateLimiter: RateLimiter): Flow<T> =
    flow { chunked(rateLimiter.amount).collect { rateLimiter.runRateLimited { it.forEach { emit(it) } } } }
