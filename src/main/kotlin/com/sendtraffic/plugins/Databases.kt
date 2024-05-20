package com.sendtraffic.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.sql.*
import kotlinx.coroutines.*
import org.jetbrains.exposed.sql.*

fun Application.configureDatabases() {
    val database = Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = ""
        )

    // val url = environment.config.property("postgres.url").getString()
    // val database = Database.connect(
    //         url = "jdbc:postgresql://host:port/database",
    //         user = environment.config.property("postgres.user").getString(),
    //         driver = "org.postgresql.Driver",
    //         password = environment.config.property("postgres.password").getString()
    //     )
    val loadService = LoadService(database)
    routing {
        // Create job
        post("/load") {
            val loadJob = call.receive<ExposedLoadJob>()
            val id = loadService.create(loadJob)
            call.respond(HttpStatusCode.Created, mapOf(
                "id" to id
            ))
        }
        
        // Read job
        get("/load/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val loadJob = loadService.read(id)
            if (loadJob != null) {
                call.respond(HttpStatusCode.OK, loadJob)
            } else {
                call.respond(HttpStatusCode.NotFound)
            }
        }
        
        // Update job
        put("/load/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            val loadJob = call.receive<ExposedLoadJob>()
            loadService.update(id, loadJob)
            call.respond(HttpStatusCode.OK, loadJob)
        }
        
        // Delete job
        delete("/load/{id}") {
            val id = call.parameters["id"]?.toInt() ?: throw IllegalArgumentException("Invalid ID")
            loadService.delete(id)
            call.respond(HttpStatusCode.OK, mapOf("id" to id))
        }
    }
}
