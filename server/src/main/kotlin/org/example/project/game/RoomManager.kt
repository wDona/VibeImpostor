package org.example.project.game

import org.example.project.model.RoomConfig
import kotlin.random.Random

object RoomManager {
    private val rooms = mutableMapOf<String, Room>()
    private val random = Random(System.currentTimeMillis())

    fun generateCode(): String {
        while (true) {
            val code = (0..5).map { ('A'..'Z') + ('0'..'9') }.map { it.random(random) }.joinToString("")
            if (!rooms.containsKey(code)) return code
        }
    }

    fun createRoom(hostId: String, config: RoomConfig): Room {
        val code = generateCode()
        val room = Room(code, hostId, config)
        rooms[code] = room
        return room
    }

    fun findRoom(code: String): Room? = rooms[code]

    fun deleteRoom(code: String) {
        rooms.remove(code)
    }

    fun getAllRooms(): List<Room> = rooms.values.toList()
}
