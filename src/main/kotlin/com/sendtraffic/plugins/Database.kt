package com.sendtraffic.plugins

import java.sql.*
import org.jetbrains.exposed.sql.*

fun connectToDatabase(): Database {
    return Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = ""
    )
}