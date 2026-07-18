package org.example.project

import io.ktor.http.ContentType
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.serialization.kotlinx.json.*
import org.example.project.db.initDatabase
import org.example.project.db.seedBaseWordsIfEmpty
import org.example.project.game.gameServer
import org.example.project.protocol.ProtocolJson

fun main() {
    // Determine port from environment or system property, default to 8080
    val envPort = System.getenv("SERVER_PORT")
    val propPort = System.getProperty("server.port")
    val portToUse = listOfNotNull(envPort, propPort).firstOrNull()?.toIntOrNull() ?: 8080

    println("Starting server on port $portToUse (requested)")
    try {
        val server = embeddedServer(Netty, port = portToUse, host = "0.0.0.0", module = Application::module)
        // Persist chosen port so local clients can discover it during development
        try {
            val buildDir = java.io.File("build")
            buildDir.mkdirs()
            java.io.File(buildDir, "server_port.txt").writeText(portToUse.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        server.start(wait = true)
    } catch (e: Throwable) {
        System.err.println("Failed to start server on port $portToUse: ${e.message}")
        System.err.println("Please stop any process using that port or set SERVER_PORT to a different port.")
        kotlin.system.exitProcess(1)
    }
}

fun Application.module() {
    initDatabase()
    seedBaseWordsIfEmpty()

    install(WebSockets)

    install(ContentNegotiation) {
        json(ProtocolJson.json, contentType = ContentType.Application.Json)
    }

    install(StatusPages) {
        exception<Exception> { call, exception ->
            call.respond(
                io.ktor.http.HttpStatusCode.InternalServerError,
                mapOf("error" to (exception.message ?: "Internal server error"))
            )
        }
    }

    install(CORS) {
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowHeader(io.ktor.http.HttpHeaders.Authorization)
        allowHeader(io.ktor.http.HttpHeaders.Accept)
        allowHeadersPrefixed("x-")
        allowCredentials = true

        allowHost("localhost:8080")
        allowHost("127.0.0.1:8080")
        allowHost("impostor.wdona.dev", schemes = listOf("http", "https"))
    }

    routing {
        get("/health") {
            call.respondText("OK")
        }

        // Los deadlines de turno y votación viajan como epoch absoluto de este
        // reloj. Un cliente con la hora desajustada (móvil, portátil suspendido,
        // NTP apagado) calcularía mal el tiempo restante, así que puede pedir
        // nuestra hora y corregir el desfase.
        get("/time") {
            call.respondText(System.currentTimeMillis().toString())
        }

        authRoutes()
        packRoutes()
        gameServer()
    }
}