# Fase 12 — Pulido y despliegue

Este plan está escrito para un modelo pequeño. Sigue los pasos EN ORDEN. Cada paso
dice exactamente qué archivo editar y qué código escribir. No inventes nada extra.

---

## Paso 1 — CRÍTICO: Arreglar GameClient (el juego no funciona sin esto)

**Problema:** `GameClient.send()` crea un Frame pero nunca lo envía (tiene `// TODO`).
La sesión WebSocket vive dentro del lambda de `connect()`, y `send()` no tiene acceso
a ella. Hay que refactorizar para usar un `Channel` interno.

**Archivo a reemplazar completamente:**
`app/shared/src/commonMain/kotlin/org/example/project/net/GameClient.kt`

**Nuevo contenido del archivo:**

```kotlin
package org.example.project.net

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.HttpMethod
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import org.example.project.protocol.ClientMessage
import org.example.project.protocol.ProtocolJson
import org.example.project.protocol.ServerMessage

class GameClient(private val baseUrl: String) {
    private val client = HttpClient { install(WebSockets) }
    private val outgoing = Channel<ClientMessage>(Channel.BUFFERED)

    suspend fun send(message: ClientMessage) {
        outgoing.send(message)
    }

    fun connect(firstMessage: ClientMessage): Flow<ServerMessage> = flow {
        outgoing.send(firstMessage)
        val host = baseUrl.removePrefix("http://").removePrefix("https://").substringBefore(":")
        val port = baseUrl.substringAfterLast(":").toIntOrNull() ?: 8080
        client.webSocket(method = HttpMethod.Get, host = host, port = port, path = "/ws/game") {
            launch {
                for (msg in outgoing) {
                    send(Frame.Text(ProtocolJson.json.encodeToString(msg)))
                }
            }
            for (frame in incoming) {
                if (frame !is Frame.Text) continue
                try {
                    emit(ProtocolJson.json.decodeFromString<ServerMessage>(frame.readText()))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
```

**Luego actualizar GameViewModel.kt** — los métodos `createRoom()` y `joinRoom()`
deben pasar el primer mensaje a `connect()` en vez de llamar `sendGameMessage` antes:

En `GameViewModel.kt`:

- Cambia `createRoom()`:
```kotlin
fun createRoom() {
    viewModelScope.launch {
        gameClient = GameClient(baseUrl)
        val firstMsg = ClientMessage.CreateRoom(playerName, apiClient?.authToken)
        gameClient!!.connect(firstMsg).collect { msg -> handleServerMessage(msg) }
    }
}
```

- Cambia `joinRoom(code: String)`:
```kotlin
fun joinRoom(code: String) {
    viewModelScope.launch {
        gameClient = GameClient(baseUrl)
        val firstMsg = ClientMessage.JoinRoom(code, playerName, apiClient?.authToken)
        gameClient!!.connect(firstMsg).collect { msg -> handleServerMessage(msg) }
    }
}
```

- Elimina la función privada `listenToServer()` — ya no se usa.
- Elimina la línea `private var apiClient: ApiClient? = null` (ya no se necesita)
  y quita las dos líneas `apiClient = ApiClient(baseUrl)` de createRoom/joinRoom.
- La función privada `sendGameMessage` queda igual.

**Criterio:** El proyecto compila con `./gradlew build -x test`.

---

## Paso 2 — Mostrar errores del servidor en la UI

**Problema:** `GameState.error` se guarda al recibir `ErrorMessage` pero ninguna
pantalla lo muestra.

**Archivo a editar:** `app/shared/src/commonMain/kotlin/org/example/project/ui/AppRoot.kt`

Añadir justo después de la línea `val colorScheme = ...`:

```kotlin
val error = state.value.error
```

Cambiar el bloque `Scaffold { ... }` por este (añadir `snackbarHostState` y `LaunchedEffect`):

```kotlin
val snackbarHostState = remember { SnackbarHostState() }

LaunchedEffect(error) {
    if (error != null) {
        snackbarHostState.showSnackbar(error)
        viewModel.dismissError()
    }
}

MaterialTheme(colorScheme = colorScheme) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (state.value.screen) {
                Screen.HOME     -> HomeScreen(viewModel)
                Screen.LOBBY    -> LobbyScreen(viewModel)
                Screen.GAME     -> GameScreen(viewModel)
                Screen.VOTING   -> VotingScreen(viewModel)
                Screen.RESULT   -> ResultScreen(viewModel)
                Screen.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}
```

Añadir los imports que falten:
```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
```

**Archivo a editar:** `app/shared/src/commonMain/kotlin/org/example/project/GameViewModel.kt`

Añadir esta función junto a las demás funciones públicas:
```kotlin
fun dismissError() {
    _state.value = _state.value.copy(error = null)
}
```

**Criterio:** Cuando el servidor envía `ErrorMessage`, aparece un Snackbar.

---

## Paso 3 — Pantalla AskWantVote (el TODO que falta)

**Problema:** Al final de una ronda el servidor envía `AskWantVote` preguntando si
los jugadores quieren votar. El ViewModel tiene `// TODO: mostrar diálogo`. Sin
manejar esto, los jugadores no responden y la ronda nunca avanza a votación.

**Archivo a editar:** `app/shared/src/commonMain/kotlin/org/example/project/GameViewModel.kt`

1. Añadir estos campos a `data class GameState`:
```kotlin
val askingForVote: Boolean = false,
val voteDeadlineMs: Long = 0L,
```

2. En `handleServerMessage`, reemplaza el bloque `is ServerMessage.AskWantVote`:
```kotlin
is ServerMessage.AskWantVote -> {
    _state.value = _state.value.copy(
        askingForVote = true,
        voteDeadlineMs = msg.deadlineEpochMs
    )
}
```

3. Añadir una función pública `respondVote`:
```kotlin
fun respondVote(wants: Boolean) {
    _state.value = _state.value.copy(askingForVote = false)
    viewModelScope.launch { sendGameMessage(ClientMessage.AnswerWantVote(wants)) }
}
```

**Archivo a editar:** `app/shared/src/commonMain/kotlin/org/example/project/ui/GameScreen.kt`

Al final del `Column` principal, antes del cierre `}`, añadir este bloque:

```kotlin
if (state.value.askingForVote) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = { viewModel.respondVote(false) },
        title = { Text("¿Votar ahora?") },
        text = { Text("¿Quieres votar para expulsar a alguien?") },
        confirmButton = {
            Button(onClick = { viewModel.respondVote(true) }) { Text("Sí") }
        },
        dismissButton = {
            Button(onClick = { viewModel.respondVote(false) }) { Text("No") }
        }
    )
}
```

**Criterio:** Cuando termina una ronda aparece el diálogo y el juego avanza.

---

## Paso 4 — Reconexión automática

**Problema:** Si cae el WebSocket el cliente se queda en la pantalla donde estaba
sin poder hacer nada.

**Archivo a editar:** `app/shared/src/commonMain/kotlin/org/example/project/GameViewModel.kt`

Añadir un campo privado:
```kotlin
private var lastFirstMessage: ClientMessage? = null
```

Guardar el primer mensaje al conectar. En `createRoom()` y `joinRoom()` añadir justo
antes de la línea `gameClient!!.connect(...)`:
```kotlin
lastFirstMessage = firstMsg
```

Añadir la función privada de reconexión. Llámala cuando el Flow termine
(añadir `.onCompletion { }` al collect):

```kotlin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onCompletion

// En createRoom() y joinRoom(), reemplaza la línea .collect por:
gameClient!!.connect(firstMsg)
    .onCompletion {
        if (_state.value.screen != Screen.HOME) {
            delay(3000)
            reconnect()
        }
    }
    .collect { msg -> handleServerMessage(msg) }
```

```kotlin
private fun reconnect() {
    val msg = lastFirstMessage ?: return
    viewModelScope.launch {
        gameClient = GameClient(baseUrl)
        gameClient!!.connect(msg)
            .onCompletion {
                if (_state.value.screen != Screen.HOME) {
                    delay(3000)
                    reconnect()
                }
            }
            .collect { handleServerMessage(it) }
    }
}
```

**Criterio:** Si el servidor se reinicia, el cliente intenta reconectar cada 3s.

---

## Paso 5 — Implementar UpdateConfig en el servidor

**Problema:** En `GameServer.kt` el handler de `UpdateConfig` tiene un TODO vacío.
El host no puede cambiar la configuración.

**Archivo a editar:** `server/src/main/kotlin/org/example/project/game/GameServer.kt`

Reemplaza el bloque `is ClientMessage.UpdateConfig -> { ... }` (líneas 73-82) por:

```kotlin
is ClientMessage.UpdateConfig -> {
    if (room != null && player?.isHost == true && room.state == RoomState.LOBBY) {
        room.config = message.config
        broadcastRoomUpdate(room)
    }
}
```

También en `Room.kt`, asegúrate de que `config` sea `var` (no `val`):
```kotlin
var config: RoomConfig,
```

**Criterio:** El host puede cambiar el modo de juego desde el lobby.

---

## Paso 6 — Arreglar el título de la app Desktop

**Archivo a editar:** `app/desktopApp/src/main/kotlin/org/example/project/main.kt`

Cambia `title = "KotlinProject"` por `title = "Impostor"`.

---

## Paso 7 — Verificar buildFatJar

El plugin de Ktor ya está en `server/build.gradle.kts`. Solo verificar que funciona:

```bash
./gradlew :server:buildFatJar
```

El resultado estará en `server/build/libs/server-all.jar`.

Para lanzarlo con PostgreSQL:
```bash
DB_URL="jdbc:postgresql://host:5432/impostor?user=user&password=pass" java -jar server/build/libs/server-all.jar
```

Si el task `buildFatJar` no existe, revisar que `libs.versions.toml` tiene el plugin
`ktor` apuntando a `io.ktor.plugin` (no a otro).

---

## Paso 8 — Verificar que compila todo

```bash
./gradlew build -x test
```

Debe terminar con `BUILD SUCCESSFUL`. Si hay errores, arreglarlos antes de continuar.

---

## Criterio de aceptación final

1. `./gradlew build -x test` → BUILD SUCCESSFUL
2. `./gradlew :server:buildFatJar` → genera el jar
3. Iniciar el servidor, abrir la app Desktop, crear sala, unirse con otro cliente,
   empezar partida, pasar turnos, votar → el juego llega a `ResultScreen`
