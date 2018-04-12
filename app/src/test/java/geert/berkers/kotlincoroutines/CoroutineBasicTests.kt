package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.*
import org.junit.Test

/**
 * Local Unit Tests for learning the Kotlin Coroutines Library.
 *
 * See [Kotlin Coroutines GitHub page](https://github.com/Kotlin/kotlinx.coroutines/blob/master/coroutines-guide.md#coroutine-basics).
 */
class CoroutineBasicTests {

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
      launch {
            repeat(1000) { i ->
                println("I'm sleeping $i ...")
                delay(500L)
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")

    }







    @Test
    fun testEmptyRunBlockingFunction() = runBlocking {
        // Used as template function
        // Copy and paste to skip typing
    }
}
