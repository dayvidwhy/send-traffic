package com.sendtraffic.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.network.sockets.connect
import java.sql.*
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*
import java.util.concurrent.ConcurrentHashMap

fun connectToDatabase(): Database {
    return Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
}

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

fun Application.configureRoutes() {
    val database = connectToDatabase()
    val loadService = LoadService(database)
    routing {
        get("/up") {
            println("Up")
            call.respondText("Up", ContentType.Text.Plain)
        }
        route("/loadtest") {
            // Create job
            post {
                val loadJob = try {
                    call.receive<ExposedLoadJob>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid job"))
                    return@post
                }
        
                val id = try {
                    loadService.create(loadJob)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to create job"))
                    return@post
                }

                loadTestManager.startLoadTest(loadJob, id)
        
                call.respond(HttpStatusCode.Created, mapOf(
                    "id" to id
                ))
            }
            
            // Read job
            get("/{id}") {
                val id = try {
                    call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                    return@get
                }
        
                val loadJob = try {
                    loadService.read(id) ?: throw IllegalArgumentException("Failed to read job")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to read job"))
                    return@get
                }
                
                call.respond(HttpStatusCode.OK, loadJob)
            }
            
            // Update job
            put("/{id}") {
                val id = try {
                    call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                    return@put
                }
        
                val loadJob = try {
                    call.receive<ExposedLoadJob>()
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid job"))
                    return@put
                }
                
                try {
                    loadService.update(id, loadJob)
                    call.respond(HttpStatusCode.OK, loadJob)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to update job"))
                }
            }
            
            // Delete job
            delete("/{id}") {
                val id = try {
                    call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                    return@delete
                }
        
                try {
                    loadService.delete(id)
                    loadTestManager.stopLoadTest(id)
                    call.respond(HttpStatusCode.OK, mapOf("id" to id))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to delete job"))
                }
            }
        }
    }
}
