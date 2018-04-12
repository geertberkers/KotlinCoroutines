package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.system.measureTimeMillis

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineComposingSuspendinglTests{

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
}