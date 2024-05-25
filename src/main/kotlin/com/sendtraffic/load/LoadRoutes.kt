package com.sendtraffic.load

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.network.sockets.connect
import java.sql.*
import org.jetbrains.exposed.sql.*
import com.sendtraffic.plugins.connectToDatabase

fun Application.configureRoutes() {
    val database = connectToDatabase()
    val loadService = LoadService(database)
    routing {
        get("/health") { call.respondText("Up", ContentType.Text.Plain) }
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
