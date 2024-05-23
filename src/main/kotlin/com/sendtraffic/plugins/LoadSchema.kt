package com.sendtraffic.plugins

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlinx.serialization.Serializable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*

@Serializable
data class ExposedLoadJob(val description: String, val url: String)

class LoadService(private val database: Database) {
    object LoadTests : Table() {
        val id = integer("id").autoIncrement()
        val description = varchar("description", length = 50)
        val url = varchar("url", length = 250)

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(LoadTests)
        }
    }

    suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }

    suspend fun create(loadJob: ExposedLoadJob): Int = dbQuery {
        LoadTests.insert {
            it[description] = loadJob.description
            it[url] = loadJob.url
        }[LoadTests.id]
    }

    suspend fun read(id: Int): ExposedLoadJob? {
        return dbQuery {
            LoadTests.select { LoadTests.id eq id }
                .map { ExposedLoadJob(it[LoadTests.description], it[LoadTests.url]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, loadJob: ExposedLoadJob) {
        dbQuery {
            LoadTests.update({ LoadTests.id eq id }) {
                it[description] = loadJob.description
                it[url] = loadJob.url
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            LoadTests.deleteWhere { LoadTests.id.eq(id) }
        }
    }
}

