package com.sendtraffic

import com.sendtraffic.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.*

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            configureRoutes()
        }
        client.get("/up").apply {
            assertEquals(HttpStatusCode.OK, status)
            assertEquals("Up", bodyAsText())
        }
    }
}
