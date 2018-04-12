@file:Suppress("RemoveExplicitTypeArguments")

package geert.berkers.kotlincoroutines

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.selects.select
import org.junit.Test
import java.util.*
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.coroutineContext

/**
 * Created by Zorgkluis (Geert).
 */
class CoroutineSelectExpression {

    // Select from Channels

    private fun fizz(context: CoroutineContext) = produce<String>(context) {
        while (true) { // sends "Fizz" every 300 ms
            delay(300)
            send("Fizz")
        }
    }


    private fun buzz(context: CoroutineContext) = produce<String>(context) {
        while (true) { // sends "Buzz!" every 500 ms
            delay(500)
            send("Buzz!")
        }
    }


    private suspend fun selectFizzBuzz(fizz: ReceiveChannel<String>, buzz: ReceiveChannel<String>) {
        select<Unit> { // <Unit> means that this select expression does not produce any result
            fizz.onReceive { value ->  // this is the first select clause
                println("fizz -> '$value'")
            }
            buzz.onReceive { value ->  // this is the second select clause
                println("buzz -> '$value'")
            }
        }
    }

    @Test
    fun testFizzBuzz() = runBlocking<Unit> {
        val fizz = fizz(coroutineContext)
        val buzz = buzz(coroutineContext)

        repeat(7) {
            selectFizzBuzz(fizz, buzz)
        }

        coroutineContext.cancelChildren() // cancel fizz & buzz coroutines
    }


    // Select on close
    private suspend fun selectAorB(a: ReceiveChannel<String>, b: ReceiveChannel<String>): String =
            select<String> {
                a.onReceiveOrNull { value ->
                    if (value == null)
                        "Channel 'a' is closed"
                    else
                        "a -> '$value'"
                }
                b.onReceiveOrNull { value ->
                    if (value == null)
                        "Channel 'b' is closed"
                    else
                        "b -> '$value'"
                }
            }


    @Test
    fun testClose() = runBlocking<Unit> {
        val a = produce<String>(coroutineContext) {
            repeat(4) { send("Hello $it") }
        }

        val b = produce<String>(coroutineContext) {
            repeat(4) { send("World $it") }
        }

        repeat(8) {
            // print first eight results
            println(selectAorB(a, b))
        }

        coroutineContext.cancelChildren()

    }

    // Selecting on send

    private fun produceNumbers(context: CoroutineContext, side: SendChannel<Int>) = produce<Int>(context) {
        for (num in 1..10) {        // produce 10 numbers from 1 to 10
            delay(100)              // every 100 ms
            select<Unit> {
                onSend(num) {}      // Send to the primary channel
                side.onSend(num) {} // or to the side channel
            }
        }
    }

    @Test
    fun testSelectingSend() = runBlocking<Unit> {
        val side = Channel<Int>() // allocate side channel]

        launch(coroutineContext) { // this is a very fast consumer for the side channel
            side.consumeEach { println("Side channel has $it") }
        }

        produceNumbers(coroutineContext, side).consumeEach {
            println("Consuming $it")
            delay(250) // Digest consumed number properly
        }

        println("Done consuming")
        coroutineContext.cancelChildren()
    }

    // NOTE: Selected deferred:
    private fun asyncString(time: Int) = async {
        delay(time.toLong())
        "Waited for $time ms"
    }

    private fun asyncStringsList(): List<Deferred<String>> {
        val random = Random(3)
        return List(12) { asyncString(random.nextInt(1000)) }
    }

    @Test
    fun testSelectingDeferred() = runBlocking<Unit> {
        val list = asyncStringsList()
        val result = select<String> {
            list.withIndex().forEach { (index, deferred) ->
                deferred.onAwait { answer ->
                    "Deferred $index produced answer '$answer'"
                }
            }
        }

        println(result)
        val countActive = list.count { it.isActive }
        println("$countActive coroutines are still active")
    }

    // NOTE: Swift over channel deferred alues

    private fun asyncString(str: String, time: Long) = async {
        delay(time)
        str
    }

    private fun switchMapDeferreds(input: ReceiveChannel<Deferred<String>>) = produce<String> {
        var current = input.receive() // Start  first received deferred value

        while (isActive) { // Loop while not cancelled/closed

            val next = select<Deferred<String>?> { // return next deferred value from this select or null

                input.onReceiveOrNull { update ->
                    update // replaces next value to wait
                }

                current.onAwait { value ->
                    send(value) // send value that current deferred has produced
                    input.receiveOrNull() // and use the next deferred from the input channel
                }
            }

            if (next == null) {
                println("Channel was closed")
                break
            } else {
                current = next
            }
        }
    }

    @Test
    fun testSwitchMapDeferreds() = runBlocking<Unit> {
        val chan = Channel<Deferred<String>>() // the channel for test

        launch(coroutineContext) { // launch printing coroutine
            for (s in switchMapDeferreds(chan))
                println(s) // print each received string
        }

        chan.send(asyncString("BEGIN", 100))
        delay(200) // enough time for "BEGIN" to be produced

        chan.send(asyncString("Slow", 500))
        delay(100) // not enough time to produce slow

        chan.send(asyncString("Replace", 100))
        delay(500) // give it time before the last one

        chan.send(asyncString("END", 500))
        delay(1000) // give it time to process

        chan.close() // close the channel ...
        delay(500) // and wait some time to let it finish
    }
}