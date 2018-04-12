package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.*
import org.junit.Test

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineContextDispatcherTests{

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
}