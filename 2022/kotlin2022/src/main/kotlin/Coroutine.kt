import kotlinx.coroutines.delay

suspend fun myDelay(d: Long): Int {
    delay(d)
    return 99
}

class Coroutine {
    suspend fun mySuspendFunc(d: Long): Int {
        println("START")
        val x = myDelay(d)
        println(x)
        return x
    }

/*
    fun func01(var1: Continuation<Any?>): Any {
        class MyCoroutineImpl(completion: Continuation<Any?>?) : ContinuationImpl(completion) {
            var result: Any? = null
            var label = 0
            override fun invokeSuspend(result: Result<Any?>): Any? {
                this.result = result
                label = label or Int.MIN_VALUE
                return this@Coroutine.func01(this as Continuation<Any?>)
            }
        }

        var `$continuation`: Any
        do {
            if (var1 is MyCoroutineImpl) {
                `$continuation` = var1
                if ((`$continuation` as MyCoroutineImpl).label and Int.MIN_VALUE != 0) {
                    (`$continuation` as MyCoroutineImpl).label -= Int.MIN_VALUE
                    break
                }
            }
            `$continuation` = MyCoroutineImpl(var1)
        } while (false)
        val `$result` = (`$continuation` as MyCoroutineImpl).result
        val var5 = COROUTINE_SUSPENDED
        val var10000: Any?
        when ((`$continuation` as MyCoroutineImpl).label) {
            0 -> {
                //`$result`.throwOnFailure()
                println("START")
                (`$continuation` as MyCoroutineImpl).label = 1
                var10000 = func1(`$continuation` as Continuation<*>)
                if (var10000 === var5) {
                    return var5
                }
            }
            1 -> {
                //`$result`.throwOnFailure()
                var10000 = `$result`
            }
            else -> throw IllegalStateException("call to 'resume' before 'invoke' with coroutine")
        }
        val x = (var10000 as Number?)!!.toInt()
        println(x)
        return Unit
    }

 */
}
