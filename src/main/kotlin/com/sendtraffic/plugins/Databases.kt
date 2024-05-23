package com.sendtraffic.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.network.sockets.connect
import java.sql.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*

fun connectToDatabase(): Database {
    // val url = environment.config.property("postgres.url").getString()
    // val database = Database.connect(
    //     url = "jdbc:postgresql://host:port/database",
    //     user = environment.config.property("postgres.user").getString(),
    //     driver = "org.postgresql.Driver",
    //     password = environment.config.property("postgres.password").getString()
    // )
    return Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
}

fun Application.configureDatabases() {
    val database = connectToDatabase()
    val loadService = LoadService(database)
    routing {
        // Create job
        post("/load") {
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

            call.respond(HttpStatusCode.Created, mapOf(
                "id" to id
            ))
        }
        
        // Read job
        get("/load/{id}") {
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
        put("/load/{id}") {
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
        delete("/load/{id}") {
            val id = try {
                call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid ID"))
                return@delete
            }

            try {
                loadService.delete(id)
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "Failed to delete job"))
            }
        }
    }
}
