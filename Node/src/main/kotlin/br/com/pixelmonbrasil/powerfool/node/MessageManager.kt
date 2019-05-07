package br.com.pixelmonbrasil.powerfool.node

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.spongepowered.api.util.TypeTokens

class MessageManager {

    val gson = GsonBuilder().create()
    var channel: Channel
    var connection: Connection

    init {
        val factory = ConnectionFactory()
        factory.host = Powerfool.INSTANCE.config.getNode("messagebroker", "host").string
        factory.username = Powerfool.INSTANCE.config.getNode("messagebroker", "username").string
        factory.password = Powerfool.INSTANCE.config.getNode("messagebroker", "password").string
        factory.port = Powerfool.INSTANCE.config.getNode("messagebroker", "port").int

        connection = factory.newConnection()
        channel = connection.createChannel()
        channel.queueDeclare("register_server", true, false, false, null)
        channel.queueDeclare("update_server", true, false, false, null)
    }

    fun sendRegister() {
        val config = Powerfool.INSTANCE.config
        val server = Server(config.getNode("bungeeId").string!!,
            config.getNode("serverName").string!!,
            config.getNode("itemString").string!!,
            config.getNode("description").getList(TypeTokens.STRING_TOKEN).toTypedArray(),
            Powerfool.INSTANCE.vips.toTypedArray(),
            config.getNode("desiredPosition").int,
            0,
            config.getNode("maxPlayers").int,
            config.getNode("isVisible").boolean,
            config.getNode("onlyVips").boolean,
            config.getNode("isHighlightServer").boolean,
            0)
        val json = gson.toJson(server)
        channel.basicPublish("", "register_server", null, json.toByteArray())
    }

    fun sendUpdate(property: Pair<String, String>) {
        val json = JsonObject().apply {
            this.addProperty("bungeeId", Powerfool.INSTANCE.config.getNode("bungeeId").string!!)
            this.addProperty(property.first, property.second)
        }
        channel.basicPublish("", "update_server", null, gson.toJson(json).toByteArray())
    }

}