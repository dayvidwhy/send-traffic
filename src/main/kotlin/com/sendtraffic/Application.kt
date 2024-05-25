package com.sendtraffic

import com.sendtraffic.plugins.*
import com.sendtraffic.load.*
import io.ktor.server.application.*
import java.io.File

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    configureSerialization()
    configureRoutes()
    configureSecurity()
}
