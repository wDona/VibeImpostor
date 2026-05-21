plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.shadow)
}

group = "org.example.project"
version = "1.0.0"

application {
    mainClass.set("org.example.project.ApplicationKt")
}

// Configuración de ShadowJar para crear el Fat JAR
tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveFileName.set("server-all.jar")
    mergeServiceFiles() // Necesario para Ktor/Logback
}

dependencies {
    api(projects.core)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverWebsockets)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverAuth)
    implementation(libs.ktor.serverStatusPages)
    implementation(libs.ktor.serverCors)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.javaTime)
    implementation(libs.h2)
    implementation(libs.postgresql)
    implementation(libs.hikari)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}