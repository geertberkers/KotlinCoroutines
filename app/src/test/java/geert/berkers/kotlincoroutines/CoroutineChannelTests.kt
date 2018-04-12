@file:Suppress("RemoveExplicitTypeArguments")

package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.cancelChildren
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Test
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineChannelTests{

    // Channels are similar to BlockingQueue

    @Test
    fun testChannel() = runBlocking {
        val channel = Channel<Int>()
        launch {
            // Some heavy CPU-consuming computation or async logic.
            // But we'll just send five squares
            for (x in 1..5) channel.send(x * x)
        }

        // Print received result as integers:
        repeat(5) {
            println(channel.receive())
        }

        println("Done!")

    }


    // Close to indicate there are no more new items!
    @Test
    fun testClosingIteration() = runBlocking {
        val channel = Channel<Int>()
        launch {
            for (x in 1..5) {
                channel.send(x * x)
            }

            // Done sending. Close it
            channel.close()
        }

        // Print result till closed
        for (y in channel) {
            println(y)
        }

        println("Done!")
    }


    // NOTE: Helper function for producer consumer pattern

    private fun produceSquares() = produce<Int> {
        for (x in 1..5) send(x * x)
    }


    @Test
    fun testChannelProducer() = runBlocking {
        val squares = produceSquares()
        squares.consumeEach { println(it) }
        println("Done!")
    }


    // NOTE: Helper methods for Pipeline

    private fun produceNumbers() = produce<Int> {
        var x = 1
        while (true) send(x++) // infinite stream of integers starting from 1
    }


    private fun square(numbers: ReceiveChannel<Int>) = produce<Int> {
        for (x in numbers) send(x * x)
    }


    @Test
    fun testPipeLine() = runBlocking<Unit> {
        val numbers = produceNumbers()
        val squares = square(numbers)
        for (i in 1..5) println(squares.receive()) // print first five

        println("Done!")
        squares.cancel() // need to cancel these coroutines in a larger app
        numbers.cancel()
    }


    // NOTE: Helper methods Prime Numbers PipeLine

    private fun numbersFrom(context: CoroutineContext, start: Int) = produce<Int>(context) {
        var x = start
        while (true) send(x++) // infinite stream of integers from start
    }


    private fun filter(context: CoroutineContext, numbers: ReceiveChannel<Int>, prime: Int) = produce<Int>(context) {
        for (x in numbers) if (x % prime != 0) send(x)
    }


    @Test
    fun testPrimePipeline() = runBlocking {
        var cur = numbersFrom(coroutineContext, 2)

        for (i in 1..10) {
            val prime = cur.receive()
            println(prime)
            cur = filter(coroutineContext, cur, prime)
        }

        coroutineContext.cancelChildren() // cancel all children to let main finish
    }


    // NOTE: Helper methods for Fan-out

    private fun produceNumbers2() = produce<Int> {
        var x = 1 // start from 1
        while (true) {
            send(x++) // produce next
            delay(100) // wait 0.1s
        }
    }


    private fun launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
        channel.consumeEach {
            println("Processor #$id received $it")
        }
    }


    @Test
    fun testFanOut() = runBlocking<Unit> {
        val producer = produceNumbers2()
        repeat(5) { launchProcessor(it, producer) }

        delay(950)
        producer.cancel() // Cancel Producer -> Kill all
    }


    // NOTE: Helper methods for Fan-in

    private suspend fun sendString(channel: SendChannel<String>, s: String, time: Long) {
        while (true) {
            delay(time)
            channel.send(s)
        }
    }

    @Test
    fun testFanIn() = runBlocking<Unit> {
        val channel = Channel<String>()

        launch(coroutineContext) { sendString(channel, "foo", 200L) }
        launch(coroutineContext) { sendString(channel, "BAR!", 500L) }

        repeat(6) { // receive first six
            println(channel.receive())
        }

        coroutineContext.cancelChildren() // Cancel children. Let finish main
    }


    @Test
    fun testBufferedChannel() = runBlocking<Unit> {
        val channel = Channel<Int>(4)   // Create Buffered Channel with size 4

        val sender = launch(coroutineContext) {
            repeat(10) {
                println("Sending $it")  // Print before sending
                channel.send(it)        // Suspend when buffer filled
            }
        }

        // don't receive anything... just wait....
        delay(1000)
        sender.cancel() // cancel sender coroutine
    }

    // Helper method fair channel
    data class Ball(var hits: Int)

    private suspend fun player(name: String, table: Channel<Ball>) {
        for (ball in table) {   // Receive
            ball.hits++
            println("$name $ball")

            delay(300)          // Wait
            table.send(ball)    // Send ball back
        }
    }

    // First In - First Out
    @Test
    fun testFailChannels() = runBlocking<Unit> {
        val table = Channel<Ball>() // This is a shared table

        launch(coroutineContext) { player("ping", table) }
        launch(coroutineContext) { player("pong", table) }

        table.send(Ball(0)) // Serve the ball
        delay(1000)         // Game ended

        coroutineContext.cancelChildren() // Game Over -> cancel
    }

}