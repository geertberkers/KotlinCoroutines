package geert.berkers.kotlincoroutines

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import org.w3c.dom.Node

interface JobHolder {
    val job: Job
}

class MainActivity : AppCompatActivity(), JobHolder {

    // MainActivity Job Instance
    override val job: Job = Job()


    @Suppress("unused") // Used in commented code for testing!
    private val View.contextJob: Job
        get() = (context as? JobHolder)?.job ?: NonCancellable


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setup(hello, fab)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_settings -> return true
        }

        return super.onOptionsItemSelected(item)
    }


    override fun onDestroy() {
        super.onDestroy()
        println("onDestroy() - MainActivity - Kill Jobs.")
        job.cancel()
    }


    // GitHub implementation - Launch coroutine
//    @SuppressLint("SetTextI18n")
//    private fun setup(hello: TextView, fab: FloatingActionButton) {
//        val job = launch(UI) { // launch coroutine in UI context
//            for (i in 10 downTo 1) { // countdown from 10 to 1
//                hello.text = "Countdown $i ..." // update text
//                delay(500) // wait half a second
//            }
//            hello.text = "Done!"
//        }
//
//        fab.setOnClickListener { job.cancel() }  // cancel coroutine on click
//    }


    // NOTE: Extension for coroutines!

//    fun View.onClick(action: suspend () -> Unit) {
//        setOnClickListener {
//            launch(UI) {
//                action()
//            }
//        }
//    }

    // NOTE: At most 1 concurrent Job!

    fun View.onClick(action: suspend (View) -> Unit) {
        // Launch one actor
        val eventActor = actor<View>(contextJob, capacity = Channel.CONFLATED) {
            for (event in channel) action(event)
        }

        // Install a listener to activate this actor
        setOnClickListener {
            eventActor.offer(it)
        }
    }
//
//    @SuppressLint("SetTextI18n")
//    private fun setup(hello: TextView, fab: FloatingActionButton) {
//        fab.onClick { // start coroutine when the circle is clicked
//            for (i in 10 downTo 1) { // countdown from 10 to 1
//                hello.text = "Countdown $i ..." // update text
//                delay(500) // wait half a second
//            }
//
//            hello.text = "Done!"
//        }
//    }

    // NOTE: Blocking operations

    private suspend fun fib(x: Int): Int = withContext(CommonPool) {
        fibBlocking(x)
    }


    private fun fibBlocking(x: Int): Int =
            if (x <= 1) x else fibBlocking(x - 1) + fibBlocking(x - 2)

//
//    @SuppressLint("SetTextI18n")
//    private fun setup(hello: TextView, fab: FloatingActionButton) {
//        var result = "none" // the last result
//        // counting animation
//        launch(UI) {
//            var counter = 0
//            while (true) {
//                hello.text = "${++counter}: $result"
//                delay(100) // update the text every 100ms
//            }
//        }
//        // compute the next fibonacci number of each click
//        var x = 1
//        fab.onClick {
//            result = "fib($x) = ${fib(x)}"
//            x++
//        }
//    }

    //  NOTE: Tested some code myself before implementing GitHub Guide
//    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
//    private fun setup(hello: TextView, fab: FloatingActionButton) {
//        val helloText =             "Welcome by Coroutine Demo"
//        val beforeLaunch =          " - Before launch"
//        val insideCoroutine =       "  - Inside coroutine"
//        val afterDelay =            "  - After delay"
//        val afterLaunch =           " - After launch"
//
//        hello.text = helloText
//
//        fab.setOnClickListener { view ->
//            hello.text = beforeLaunch
//            println(beforeLaunch)
//
//            // NOTE: Gets launched!
//            launch(UI) {
//                hello.text = insideCoroutine
//                println(insideCoroutine)
//                delay(1000)
//                println(afterDelay)
//                hello.text = afterDelay
//            }
//
////            //  NOTE: when there is no Android UI imported
////            val beforeCrash =           "  - Before Crash"
////            val whileCrashHandling =    "  - Handling error with context!"
////            val finalising =            "   - Finally done!"
////
////            // No Android UI -> Crashes :
////            launch {
////                hello.text = insideCoroutine
////                println(insideCoroutine)
////                delay(1000)
////                println(beforeCrash)
////                // NOTE: App crashes!
////                // Only the original thread that created a view hierarchy can touch its views.
////                // So to change this we need uiContext!
////                try {
////                    hello.text = beforeCrash
////                } catch (ex: Exception){
////                    withContext(context = view.contextJob){
////                        println(whileCrashHandling)
////                        // NOTE: Printing result.
////                        // No updates on TextView.
////                        hello.text = whileCrashHandling
////                    }
////                } finally {
////                    println(finalising)
////                    // NOTE: Printing result.
////                    // No updates on TextView.
////                    hello.text = finalising
////                }
////            }
//
//            // NOTE: Gets called before inside! Launch not launched yet!
//            hello.text = afterLaunch
//            println(afterLaunch)
//        }
//    }

    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun setup(hello: TextView, fab: FloatingActionButton) {
        fab.onClick {
            println("Before launch")
            launch(UI, CoroutineStart.UNDISPATCHED) { // UNDISPATCHED -> Execute till 1st suspension
                println("Inside coroutine")
                delay(100)                            // Done after suspension
                println("After delay")
            }
            println("After launch")                   // Done while suspension start
        }
    }
}