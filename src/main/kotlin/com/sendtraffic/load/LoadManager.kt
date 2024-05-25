package com.sendtraffic.load

import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*

object loadTestManager {
    private val client = OkHttpClient()
    val activeLoadTests = ConcurrentHashMap<Int, ExposedLoadJob>()
    val activeLoadScopes = ConcurrentHashMap<Int, CoroutineScope>()

    // Start the load test
    fun startLoadTest(loadJob: ExposedLoadJob, id: Int) {
        println("Starting load test $id, url is ${loadJob.url}")
        
        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch {
            val startTime = System.currentTimeMillis()
            val endTime = startTime + loadJob.duration * 1000

            try {
                while (System.currentTimeMillis() < endTime) {
                    println("Building request to ${loadJob.url}")
                    val request = Request.Builder()
                        .url(loadJob.url)
                        .build()
                    try {
                        println("Sending request to ${loadJob.url}")
                        client.newCall(request).execute().use { response ->
                            if (!response.isSuccessful) {
                                println("Unexpected code $response")
                            } else {
                                println("Received response from ${loadJob.url}")
                            }
                        }
                    } catch (e: Exception) {
                        println("Failed to send request to ${loadJob.url}: ${e.message}")
                    }

                    delay(1000 / loadJob.rate.toLong())
                }
            } catch (e: CancellationException) {
                println("Load test $id stopped")
                activeLoadTests.remove(id)
            } finally {
                println("Load test $id completed")
                activeLoadTests.remove(id)
            }
        }

        activeLoadTests[id] = loadJob
        activeLoadScopes[id] = scope
    }

    // Cancel the load test
    fun stopLoadTest(id: Int) {
        val scope = activeLoadScopes[id]
        if (scope != null) {
            activeLoadTests.remove(id)
            scope.cancel()
            println("Load test $id canceled")
        }
    }
}
