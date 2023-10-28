package studio.papercube.mindustryplugins

import arc.Events
import arc.util.CommandHandler
import arc.util.Log
import mindustry.Vars
import mindustry.core.GameState
import mindustry.game.EventType
import mindustry.game.EventType.UnitBulletDestroyEvent
import mindustry.game.EventType.UnitChangeEvent
import mindustry.game.EventType.UnitCreateEvent
import mindustry.game.EventType.UnitDestroyEvent
import mindustry.game.EventType.UnitSpawnEvent
import mindustry.game.EventType.UnitUnloadEvent
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.mod.Plugin
import java.util.*

class PlayerPauseCommandPlugin : Plugin() {
    val teleportHandler = TeleportHandler()

    private fun registerDebuggingEvents() {
        // Events: https://github.com/Anuken/Mindustry/blob/master/core/src/mindustry/game/EventType.java
        Events.on(UnitDestroyEvent::class.java) { event: UnitDestroyEvent ->
            println("Unit destroyed: ${event.unit} (unit.player: ${event.unit.player}, controller: ${event.unit.controller()})")
            if (event.unit.player != null) {
                event.unit.player.sendMessage("You died!")
            }
        }

        Events.on(UnitChangeEvent::class.java) { event: UnitChangeEvent ->
            println("Unit changed: player=${event.player}, unit=${event.unit} (unit.player: ${event.unit.player}, controller: ${event.unit.controller()})")
            println(" ---- Unit Position: ${event.unit.tileX()}, ${event.unit.tileY()} ---- ")
            println(" ---- Player Position: ${event.player.tileX()}, ${event.player.tileY()} ---- ")
        }

        Events.on(UnitBulletDestroyEvent::class.java) { event: UnitBulletDestroyEvent ->
            println("Unit bullet destroyed: bullet=${event.bullet}, unit=${event.unit} (unit.player: ${event.unit.player}, unit.controller: ${event.unit.controller()})")
        }

        Events.on(UnitCreateEvent::class.java) { event: UnitCreateEvent ->
            // show unit, spawner, spawerUnit
            println("Unit created: unit=${event.unit}, spawner=${event.spawner}, spawnerUnit=${event.spawnerUnit}")
        }

        Events.on(UnitSpawnEvent::class.java) { event: UnitSpawnEvent ->
            println("Unit spawned: unit=${event.unit}")
        }

        Events.on(UnitUnloadEvent::class.java) {
            println("Unit unloaded: unit=${it.unit}")
        }

        Events.run(EventType.Trigger.update) {
            if (Vars.state.`is`(GameState.State.playing)) {
                Groups.player.forEach { player ->
                    if (player.unit() == null) {
                        player.sendMessage("You died!")
                    }
                }
            }
        }
    }

    //called when game initializes
    override fun init() {
//        registerDebuggingEvents()
        Events.on(UnitChangeEvent::class.java, teleportHandler::handleUnitChangeEvent)
        /*
        //listen for a block selection event
        Events.on(BuildSelectEvent::class.java) { event: BuildSelectEvent ->
            if (!event.breaking && event.builder != null && event.builder.buildPlan() != null && event.builder.buildPlan().block === Blocks.thoriumReactor && event.builder.isPlayer) {
                //player is the unit controller
                val player = event.builder.player

                //send a message to everyone saying that this player has begun building a reactor
                Call.sendMessage("[scarlet]ALERT![] " + player.name + " has begun building a reactor at " + event.tile.x + ", " + event.tile.y)
            }
        }

        //add a chat filter that changes the contents of all messages
        //in this case, all instances of "heck" are censored
        Vars.netServer.admins.addChatFilter { player: Player?, text: String -> text.replace("heck", "h*ck") }

        //add an action filter for preventing players from doing certain things
        Vars.netServer.admins.addActionFilter { action: PlayerAction ->
            //random example: prevent blast compound depositing
            if (action.type == ActionType.depositItem && action.item === Items.blastCompound && action.tile.block() is CoreBlock) {
                action.player.sendMessage("Example action filter: Prevents players from depositing blast compound into the core.")
                return@addActionFilter false
            }
            true
        }
        */
    }

    //register commands that run on the server
    override fun registerServerCommands(handler: CommandHandler) {
        /*
        handler.register("reactors", "List all thorium reactors in the map.") { args: Array<String?>? ->
            for (x in 0 until Vars.world.width()) {
                for (y in 0 until Vars.world.height()) {
                    //loop through and log all found reactors
                    //make sure to only log reactor centers
                    if (Vars.world.tile(x, y).block() === Blocks.thoriumReactor && Vars.world.tile(x, y).isCenter) {
                        Log.info("Reactor at @, @", x, y)
                    }
                }
            }
        }
        */
    }

    //register commands that player can invoke in-game
    override fun registerClientCommands(handler: CommandHandler) {
        handler.register("pause", "<on|off>", "Specify paused state") { args: Array<String>, player: Player ->
            if (args.isEmpty()) {
                player.sendMessage("[accent]Insufficient args.")
                return@register
            }
            val option = args[0].trim().lowercase(Locale.getDefault())
            if (option != "on" && option != "off") {
                player.sendMessage("[accent]Unrecognized option ${args[0]}")
            }
            setGamePaused(option == "on", player)
        }

        handler.register("p", "Toggle game paused state") { args: Array<String>, player: Player ->
            toggleGamePaused(player)
        }

        handler.register(
            "tp",
            "<x|username> [y]",
            "Teleport to specified location or user"
        ) { args: Array<String>, player: Player ->
            when (args.size) {
                2 -> run {
                    val px = args[0].toDoubleOrNull()?.toInt()
                    val py = args[1].toDoubleOrNull()?.toInt()
                    if (px == null || py == null) {
                        player.sendMessage("[accent]Invalid coordinates")
                        return@run
                    }

                    teleportHandler.teleport(player, px, py)
                }

                1 -> run {
                    val (cnt, targetPlayer) = findPlayerByName(args[0].filter { !it.isWhitespace() }, player)
                    when(cnt) {
                        0 -> player.sendMessage("[accent]User ${args[0]}[accent] not found or you cannot teleport to yourself.")
                        1 -> teleportHandler.teleport(player, targetPlayer!!.tileX(), targetPlayer.tileY(), forceCheck = false)
                        else -> player.sendMessage("[accent]Ambiguous Match. Please specify user name in full")
                    }

                }

                else -> player.sendMessage("[accent]Invalid number of arguments")
            }
        }

        handler.register("back", "Teleport to previous location") { args: Array<String>, player: Player ->
            teleportHandler.returnToLastPosition(player)
        }

        handler.register("here", "Teleport to where your mouse is pointing at") {args: Array<String>, player: Player ->
            val px = player.mouseX() / Vars.tilesize
            val py = player.mouseY() / Vars.tilesize

            teleportHandler.teleport(player, px.toInt(), py.toInt(), positionDescription = "the pointed location")
        }

        /*

        //register a simple reply command
        handler.register(
            "reply",
            "<text...>",
            "A simple ping command that echoes a player's text."
        ) { args: Array<String>, player: Player -> player.sendMessage("You said: [accent] " + args[0]) }

        //register a whisper command which can be used to send other players messages
        handler.register(
            "whisper",
            "<player> <text...>",
            "Whisper text to another player."
        ) { args: Array<String>, player: Player ->
            //find player by name
            val other = Groups.player.find { p: Player -> p.name.equals(args[0], ignoreCase = true) }

            //give error message with scarlet-colored text if player isn't found
            if (other == null) {
                player.sendMessage("[scarlet]No player by that name found!")
                return@register
            }

            //send the other player a message, using [lightgray] for gray text color and [] to reset color
            other.sendMessage("[lightgray](whisper) " + player.name + ":[] " + args[1])
        }

        */
    }

    private fun setGamePaused(paused: Boolean, invoker: Player?) {
        Vars.state.set(
            when (paused) {
                true -> GameState.State.paused
                false -> GameState.State.playing
            }
        )
        val msg = "${invoker?.name} [accent]has ${if (paused) "paused" else "resumed"} the game. "
        Log.info(MindustryTextUtil.removeColorMarkups(msg))
        Groups.player.forEach {
            it.sendMessage(msg)
        }
    }

    private fun toggleGamePaused(invoker: Player? = null) {
        setGamePaused(!Vars.state.isPaused, invoker)
    }

    private fun findPlayerByName(name: String, self: Player?): Pair<Int, Player?> {
        val players = Groups.player.toList().filter { it.id != self?.id }
        val targetPlayer = players.filter { it.name.contains(name, ignoreCase = true) }
        val uniquePlayer = players.find { it.name.equals(name, ignoreCase = true) }
        if (uniquePlayer != null) {
            return Pair(1, uniquePlayer)
        }
        return when {
            targetPlayer.isEmpty() -> Pair(0, null)
            targetPlayer.size >= 2 -> Pair(targetPlayer.size, null)
            else -> Pair(1, targetPlayer[0])
        }
    }


}
