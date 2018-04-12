package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.*
import org.junit.Test

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineCancellationTimeOutTests {

    @Test
    fun testCancelWhileSleepingTwiceASecond() = runBlocking {
        val job = launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")

        job.cancel()    // Cancel the job
        job.join()      // Wait to complete

        println("main: Now I can quit.")
    }


    @Test
    fun testComputationCodeCancellable() = runBlocking {
        val startTime = System.currentTimeMillis()

        val job = launch {
            var nextPrintTime = startTime
            var i = 0

            while (isActive) {
                /*
                NOTE: isActive is a Coroutine property!
                NOTE: Changed on job.cancelAndJoin().
                NOTE: Stops immediately
                Output for isActive:

                I'm sleeping 0 ...
                I'm sleeping 1 ...
                I'm sleeping 2 ...
                main: I'm tired of waiting!
                main: Now I can quit.

                Output for i <= 5:
                NOTE: Means its get completed!

                I'm sleeping 0 ...
                I'm sleeping 1 ...
                I'm sleeping 2 ...
                main: I'm tired of waiting!
                I'm sleeping 3 ...
                I'm sleeping 4 ...
                I'm sleeping 5 ...
                main: Now I can quit.

                 */

                // Print a message twice a second
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("I'm sleeping ${i++} ...")
                    nextPrintTime += 500L
                }
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")

        job.cancelAndJoin() // Cancel job and wait for completion
        println("main: Now I can quit.")
    }


    @Test
    fun testClosingResource() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                println("I'm running finally")
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")

        job.cancelAndJoin()
        println("main: Now I can quit.")
    }


    @Test
    fun testNonCancellableBlock() = runBlocking {
        val job = launch {
            try {
                repeat(1000) { i ->
                    println("I'm sleeping $i ...")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    println("I'm running finally")
                    delay(1000L)
                    println("And delayed 1 sec! Non-cancellable FTW!")
                }
            }
        }
        delay(1300L)
        println("main: I'm tired of waiting!")

        job.cancelAndJoin()
        println("main: Now I can quit.")
    }


    @Test
    fun testTimeOut() = runBlocking {
        // Throws kotlinx.coroutines.experimental.TimeoutCancellationException:
        // Message: Timed out waiting for 1300 MILLISECONDS
        withTimeout(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }
    }


    @Test
    fun testTimeOutOrNull() = runBlocking {
        val result = withTimeoutOrNull(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
            "Done" // Will cancel before returned
        }
        println("Result is $result")
    }


    @Test
    fun testTimeOutOrNullResult() = runBlocking {
        val result = withTimeoutOrNull(5300L) {
            repeat(3) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
            "Done" // Will return when finished
        }
        println("Result is $result")
    }

}