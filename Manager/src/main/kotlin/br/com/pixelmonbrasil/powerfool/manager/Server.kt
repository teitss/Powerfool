package br.com.pixelmonbrasil.powerfool.manager

import java.util.*

data class Server(
    val bungeeId: String,
    var serverName: String,
    var itemString: String,
    var description: Array<String>,
    var vips: Array<UUID>,
    var desiredPosition: Int,
    var onlinePlayers: Int,
    var maxPlayers: Int,
    var isVisible: Boolean,
    var onlyVips: Boolean,
    var isHighlightServer: Boolean,
    var lastUpdate: Long) {
}