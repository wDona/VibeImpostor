package org.example.project.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun initDatabase() {
    val dbUrl = System.getenv("DB_URL") ?: "jdbc:h2:./data/impostor;DB_CLOSE_DELAY=-1"

    val hikariConfig = HikariConfig().apply {
        jdbcUrl = dbUrl
        driverClassName = when {
            dbUrl.startsWith("jdbc:h2:") -> "org.h2.Driver"
            dbUrl.startsWith("jdbc:postgresql:") -> "org.postgresql.Driver"
            else -> "org.h2.Driver"
        }
        maximumPoolSize = 10
    }

    val dataSource = HikariDataSource(hikariConfig)
    Database.connect(dataSource)

    transaction {
        SchemaUtils.create(Users, Sessions, WordPacks, Categories, Words)
        SchemaUtils.createMissingTablesAndColumns(Words)
    }
}
