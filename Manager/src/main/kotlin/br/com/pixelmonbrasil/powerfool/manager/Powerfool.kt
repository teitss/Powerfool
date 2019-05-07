package br.com.pixelmonbrasil.powerfool.manager

import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.properties.Delegates

class Powerfool: JavaPlugin() {

    val servers: HashMap<String, Server> = HashMap()
    lateinit var messageManager: MessageManager

    companion object {
        var INSTANCE: Powerfool by Delegates.notNull()
    }

    override fun onEnable() {
        INSTANCE = this
        saveDefaultConfig()
        messageManager = MessageManager()
        server.pluginManager.registerEvents(Listeners, this)
        server.messenger.registerOutgoingPluginChannel(this, "BungeeCord");
    }

    override fun onDisable() {
        server.scheduler.cancelTasks(this)
        messageManager.connection.close()
    }

    fun getAllVips() : HashSet<UUID> {
        return hashSetOf<UUID>().apply {
           servers.values.forEach {
               this.addAll(it.vips)
           }
        }
    }



}