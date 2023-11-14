package com.threshold.mytoolbox

import org.junit.Test

import org.junit.Assert.*
import java.util.concurrent.Executors

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testExecutors() {
        val singleThreadExecutors = Executors.newSingleThreadExecutor()
        singleThreadExecutors.submit {
            var counter = 0L
            while (true) {
                System.out.print(" "+counter++)
                Thread.sleep(1)
            }
        }
        Thread.sleep(10)
        System.out.println("\nbefore shutdownNow->"+System.currentTimeMillis())
        singleThreadExecutors.shutdownNow()
        System.out.println("after shutdownNow->"+System.currentTimeMillis())
        Thread.sleep(1000)
    }
}
