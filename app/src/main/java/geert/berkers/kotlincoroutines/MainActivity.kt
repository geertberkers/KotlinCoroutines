package geert.berkers.kotlincoroutines

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


    @Suppress("UNUSED_ANONYMOUS_PARAMETER")
    private fun setup(hello: TextView, fab: FloatingActionButton) {
        val helloText =             "Welcome by Coroutine Demo"
        val beforeLaunch =          " - Before launch"
        val insideCoroutine =       "  - Inside coroutine"
        val afterDelay =            "  - After delay"
        val afterLaunch =           " - After launch"

        hello.text = helloText

        fab.setOnClickListener { view ->
            hello.text = beforeLaunch
            println(beforeLaunch)

            // NOTE: Gets launched!
            launch(UI) {
                hello.text = insideCoroutine
                println(insideCoroutine)
                delay(1000)
                println(afterDelay)
                hello.text = afterDelay
            }

//            //  NOTE: when there is no Android UI imported
//            val beforeCrash =           "  - Before Crash"
//            val whileCrashHandling =    "  - Handling error with context!"
//            val finalising =            "   - Finally done!"
//
//            // No Android UI -> Crashes :
//            launch {
//                hello.text = insideCoroutine
//                println(insideCoroutine)
//                delay(1000)
//                println(beforeCrash)
//                // NOTE: App crashes!
//                // Only the original thread that created a view hierarchy can touch its views.
//                // So to change this we need uiContext!
//                try {
//                    hello.text = beforeCrash
//                } catch (ex: Exception){
//                    withContext(context = view.contextJob){
//                        println(whileCrashHandling)
//                        // NOTE: Printing result.
//                        // No updates on TextView.
//                        hello.text = whileCrashHandling
//                    }
//                } finally {
//                    println(finalising)
//                    // NOTE: Printing result.
//                    // No updates on TextView.
//                    hello.text = finalising
//                }
//            }

            // NOTE: Gets called before inside! Launch not launched yet!
            hello.text = afterLaunch
            println(afterLaunch)
        }
    }
}