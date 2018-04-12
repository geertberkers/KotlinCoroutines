package geert.berkers.kotlincoroutinesunittests

import kotlinx.coroutines.experimental.*
import org.junit.Test
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


    // NOTE:    These xxxAsync functions are not suspending and can be called from anywhere
    //          However, their use always implies asynchronous (here concurrent) execution!
    private fun somethingUsefulOneAsync() = async { // Returns Deferred<Int>
        doSomethingUsefulOne()
    }


    private fun somethingUsefulTwoAsync() = async { // Returns Deferred<Int>
        doSomethingUsefulTwo()
    }

    @Test
    fun testAsyncStyleFunctions() {
        // Example showing their use outside of a coroutine!

        val time = measureTimeMillis {

            // Initiate async outside of a coroutine
            val one = somethingUsefulOneAsync() // Return Deferred<Int>
            val two = somethingUsefulTwoAsync() // Return Deferred<Int>

            // Waiting for result must involve suspend or blocking!
            runBlocking {
                val resultOne = one.await() // Gets Deferred Value
                val resultTwo = two.await() // Gets Deferred Value

                println("The answer is ${resultOne + resultTwo}")
            }
        }

        println("Completed in $time ms")
    }

    @Test
    fun testCoroutineContextDispatchers() = runBlocking {
        val jobs = arrayListOf<Job>()

        // NOTE:    Default Dispatcher is the same as CommonPool.
        //          launch{ ... }  equals to :
        //          launch(DefaultDispatcher){ ... } and:
        //          launch(CommonPool){ ... }

        jobs += launch(Unconfined) { // Not confined -- Works on MainThread
            println("      'Unconfined': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(coroutineContext) { // Parent context, runBlocking coroutine
            println("'coroutineContext': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(CommonPool) { // Dispatched to ForkJoinPool.commonPool (or equivalent)
            println("      'CommonPool': I'm working in thread ${Thread.currentThread().name}")
        }
        jobs += launch(newSingleThreadContext("MyOwnThread")) { // Own New Thread
            println("          'newSTC': I'm working in thread ${Thread.currentThread().name}")
        }

        jobs.forEach { it.join() }
    }


    @Test
    fun testUnconfinedAndConfined() = runBlocking {
        val jobs = arrayListOf<Job>()

        jobs += launch(Unconfined) {
            println("      'Unconfined': I'm working in thread ${Thread.currentThread().name}")
            delay(500)
            println("      'Unconfined': After delay in thread ${Thread.currentThread().name}")
        }

        jobs += launch(coroutineContext) {
            println("'coroutineContext': I'm working in thread ${Thread.currentThread().name}")
            delay(1000)
            println("'coroutineContext': After delay in thread ${Thread.currentThread().name}")
        }

        jobs.forEach { it.join() }
    }


    @Test
    fun testDebugging() = runBlocking {
        val a = async(coroutineContext) {
            log("I'm computing a piece of the answer")       // Deferred
            6
        }

        val b = async(coroutineContext) {
            log("I'm computing another piece of the answer") // Deferred
            7
        }

        log("The answer is ${a.await() * b.await()}")        // runBlocking
    }


    private fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")


    @Test
    fun testJumpingThreads() = runBlocking {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")
                    withContext(ctx2) {
                        log("Working in ctx2")
                    }
                    log("Back to ctx1")
                }
            }
        }
    }


    @Test
    fun testJobInContext() = runBlocking {
        println("My job is ${coroutineContext[Job]}")

    }


    @Test
    fun testCoroutineChildren() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {

            // it spawns two other jobs, one with its separate context
            val job1 = launch {
                println("job1: I have my own context and execute independently!")
                delay(1000)
                println("job1: I am not affected by cancellation of the request")
            }

            // and the other inherits the parent context
            val job2 = launch(coroutineContext) {
                delay(100)
                println("job2: I am a child of the request coroutine")
                delay(1000)
                println("job2: I will not execute this line if my parent request is cancelled")
            }
            // request completes when both its sub-jobs complete:
            job1.join()
            job2.join()
        }
        delay(500)
        request.cancel()

        delay(1000) // Delay to see what happens
        println("main: Who has survived request cancellation?")
    }

    @Test
    fun testCombiningContexts() = runBlocking {
        val request = launch(coroutineContext) { // use the context of `runBlocking`
            // spawns CPU-intensive child job in CommonPool !!!
            val job = launch(coroutineContext + CommonPool) {
                println("job: I am a child of the request coroutine, but with a different dispatcher")
                delay(1000)
                println("job: I will not execute this line if my parent request is cancelled")
            }
            job.join() // request completes when its sub-job completes
        }
        delay(500)
        request.cancel()

        delay(1000) // Delay to see what happens
        println("main: Who has survived request cancellation?")
    }

    @Test
    fun testParentalResponsibility() = runBlocking {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            repeat(3) { i -> // launch a few children jobs
                launch(coroutineContext)  {
                    delay((i + 1) * 200L) // Variable delay 200ms, 400ms, 600ms
                    println("Coroutine $i is done")
                }
            }
            println("request: I'm done and I don't explicitly join my children that are still active")
        }

        request.join() // Wait until completion, including all its children
        println("Now processing of the request is complete")
    }


    @Test
    fun testNamedCoroutinesForDebugging() = runBlocking {
        log("Started main coroutine")
        // run two background value computations
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log("Computing v1")
            252
        }
        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log("Computing v2")
            6
        }
        log("The answer for v1 / v2 = ${v1.await() / v2.await()}")
    }


    @Test
    fun testCancellationExplicitJob() = runBlocking {
        val job = Job() // Job to manage lifecycle

        // now launch ten coroutines for a demo, each working for a different time
        val coroutines = List(10) { i ->
            
//          launch(coroutineContext) { // No Parent Job. So they are not cancelled

            // All children of our job object
            launch(coroutineContext, parent = job) { // Context of main runBlocking thread, but with parent job
                delay((i + 1) * 200L) // Variable delay 200ms, 400ms, ... etc
                println("Coroutine $i is done")
            }
        }
        println("Launched ${coroutines.size} coroutines")
        delay(500L)

        println("Cancelling the job!")
        job.cancelAndJoin() // Cancel all our coroutines and wait for all of them to complete
    }

    @Test
    fun testEmptyRunBlockingFunction() = runBlocking {
        // Used as template function
        // Copy and paste to skip typing
    }
}
