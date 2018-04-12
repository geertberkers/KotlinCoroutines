package geert.berkers.kotlincoroutinesunittests

import kotlinx.coroutines.experimental.*
import org.junit.Test

import org.junit.Assert.*
import kotlin.system.measureTimeMillis

/**
 * Local Unit Tests for learning the Kotlin Coroutines Library.
 *
 * See [Kotlin Coroutines GitHub page](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#coroutine-basics).
 */
class KotlinCoroutinesTests {

    @Test
    fun testCoroutinesLearner() {
        launch { // launch new coroutine in background and continue
            delay(1000L) // Non-blocking delay
            println("World!")
        }

        println("Hello,") // Continue on MainThread during coroutine

        // delay(2000) // Cannot be called. Needs suspension
        runBlocking {
            delay(2000L) // Block main thread to keep JVM alive
        }
    }


    @Suppress("RemoveExplicitTypeArguments")
    @Test
    fun testMySuspendingFunction() = runBlocking<Unit> { // Unit can be removed
        val job = launch { doWorld() } // Launch new coroutine with reference

        println("Hello,")

        job.join() // Wait until completion
    }


    // My first suspending function
    private suspend fun doWorld() {
        println("Wait 1 second")
        delay(250L)
        println("   Wait 0.75 second")
        delay(250L)
        println("   Wait 0.5 second")
        delay(250L)
        println("   Wait 0.25 second")
        delay(250L)

        println("World!")
    }


    @Test
    fun testThousandsOfCoroutines() = runBlocking {
        val jobs = List(100_000) { // Launch a lot of coroutines
            launch {
                delay(1000L)
                print(".")
            }
        }

        jobs.forEach { it.join() } // Wait to complete
    }


    @Test
    fun testSleepingTwiceASecond() = runBlocking {
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


    // Helper functions for the following tests

    private suspend fun doSomethingUsefulOne(): Int {
        delay(1000L) // Pretend this is useful XD
        return 13
    }

    private suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L) // Another useful thing ;)
        return 29
    }

    @Test
    fun testComposingSuspendingFunctions() = runBlocking {
        // NOTE:    Sequential function!
        // Output:  The answer is 42
        //          Completed in 2051 ms

        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }

        println("Completed in $time ms")
    }


    @Test
    fun testAsyncComposingSuspendingFunctions() = runBlocking {
        // NOTE:    Concurrent function!
        // Output:  The answer is 42
        //          Completed in 1046 ms

        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }

        println("Completed in $time ms")
    }


    @Test
    fun testLazyStartedAsync() = runBlocking {
        // NOTE:    Lazy function!
        // Optional start parameter with CoroutineStart.LAZY
        // It only starts when the result is needed by some await or if start is invoked.

        // Output:  The answer is 42
        //          Completed in 1046 ms

        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }

        println("Completed in $time ms")
    }

    
    @Test
    fun testAsyncStyleFunctions() {
        val time = measureTimeMillis {

            // Initiate async outside of a coroutine
            val one = somethingUsefulOneAsync() // Return Deferred<Int>
            val two = somethingUsefulTwoAsync() // Return Deferred<Int>

            // Waiting for result must involve supsend or blocking!
            runBlocking {
                val resultOne = one.await() // Gets Deferred Value
                val resultTwo = two.await() // Gets Deferred Value

                println("The answer is ${resultOne + resultTwo}")
            }
        }

        println("Completed in $time ms")
    }


    private fun somethingUsefulOneAsync() = async { // Returns Deferred<Int>
        doSomethingUsefulOne()
    }


    private fun somethingUsefulTwoAsync() = async { // Returns Deferred<Int>
        doSomethingUsefulTwo()
    }


    @Test
    fun testEmptyRunBlockingFunction() = runBlocking {
        // Used as template function
        // Copy and paste to skip typing
    }
}
