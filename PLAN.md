# Plan de desarrollo - Juego "Impostor"

Este plan está escrito en pasos pequeños y concretos. Sigue las fases EN ORDEN.
No saltes fases. Cada fase termina con un "Criterio de aceptación": no pases a la
siguiente fase hasta que se cumpla.

Palabras clave del juego:
- "Partida" = una sesión de juego completa. Termina cuando se expulsa al impostor.
- "Ronda" = una vuelta dentro de una partida. En una ronda cada jugador dice 1 palabra.
- "Sala" = un grupo de jugadores con un código. Una sala puede jugar muchas partidas.

---

## 0. Arquitectura y tecnologías

Reglas generales:
- Todo el estado del juego vive en el SERVIDOR. Los clientes solo muestran lo que
  el servidor envía y mandan acciones del usuario.
- Durante la partida la comunicación es por WebSocket (tiempo real).
- El registro/login y la gestión de paquetes de palabras es por HTTP REST normal.
- Las salas y partidas viven SOLO en memoria del servidor (no en base de datos).
- La base de datos guarda SOLO: usuarios y paquetes de palabras.

Módulos del proyecto (ya existen, no los crees de nuevo):
- `core`        -> Kotlin Multiplatform. Modelos de datos y protocolo de mensajes.
                   Lo usan el servidor y el cliente. NO pongas lógica de juego aquí.
- `server`      -> Ktor (JVM). Lógica del juego, WebSocket, REST, base de datos.
- `app/shared`  -> Compose Multiplatform. UI compartida y cliente WebSocket.
- `app/androidApp`, `app/desktopApp`, `app/webApp` -> arranques por plataforma.

Paquete base de código: `org.example.project`. El código nuevo va en subpaquetes:
- `org.example.project.model`     (en `core`)
- `org.example.project.protocol`  (en `core`)
- `org.example.project.game`      (en `server`)
- `org.example.project.db`        (en `server`)
- `org.example.project.ui`        (en `app/shared`)

Decisión sobre cuentas (ya decidida, no la cambies):
- Sin cuenta: solo se pueden usar las palabras base. Puedes jugar normal.
- Con cuenta: puedes crear, importar y guardar paquetes de palabras en el servidor.

---

## Fase 1 - Dependencias y configuración del proyecto

### Objetivo
Añadir todas las librerías que harán falta. Que el proyecto siga compilando.

### Archivos a editar
- `gradle/libs.versions.toml`
- `core/build.gradle.kts`
- `server/build.gradle.kts`
- `app/shared/build.gradle.kts`

### Tareas
1. En `gradle/libs.versions.toml`, sección `[versions]`, añade:
   - `kotlinx-serialization = "1.8.0"`
   - `exposed = "0.58.0"`
   - `h2 = "2.3.232"`
   - `postgresql = "42.7.4"`
   - `hikari = "6.2.1"`
2. En `[libraries]` añade entradas para:
   - `kotlinx-serialization-json` (modulo `org.jetbrains.kotlinx:kotlinx-serialization-json`)
   - `ktor-server-websockets`, `ktor-server-content-negotiation`,
     `ktor-serialization-kotlinx-json`, `ktor-server-auth`,
     `ktor-server-status-pages`, `ktor-server-cors`
   - `ktor-client-core`, `ktor-client-websockets`, `ktor-client-content-negotiation`
   - `ktor-client-cio` (para Android y Desktop), `ktor-client-js` (para Web)
   - `exposed-core`, `exposed-jdbc`, `exposed-dao`, `exposed-java-time`
   - `h2`, `postgresql`, `hikari`
3. En `[plugins]` añade `kotlinSerialization`
   (`org.jetbrains.kotlin.plugin.serialization`, version.ref `kotlin`).
4. En `core/build.gradle.kts`: aplica el plugin `kotlinSerialization` y añade
   `kotlinx-serialization-json` a `commonMain.dependencies`.
5. En `server/build.gradle.kts`: añade las dependencias `ktor-server-*` del paso 2,
   `kotlinx-serialization-json`, y todas las de Exposed + `h2` + `postgresql` + `hikari`.
   Aplica el plugin `kotlinSerialization`.
6. En `app/shared/build.gradle.kts`: aplica el plugin `kotlinSerialization`. Añade a
   `commonMain` las `ktor-client-core`, `ktor-client-websockets`,
   `ktor-client-content-negotiation` y `kotlinx-serialization-json`. Añade
   `ktor-client-cio` a `androidMain` y a la fuente JVM/desktop, y `ktor-client-js` a `jsMain`.

### Criterio de aceptación
`./gradlew build` compila sin errores.

---

## Fase 2 - Modelos compartidos (modulo `core`)

### Objetivo
Definir las clases de datos que viajan entre servidor y cliente. Son @Serializable.

### Archivos a crear
- `core/src/commonMain/kotlin/org/example/project/model/Models.kt`

### Tareas
1. Crea estos enum:
   - `RoomState { LOBBY, IN_GAME, ASK_VOTE, VOTING, FINISHED }`
   - `Role { INNOCENT, IMPOSTOR }`
   - `GameMode { VOICE, TEXT }`  (VOICE = Opcion A, TEXT = Opcion B)
   - `ImpostorMode { FIXED, RANDOM }`
2. Crea `data class PublicPlayer` con: `id: String`, `name: String`, `score: Int`,
   `connected: Boolean`, `isHost: Boolean`, `waitingNextGame: Boolean`,
   `isSpectator: Boolean`.
   (`waitingNextGame` = se unió a mitad de partida y espera a la siguiente.
    `isSpectator` = fue expulsado en esta partida y ya no juega hasta que acabe.)
3. Crea `data class RoomConfig` con valores por defecto:
   - `gameMode: GameMode = GameMode.VOICE`
   - `numImpostors: Int = 1`
   - `impostorMode: ImpostorMode = ImpostorMode.FIXED`
   - `allCanBeImpostor: Boolean = false`
   - `voteTimeLimitSeconds: Int = 60`
   - `selectedCategoryIds: List<Int> = emptyList()`
   - `language: String = "es"`
4. Crea `data class RoomSnapshot` (foto del estado de la sala que se envía a todos):
   - `code: String`, `state: RoomState`, `config: RoomConfig`
   - `players: List<PublicPlayer>`, `hostId: String`
   - `currentTurnPlayerId: String?`, `turnOrder: List<String>`, `roundNumber: Int`
5. Marca todas las clases y enums con `@Serializable` (import
   `kotlinx.serialization.Serializable`).

### Criterio de aceptación
El módulo `core` compila. Las clases se serializan a JSON sin error
(prueba simple con `Json.encodeToString`).

---

## Fase 3 - Protocolo de mensajes WebSocket (modulo `core`)

### Objetivo
Definir TODOS los mensajes que se mandan por WebSocket. Esto es un "contrato":
servidor y cliente deben usar exactamente estas clases.

### Archivos a crear
- `core/src/commonMain/kotlin/org/example/project/protocol/Messages.kt`

### Tareas
1. Crea `sealed interface ClientMessage` (mensajes del cliente al servidor).
   Cada subclase es `@Serializable`:
   - `CreateRoom(playerName: String, authToken: String? = null)`
   - `JoinRoom(roomCode: String, playerName: String, authToken: String? = null)`
   - `LeaveRoom` (objeto sin datos)
   - `UpdateConfig(config: RoomConfig)`  (solo el host puede usarlo)
   - `StartGame`  (solo el host)
   - `EndTurn`  (boton "pasar turno")
   - `SubmitWord(text: String)`  (solo en modo TEXT)
   - `AnswerWantVote(wantsToVote: Boolean)`  (responde a "¿quieres votar?")
   - `CastVote(targetPlayerId: String)`
   - `AnswerEndGame(agrees: Boolean)`  (responde a "¿terminar la partida?")
2. Crea `sealed interface ServerMessage` (mensajes del servidor al cliente).
   Cada subclase es `@Serializable`:
   - `Joined(yourPlayerId: String, room: RoomSnapshot)`
   - `RoomUpdated(room: RoomSnapshot)`  (cambió algo de la sala: jugadores, config...)
   - `GameStarted(yourRole: Role, contentIsWord: Boolean, content: String, room: RoomSnapshot)`
     (`contentIsWord = true` -> `content` es la PALABRA (inocente).
      `contentIsWord = false` -> `content` es la CATEGORIA (impostor).)
   - `TurnChanged(currentTurnPlayerId: String, roundNumber: Int)`
   - `WordPlayed(playerId: String, word: String)`  (solo modo TEXT)
   - `AskWantVote(deadlineEpochMs: Long)`  (pregunta si quieren votar)
   - `VotingStarted(candidateIds: List<String>, deadlineEpochMs: Long)`
   - `VotingResult(ejectedPlayerId: String?, wasImpostor: Boolean, room: RoomSnapshot)`
   - `RoundContinues(roundNumber: Int, room: RoomSnapshot)`
   - `GameEnded(winnerIds: List<String>, room: RoomSnapshot)`
   - `ErrorMessage(text: String)`
3. Crea un objeto `ProtocolJson` con una instancia de `Json` configurada con
   `classDiscriminator = "type"` y `ignoreUnknownKeys = true`. Sirve para
   serializar/deserializar mensajes en ambos lados.

### Criterio de aceptación
Puedes serializar un `ClientMessage.CreateRoom` a texto y volver a deserializarlo
al mismo objeto.

---

## Fase 4 - Base de datos del servidor (Exposed)

### Objetivo
Crear las tablas y poder conectar a la base de datos.

### Archivos a crear (en `server/src/main/kotlin/org/example/project/db/`)
- `Database.kt`
- `Tables.kt`

### Tareas
1. En `Tables.kt` define estas tablas Exposed (`IntIdTable`):
   - `Users`: `username` varchar(32) único, `passwordHash` varchar(100),
     `createdAt` long.
   - `Sessions`: `token` varchar(64) único, `userId` reference a `Users`,
     `expiresAt` long.
   - `WordPacks`: `name` varchar(64), `language` varchar(8),
     `ownerUserId` reference a `Users` NULLABLE (null = paquete base del juego),
     `isBuiltIn` bool default false.
   - `Categories`: `packId` reference a `WordPacks`, `name` varchar(64).
   - `Words`: `categoryId` reference a `Categories`, `text` varchar(64).
2. En `Database.kt` crea una función `initDatabase()` que:
   - Lea la URL de la BD de una variable de entorno `DB_URL`.
     Si no existe, usa H2 en fichero: `jdbc:h2:./data/impostor;DB_CLOSE_DELAY=-1`.
   - Configure un pool HikariCP con esa URL.
   - Conecte Exposed (`Database.connect`).
   - Cree las tablas con `SchemaUtils.create(...)` dentro de `transaction { }`.
3. Llama a `initDatabase()` al arrancar el servidor (lo conectarás en la Fase 5).

### Criterio de aceptación
Al arrancar el servidor se crea el fichero `data/impostor.mv.db` y no hay errores.

---

## Fase 5 - Palabras base y carga de paquetes

### Objetivo
El juego debe traer entre 1000 y 3000 palabras por defecto.

### Archivos a crear
- `server/src/main/resources/words/base_es.json`  (paquete base en español)
- `server/src/main/kotlin/org/example/project/db/WordSeeder.kt`
- `server/src/main/kotlin/org/example/project/db/WordRepository.kt`

### Formato JSON de un paquete de palabras (úsalo también para importar)
```json
{
  "name": "Paquete base",
  "language": "es",
  "categories": [
    { "name": "Animales", "words": ["perro", "gato", "elefante"] },
    { "name": "Comida",   "words": ["pizza", "manzana", "arroz"] }
  ]
}
```

### Tareas
1. Crea `base_es.json` con al menos 20-30 categorías y un total de 1000 a 3000
   palabras. Cada categoría debe tener varias palabras. (Puedes generarlas por
   temas: animales, comida, deportes, países, profesiones, objetos de casa, etc.)
2. En `WordSeeder.kt` crea `seedBaseWordsIfEmpty()` que:
   - Comprueba si ya existe un `WordPack` con `isBuiltIn = true`. Si existe, no hace nada.
   - Si no existe, lee `base_es.json` desde resources, lo deserializa y crea el
     `WordPack` (isBuiltIn=true, ownerUserId=null) con sus `Categories` y `Words`.
3. En `WordRepository.kt` crea funciones:
   - `listCategories(forUserId: Int?): List<CategoryDto>` -> categorías de paquetes
     base + (si `forUserId` no es null) las de los paquetes de ese usuario.
   - `randomWordFrom(categoryIds: List<Int>): Pair<String, String>?` -> devuelve
     una palabra al azar y el nombre de su categoría. Si la lista está vacía, usa
     todas las categorías base.
   - `importPack(ownerUserId: Int, json: String): Int` -> crea un paquete nuevo
     para ese usuario a partir del JSON. Devuelve el id del paquete.
   - `deletePack(ownerUserId: Int, packId: Int)` -> borra el paquete solo si es
     de ese usuario y no es base.
4. Llama a `seedBaseWordsIfEmpty()` al arrancar, después de `initDatabase()`.

### Criterio de aceptación
Tras el primer arranque, la tabla `Words` tiene entre 1000 y 3000 filas.

---

## Fase 6 - Servidor base, REST y autenticación

### Objetivo
Arrancar Ktor con WebSocket, JSON y los endpoints de cuentas y paquetes.

### Archivos a editar/crear
- `server/src/main/kotlin/org/example/project/Application.kt`  (editar)
- `server/src/main/kotlin/org/example/project/AuthRoutes.kt`
- `server/src/main/kotlin/org/example/project/PackRoutes.kt`
- `server/src/main/kotlin/org/example/project/AuthService.kt`

### Tareas
1. En `Application.kt`, dentro de `embeddedServer(...)`, instala los plugins:
   `ContentNegotiation` (con `json(ProtocolJson.json)`), `WebSockets`,
   `StatusPages`, `CORS` (permite cualquier host para desarrollo).
   Llama a `initDatabase()` y `seedBaseWordsIfEmpty()` antes de `routing { }`.
2. En `AuthService.kt`:
   - `hashPassword(raw): String` y `verifyPassword(raw, hash): Boolean`.
     Usa PBKDF2 del JDK (`SecretKeyFactory` con `PBKDF2WithHmacSHA256`) con sal
     aleatoria; guarda sal+hash juntos en el string. No inventes tu propio hash.
   - `createSession(userId): String` -> genera un token UUID, lo guarda en
     `Sessions` con expiración a 30 días, lo devuelve.
   - `userIdForToken(token): Int?` -> devuelve el userId si el token existe y no
     ha caducado; si no, null.
3. En `AuthRoutes.kt` define rutas REST:
   - `POST /auth/register`  body `{username, password}` -> crea usuario (rechaza
     si el username ya existe), devuelve `{token}`.
   - `POST /auth/login`  body `{username, password}` -> verifica, devuelve `{token}`.
4. En `PackRoutes.kt` define rutas REST. El token llega en header
   `Authorization: Bearer <token>`:
   - `GET /packs/categories` -> lista de categorías (base + del usuario si hay token).
   - `POST /packs/import`  body = JSON de paquete -> requiere token, llama a
     `importPack`. Sin token: responde 401.
   - `DELETE /packs/{id}` -> requiere token, llama a `deletePack`.
5. Registra todas las rutas en el bloque `routing { }`.

### Criterio de aceptación
Con `curl` puedes registrar un usuario, hacer login y obtener un token; y
`GET /packs/categories` devuelve la lista de categorías base.

---

## Fase 7 - Motor de juego en el servidor

### Objetivo
Implementar toda la lógica de salas y partidas. Esta es la fase más importante.

### Archivos a crear (en `server/src/main/kotlin/org/example/project/game/`)
- `Player.kt`        (jugador conectado: id, nombre, sesión WebSocket, userId?)
- `Room.kt`          (estado de una sala + su partida actual)
- `RoomManager.kt`   (mapa de todas las salas; crea/busca/borra salas)
- `GameEngine.kt`    (funciones que avanzan la partida)
- `GameServer.kt`    (recibe ClientMessage y llama al motor; envía ServerMessage)

### Conceptos de estado (manténlos en `Room`)
- `code: String` (6 caracteres mayúsculas/dígitos, único).
- `players: lista de Player` (orden de llegada).
- `config: RoomConfig`.
- `state: RoomState`.
- Datos de la partida actual (solo válidos si `state` != LOBBY/FINISHED):
  `word: String`, `category: String`, `impostorIds: Set<String>`,
  `turnOrder: List<String>`, `currentTurnIndex: Int`, `roundNumber: Int`,
  `playedThisRound: Set<String>`, mapa de votos, mapa de respuestas "¿votar?".

### Reglas (impleméntalas EXACTAMENTE así)

Crear/unir sala:
- `CreateRoom`: genera código único, crea sala en LOBBY, el creador es host.
- `JoinRoom`: busca la sala por código. Error si no existe o si ya hay 10 jugadores.
  - Si la sala está en LOBBY: el jugador entra normal.
  - Si la sala está jugando (cualquier otro estado): entra con `waitingNextGame=true`
    y NO juega hasta la siguiente partida.
- Capacidad: mínimo 3, máximo 10 jugadores.

Salir de la sala (`LeaveRoom` o desconexión del WebSocket):
- Quita al jugador de la sala. Envía `RoomUpdated` a los demás.
- Si era su turno en una ronda: el turno pasa automáticamente al siguiente jugador.
- Si la votación está en curso: su voto (si no había votado) no cuenta.
- Si el que era host se va: el siguiente jugador de la lista pasa a ser host.
- Si quedan menos de 3 jugadores: termina la partida y elimina la sala.

Empezar partida (`StartGame`, solo host, solo en LOBBY o FINISHED, min 3 jugadores):
- Los jugadores con `waitingNextGame=true` pasan a `waitingNextGame=false`.
- Reinicia `isSpectator=false` para todos.
- Elige la palabra: `randomWordFrom(config.selectedCategoryIds)` -> palabra+categoría.
- Elige impostores:
  - Cuántos: `numImpostors`. Si `impostorMode=RANDOM`, elige un número al azar
    entre 1 y (jugadores - 1). Si `allCanBeImpostor=true`, el número puede llegar
    hasta jugadores-1.
  - Elige esos jugadores al azar.
- A cada jugador inocente: `GameStarted(role=INNOCENT, contentIsWord=true, content=word)`.
- A cada impostor: `GameStarted(role=IMPOSTOR, contentIsWord=false, content=category)`.
- `turnOrder` = jugadores activos en orden; `currentTurnIndex=0`; `roundNumber=1`.
- Estado IN_GAME. Envía `TurnChanged` con el primer jugador.

Turnos (`EndTurn`, solo lo puede mandar el jugador del turno actual):
- Marca al jugador actual en `playedThisRound`.
- Avanza `currentTurnIndex` al siguiente jugador activo (no espectador, conectado).
- Si TODOS los jugadores activos ya jugaron esta ronda:
  -> estado ASK_VOTE, envía `AskWantVote` con un deadline = ahora + voteTimeLimit.
- Si no, envía `TurnChanged` con el nuevo jugador.

Pregunta "¿queréis votar?" (`AnswerWantVote`):
- Guarda la respuesta de cada jugador activo.
- Cuando todos responden, o cuando vence el deadline (los que no respondieron
  cuentan como "no"):
  - Si el número de "sí" es MAYOR que la mitad de jugadores activos -> empieza
    votación: estado VOTING, envía `VotingStarted` con candidatos = todos los
    jugadores activos y deadline = ahora + voteTimeLimit.
  - Si no hay mayoría de "sí" -> el impostor sobrevive la ronda: cada impostor
    suma +1 punto, empieza una ronda nueva (ver "Nueva ronda").

Votación (`CastVote`, no se puede saltar):
- Guarda el voto de cada jugador (target). Un jugador no puede votarse... (permitido,
  decide tú; por defecto SÍ se permite votarse a sí mismo, es más simple).
- Al vencer el deadline o cuando todos los activos hayan votado:
  - Recuento: cuenta votos por jugador.
  - Los jugadores activos que NO votaron: su voto se asigna automáticamente al
    jugador más votado en ese momento (al "target mayoritario"). Si los recuentas
    y siguen sin mayoría, sigue al siguiente paso.
  - Si hay empate en el primer puesto -> el expulsado se elige AL AZAR entre los
    empatados.
  - Determina `ejected`.
  - Si `ejected` es impostor -> GANAN LOS INOCENTES: ver "Fin de partida (inocentes)".
  - Si `ejected` NO es impostor -> el inocente expulsado pasa a `isSpectator=true`,
    cada impostor suma +1 punto, y empieza una ronda nueva.
  - Envía `VotingResult(ejectedPlayerId, wasImpostor, room)`.

Nueva ronda:
- `roundNumber++`, `playedThisRound` vacío, `currentTurnIndex` al primer jugador
  activo, limpia votos y respuestas de "¿votar?".
- Estado IN_GAME. Envía `RoundContinues(roundNumber, room)` y luego `TurnChanged`.

Fin de partida (inocentes ganan):
- Ganadores = todos los jugadores que NO son impostores (incluidos espectadores
  que fueron inocentes). Cada ganador suma +1 punto.
- (El impostor ya fue sumando +1 por cada ronda que sobrevivió, no se le quita.)
- Estado FINISHED. Envía `GameEnded(winnerIds, room)`.

Terminar partida por acuerdo (`AnswerEndGame`):
- Cuando un jugador propone terminar (puedes activar esto con el primer
  `AnswerEndGame(agrees=true)`), se pregunta a todos. Si TODOS los activos
  responden `agrees=true`, la partida termina: estado FINISHED, `GameEnded`
  con `winnerIds` vacío (nadie gana puntos extra).

Sala sin gente:
- En cualquier momento, si los jugadores activos quedan por debajo de 3 y había
  partida en curso: termina la partida (FINISHED). Si la sala se queda con 0-2
  jugadores en LOBBY: elimina la sala del `RoomManager`.

### GameServer (pegamento WebSocket)
- Endpoint WebSocket `GET /ws/game`.
- Por cada conexión: lee mensajes de texto, deserialízalos con `ProtocolJson` a
  `ClientMessage`, y pásalos a la función correspondiente del motor.
- Para enviar: serializa `ServerMessage` y manda por el WebSocket de cada jugador.
- Cuando la conexión se cierra: trata al jugador como "salió de la sala".
- Toda modificación de una sala debe estar protegida por un `Mutex` por sala
  (evita problemas de concurrencia).

### Criterio de aceptación
Con un script de prueba (o tests de Ktor) puedes: crear sala, unir 3 clientes,
empezar partida, pasar turnos, pedir votación, votar y recibir `GameEnded`.

---

## Fase 8 - Cliente: conexión y estado (modulo `app/shared`)

### Objetivo
Conectar el cliente al servidor y mantener el estado de la UI.

### Archivos a crear (en `app/shared/src/commonMain/kotlin/org/example/project/`)
- `net/GameClient.kt`
- `net/ApiClient.kt`
- `GameViewModel.kt`

### Tareas
1. `ApiClient.kt`: usa `HttpClient` de Ktor para llamar a los endpoints REST
   (`register`, `login`, `getCategories`, `importPack`). Guarda el token en memoria.
2. `GameClient.kt`:
   - Función `connect()` que abre un WebSocket a `/ws/game`.
   - Función `send(msg: ClientMessage)` que serializa y envía.
   - Un `Flow<ServerMessage>` con los mensajes entrantes deserializados.
3. `GameViewModel.kt`: una clase con estado observable (usa `StateFlow` o
   `mutableStateOf` de Compose). Campos del estado:
   - pantalla actual (HOME, LOBBY, GAME, VOTING, RESULT)
   - `RoomSnapshot` actual, `yourPlayerId`, tu `Role`, tu palabra/categoría
   - lista de palabras dichas (modo TEXT), resultado de votación
   - El ViewModel escucha el `Flow<ServerMessage>` y actualiza el estado según
     cada mensaje. Expone funciones como `createRoom()`, `joinRoom()`,
     `startGame()`, `endTurn()`, `castVote()`, etc. que llaman a `send(...)`.
4. La URL del servidor debe ser configurable (constante por ahora:
   `http://10.0.2.2:8080` para emulador Android, `http://localhost:8080` para
   Desktop/Web).

### Criterio de aceptación
El cliente se conecta, crea una sala y recibe `Joined`; el estado del ViewModel
refleja la sala.

---

## Fase 9 - Cliente: pantallas UI (Modo A, prioridad ALTA)

### Objetivo
Pantallas en Compose Multiplatform para el modo de voz externa (Opcion A).

### Archivos a crear (en `app/shared/src/commonMain/kotlin/org/example/project/ui/`)
- `HomeScreen.kt`, `LobbyScreen.kt`, `GameScreen.kt`, `VotingScreen.kt`,
  `ResultScreen.kt`, `AppRoot.kt`

### Tareas
1. `AppRoot.kt`: un `@Composable` que mira la pantalla actual del `GameViewModel`
   y muestra la pantalla correspondiente.
2. `HomeScreen.kt`: campo de nombre, botón "Crear sala", campo de código + botón
   "Unirse a sala". (Opcional: botón de login.)
3. `LobbyScreen.kt`: muestra el código de la sala, la lista de jugadores, y un
   panel de configuración (solo editable por el host): modo de juego, número de
   impostores, modo de impostor, tiempo de voto, selección de categorías.
   Botón "Empezar partida" visible solo para el host.
4. `GameScreen.kt` (Modo A):
   - En el CENTRO: tu rol (Inocente / Impostor) y tu palabra o categoría,
     indicando claramente si es "PALABRA" o "CATEGORIA".
   - Los jugadores colocados en círculo / bordeando la pantalla, en el orden de
     turno, con su nombre. Resalta de quién es el turno actual.
   - Botón "Pasar turno", activado solo si es tu turno.
5. `VotingScreen.kt`: aparece en estado VOTING. Menú claro de selección de
   jugadores (distinto al de la pantalla de juego). Muestra el tiempo restante.
   No hay botón de "saltar".
6. `ResultScreen.kt`: muestra quién era el impostor, quién ganó y la puntuación
   de todos. Botón "Volver al lobby".
7. Conecta los botones con las funciones del `GameViewModel`.

### Criterio de aceptación
Una partida completa se puede jugar de principio a fin desde la UI en Desktop.

---

## Fase 10 - Modo B: chat de texto (prioridad BAJA)

### Objetivo
Añadir el modo de juego con chat de texto dentro de la app.

### Tareas
1. En `GameScreen.kt`, cuando `config.gameMode == TEXT`:
   - Muestra, junto a cada jugador, la última palabra que dijo.
   - Cuando sea tu turno, muestra un campo de texto para escribir la palabra.
     Al enviar, manda `SubmitWord(text)` (eso también cuenta como pasar turno).
2. En el servidor, maneja `SubmitWord`: guarda la palabra como "última palabra"
   del jugador, envía `WordPlayed` a todos, y avanza el turno igual que `EndTurn`.
3. El ViewModel guarda el mapa de últimas palabras y la UI lo muestra.

### Criterio de aceptación
Una partida en modo TEXT funciona: las palabras escritas se ven en pantalla.

---

## Fase 11 - Ajustes de usuario

### Objetivo
Menú de ajustes: idioma, sonido, tema (claro/oscuro).

### Tareas
1. Crea `SettingsScreen.kt` con: selector de idioma, interruptor de sonido,
   selector de tema (claro/oscuro/sistema).
2. Guarda los ajustes localmente en cada plataforma (usa un almacenamiento
   simple `expect/actual`: SharedPreferences en Android, fichero en Desktop,
   localStorage en Web).
3. Aplica el tema con un `MaterialTheme` que cambie entre claro y oscuro.
4. El idioma controla los textos de la UI (haz un mapa simple de traducciones).

### Criterio de aceptación
Cambiar el tema y el idioma se nota al instante y se conserva al reabrir la app.

---

## Fase 12 - Pulido, pruebas y despliegue

### Tareas
1. Maneja errores: si el servidor manda `ErrorMessage`, muéstralo en la UI.
2. Reconexión: si se cae el WebSocket, intenta reconectar y volver a la sala.
3. Prueba los casos límite: jugador que se va a mitad de turno, a mitad de voto,
   sala que se queda con menos de 3, empates en la votación.
4. Verifica que arranca en Android, Desktop y Web.
5. Empaqueta el servidor con `./gradlew :server:buildFatJar` (plugin de Ktor) y
   documenta cómo lanzarlo con PostgreSQL (variable de entorno `DB_URL`).

### Criterio de aceptación
El juego funciona completo en las tres plataformas contra un servidor desplegado.

---

## Resumen del orden de trabajo
1. Dependencias  ->  2. Modelos  ->  3. Protocolo  ->  4. Base de datos
->  5. Palabras base  ->  6. REST + auth  ->  7. Motor de juego (lo más grande)
->  8. Cliente conexión  ->  9. UI Modo A  ->  10. Modo B  ->  11. Ajustes
->  12. Pulido.

Las fases 1-7 son el núcleo. La fase 9 (UI Modo A) tiene prioridad sobre la 10.
