package studio.papercube.mindustryplugins

import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Player
import java.util.*
import kotlin.collections.ArrayDeque

class TeleportHandler {
    private data class UnitPosition(val x: Int, val y: Int)

    private val lastPosition: HashMap<String, ArrayDeque<UnitPosition>> = HashMap()

    fun teleport(
        player: Player,
        x: Int,
        y: Int,
        forceCheck: Boolean = true,
        positionDescription: String = "",
        recordLastPosition: Boolean = true
    ) {
        if (forceCheck && !(x in 0..Vars.world.width() && y in 0..Vars.world.height())) {
            player.sendMessage("Coordinates out of bounds. Must be within 0..${Vars.world.width()}, 0..${Vars.world.height()}")
            return
        }

        val px = x * Vars.tilesize.toFloat()
        val py = y * Vars.tilesize.toFloat()

        val playerUnit = player.unit()

        if (recordLastPosition) recordLastPosition(player)

        val positionDescriptionSpace = if (positionDescription.isEmpty()) "" else " "
        player.sendMessage("Teleported to $positionDescription$positionDescriptionSpace$x, $y")

        object : Thread("Teleporter Unit-" + playerUnit.id) {
            var limit = 60
            override fun run() {
                for (i in 0..9) move()
                while (!playerUnit.within(px, py, 2f * Vars.tilesize) && limit-- > 0) move()
            }

            fun move() {
                playerUnit.set(px, py)
                player.set(px, py)
                Call.setPosition(player.con, px, py)
                sleep(16)
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun returnToLastPosition(player: Player) {
        val uuid = player.uuid()
        lastPosition[uuid]?.let {
            it.removeLastOrNull()?.let {
                teleport(player, it.x, it.y, false, "the last position", recordLastPosition = false)
            }
        } ?: player.sendMessage("[accent]No last position recorded.")
    }

    fun handleUnitChangeEvent(unitChangeEvent: EventType.UnitChangeEvent?) {
        if (unitChangeEvent == null) return
        val unit = unitChangeEvent.unit
        val player = unitChangeEvent.player
//        Log.info("UnitChangeEvent: $unit, $player, unit.isNull=${unit.isNull}, unit::class.java=${unit::class.java}, ~.typeName=${unit::class.java.typeName}, unit.type=${unit.type}, unit.dead=${unit.dead}, unit.type.name=${unit.type.name}, player?.name=${player?.name}")
        if (unit.isNull && player != null) {
            recordLastPosition(player)
        }
    }

    private fun recordLastPosition(player: Player) {
        lastPosition.computeIfAbsent(player.uuid()) { ArrayDeque() }.apply {
            add(UnitPosition(player.tileX(), player.tileY()))
            while (size > 30) removeFirst()
        }
//        lastPosition[player.uuid()] = UnitPosition(player.tileX(), player.tileY())
    }
}