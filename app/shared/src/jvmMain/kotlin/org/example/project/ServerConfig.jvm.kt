package org.example.project

private fun detectServerPort(): Int? {
	// Check environment and system property first
	val envPort = System.getenv("SERVER_PORT")?.toIntOrNull()
	if (envPort != null) return envPort
	val propPort = System.getProperty("server.port")?.toIntOrNull()
	if (propPort != null) return propPort

	// Try several likely relative paths for the server_port.txt file
	val candidates = listOf(
		"server/build/server_port.txt",
		"./server/build/server_port.txt",
		"../server/build/server_port.txt"
	)

	for (path in candidates) {
		try {
			val file = java.io.File(path)
			if (file.exists()) return file.readText().trim().toIntOrNull()
		} catch (_: Exception) {
		}
	}

	return null
}

actual val serverBaseUrl: String = run {
	val port = detectServerPort()
	if (port != null) "http://localhost:$port" else "https://server-impostor.wdona.dev"
}
