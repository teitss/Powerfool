package br.com.pixelmonbrasil.powerfool.manager

import com.google.common.reflect.TypeToken
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.rabbitmq.client.CancelCallback
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.DeliverCallback
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Level

class MessageManager {

    var connection: Connection

    init {
        val factory = ConnectionFactory()
        factory.host = Powerfool.INSTANCE.config.getString("messagebroker.host")
        factory.username = Powerfool.INSTANCE.config.getString("messagebroker.username")
        factory.password = Powerfool.INSTANCE.config.getString("messagebroker.password")
        factory.port = Powerfool.INSTANCE.config.getInt("messagebroker.port")

        val gson = GsonBuilder().create()
        val parser = JsonParser()
        connection = factory.newConnection()
        val registerChannel = connection.createChannel()
        val updateChannel = connection.createChannel()
        registerChannel.queueDeclare("register_server", true, false, false, null)
        updateChannel.queueDeclare("update_server", true, false, false, null)
        val registerCallback = DeliverCallback { _, delivery ->
            val payload = String(delivery.body, StandardCharsets.UTF_8)
            val server = gson.fromJson<Server>(payload, Server::class.java)
            server.lastUpdate = System.currentTimeMillis()
            Powerfool.INSTANCE.servers[server.bungeeId] = server
            Powerfool.INSTANCE.getLogger().log(Level.INFO, "${server.bungeeId} successfully registered!")
        }
        val updateCallback = DeliverCallback { _, delivery ->
            val payload = String(delivery.body, StandardCharsets.UTF_8)
            val json = parser.parse(payload).asJsonObject
            val server = Powerfool.INSTANCE.servers[json["bungeeId"].asString]
            if (server != null) {
                json.entrySet().forEach { entry ->
                    when (entry.key) {
                        "serverName" -> server.serverName = entry.value.asString
                        "itemString" -> server.itemString = entry.value.asString
                        "description" -> server.description = gson.fromJson<Array<String>>(
                            entry.value.asString,
                            object : TypeToken<Array<String>>() {}.type
                        )
                        "vips" -> server.vips = gson.fromJson<Array<UUID>>(
                            entry.value.asString,
                            object : TypeToken<Array<UUID>>() {}.type
                        )
                        "desiredPosition" -> server.desiredPosition = entry.value.asInt
                        "onlinePlayers" -> server.onlinePlayers = entry.value.asInt
                        "maxPlayers" -> server.maxPlayers = entry.value.asInt
                        "isVisible" -> server.isVisible = entry.value.asBoolean
                        "onlyVips" -> server.onlyVips = entry.value.asBoolean
                        "isHighlightServer" -> server.isHighlightServer = entry.value.asBoolean
                    }
                }
                server.lastUpdate = System.currentTimeMillis()
            }
        }
        registerChannel.basicConsume("register_server", true, registerCallback, CancelCallback {  })
        updateChannel.basicConsume("update_server", true, updateCallback, CancelCallback {  })
    }




}