package com

import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.days


@Serializable
data class UserBox(val name: String, val age: Int)

class BoxService(database: Database) {
    object Box : Table() {
        val id = integer("id").autoIncrement()
        val name = varchar("name", length = 50)
        val age = integer("age")
        val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
        //TODO: expiry date remove nullable, default to 30 days from createdAt
        val expiry = datetime("expiry").nullable()

        override val primaryKey = PrimaryKey(id)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Box)
        }
    }

    suspend fun create(user: UserBox): Int = dbQuery {
        Box.insert {
            it[name] = user.name
            it[age] = user.age
        }[Box.id]
    }

    suspend fun read(id: Int): UserBox? {
        return dbQuery {
            Box.selectAll()
                .where { Box.id eq id }
                .map { UserBox(it[Box.name], it[Box.age]) }
                .singleOrNull()
        }
    }

    suspend fun update(id: Int, user: UserBox) {
        dbQuery {
            Box.update({ Box.id eq id }) {
                it[name] = user.name
                it[age] = user.age
            }
        }
    }

    suspend fun delete(id: Int) {
        dbQuery {
            Box.deleteWhere { Box.id.eq(id) }
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}

