@file:Suppress("RemoveExplicitTypeArguments")

package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.system.measureTimeMillis

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineSharedMutableConcurrency {

    // The problem
    @Volatile // in Kotlin `volatile` is an annotation
    private var counter = 0

    private suspend fun massiveRun(context: CoroutineContext, action: suspend () -> Unit) {
        val n = 1000 // Coroutines to launch
        val k = 1000 // Repeat times each coroutine

        val time = measureTimeMillis {
            val jobs = List(n) {
                launch(context) {
                    repeat(k) { action() }
                }
            }

            jobs.forEach { it.join() }
        }

        println("Completed ${n * k} actions in $time ms")
    }


    @Test
    fun testProblem() = runBlocking<Unit> {
        massiveRun(CommonPool) {
            counter++
        }
        println("Counter = $counter")
    }

    private val mtContext = newFixedThreadPoolContext(2, "mtPool") // explicitly define context with two threads

    @Test
    fun testWithContext() = runBlocking<Unit> {
        massiveRun(mtContext) { // use it instead of CommonPool in this sample and below
            counter++
        }
        println("Counter = $counter")
    }


    // NOTE: ThreadSafe Data structures

    var tsCounter = AtomicInteger()

    // Generic solution: Use ThreadSafety (aka synchronized, linearizable or atomic) data structures

    @Test
    fun testThreadSafe() = runBlocking<Unit> {
        massiveRun(CommonPool) {
            tsCounter.incrementAndGet()
        }

        println("Counter = ${tsCounter.get()}")
    }

    // NOTE: Thread safety confinement fine-grained

    private val counterContext = newSingleThreadContext("CounterContext")
    private var fgCounter = 0

    @Test
    fun testFineGrainedConfinement() = runBlocking<Unit> {
        massiveRun(CommonPool) { // Run using CommonPool
            withContext(counterContext) { // Confine in SingleThread Context
                fgCounter++
            }
        }
        println("Counter = $fgCounter")
    }


    // NOTE: Thread safety confinement coarse-grained

    private val cgCounterContext = newSingleThreadContext("CounterContext")
    private var cgCounter = 0

    @Test
    fun testCoarseGrained() = runBlocking<Unit> {
        massiveRun(cgCounterContext) { // run each coroutine in the single-threaded context
            cgCounter++
        }
        println("Counter = $cgCounter")
    }


    // NOTE: Mutual exclusion

    private val mutex = Mutex()
    private var mCounter = 0

    @Test
    fun testMutualExclusion() = runBlocking<Unit> {
        massiveRun(CommonPool) {
            mutex.withLock { // mutex.lock(); try { ... } finally { mutex.unlock() }
                mCounter++
            }
        }
        println("Counter = $mCounter")
    }

    // NOTE: Actors (code from githug fails)

//    sealed class CounterMsg {
//
//        object IncCounter : CounterMsg()
//
//    }
//
//    // Something wrong with CounterMsg. Cannot create because is is private
//
////    object IncCounter : CounterMsg() // one-way message to increment counter
//
////    class GetCounter(val response: CompletableDeferred<Int>) = CounterMsg.IncCounter // a request with reply
//
//    // This function launches a new counter actor
//    private fun counterActor() = actor<CounterMsg> {
//        var counter = 0 // actor state
//        for (msg in channel) { // iterate over incoming messages
//            when (msg) {
//                is CounterMsg.IncCounter -> counter++
//                is GetCounter -> msg.response.complete(counter)
//            }
//        }
//    }
//
//    @Test
//    fun testActors() = runBlocking<Unit> {
//        val counter = counterActor() // create the actor
//
//        massiveRun(CommonPool) {
//            counter.send(CounterMsg.IncCounter)
//        }
//
//        // send a message to get a counter value from an actor
//        val response = CompletableDeferred<Int>()
//        counter.send(GetCounter(response))
//
//        println("Counter = ${response.await()}")
//        counter.close() // shutdown the actor
//    }
}