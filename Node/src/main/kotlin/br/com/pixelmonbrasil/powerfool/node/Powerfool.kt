package br.com.pixelmonbrasil.powerfool.node

import com.google.inject.Inject
import me.lucko.luckperms.LuckPerms
import me.lucko.luckperms.api.event.node.NodeAddEvent
import me.lucko.luckperms.api.event.node.NodeRemoveEvent
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import org.slf4j.Logger
import org.spongepowered.api.Sponge
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.game.GameReloadEvent
import org.spongepowered.api.event.game.state.GameStartedServerEvent
import org.spongepowered.api.event.game.state.GameStartingServerEvent
import org.spongepowered.api.event.game.state.GameStoppingServerEvent
import org.spongepowered.api.plugin.Dependency
import org.spongepowered.api.plugin.Plugin
import org.spongepowered.api.plugin.PluginContainer
import org.spongepowered.api.util.TypeTokens
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit


@Plugin(id="powerfoolnode",
    name="Powerfool-Node",
    version="1.2.0",
    authors=["Teits"],
    dependencies = [Dependency(id="spotlin"), Dependency(id="luckperms")])
class Powerfool @Inject constructor(
    val logger: Logger,
    val pluginContainer: PluginContainer,
    @DefaultConfig(sharedRoot = true) val path: Path,
    @DefaultConfig(sharedRoot = true) val configLoader: ConfigurationLoader<CommentedConfigurationNode>
) {

    companion object {
        lateinit var INSTANCE: Powerfool
    }

    lateinit var config: CommentedConfigurationNode
    lateinit var messageManager: MessageManager
    lateinit var vips: HashSet<String>

    @Listener
    fun onStarting(e: GameStartingServerEvent) {
        INSTANCE = this
        logger.info("Loading configuration...")
        loadConfig()
        vips = VipRepository.getVipsForServer()
    }

    @Listener
    fun onStarted(e: GameStartedServerEvent) {
        messageManager = MessageManager()
        logger.info("Registering server...")
        messageManager.sendRegister()
        logger.info("Starting tasks...")
        Sponge.getScheduler().createTaskBuilder()
            .delay(5, TimeUnit.SECONDS)
            .interval(config.getNode("onlinePlayersUpdate").long, TimeUnit.SECONDS)
            .execute { _ ->
                messageManager.sendUpdate("onlinePlayers" to Sponge.getServer().onlinePlayers.size.toString())
            }
            .submit(this)
        logger.info("Registering permission listeners...")
        LuckPerms.getApi().eventBus.subscribe(NodeAddEvent::class.java, this::onNodeAdd)
        LuckPerms.getApi().eventBus.subscribe(NodeRemoveEvent::class.java, this::onNodeRemove)
    }

    @Listener
    fun onReload(e: GameReloadEvent) {
        loadConfig()
        vips = VipRepository.getVipsForServer()
        messageManager.sendRegister()
    }

    @Listener
    fun onStopping(e: GameStoppingServerEvent) {
        Sponge.getScheduler().getScheduledTasks(this).forEach {
            it.cancel()
        }
        messageManager.connection.close()
    }

    private fun loadConfig() {
        if (Files.notExists(path)) {
            val configFile = pluginContainer.getAsset("powerfoolnode.conf").get()
            configFile.copyToDirectory(path.parent)
            logger.info("Configuration installed with success.")
            logger.info("Now customize your settings before going online again!")
            Sponge.getServer().shutdown()
        }
        config = configLoader.load()
    }

    @Listener
    fun onNodeAdd(e: NodeAddEvent) {
        if (!e.isUser)
            return
        if (!config.getNode("permissions", "server-specific").getList(TypeTokens.STRING_TOKEN)
                .contains(e.node.permission))
            return
        val optServer = e.node.server
        if (!optServer.isPresent) {
            if (config.getNode("permissions", "global").getList(TypeTokens.STRING_TOKEN)
                    .contains(e.node.permission)) {
                vips.add(e.target.objectName)
                messageManager.sendUpdate("vips" to vips.toString())
            }
            return
        }
        if (optServer.get() != config.getNode("luckPermsServerName").string!!)
            return
        vips.add(e.target.objectName)
        messageManager.sendUpdate("vips" to vips.toString())
    }

    @Listener
    fun onNodeRemove(e: NodeRemoveEvent) {
        if (!e.isUser)
            return
        if (!config.getNode("permissions", "server-specific").getList(TypeTokens.STRING_TOKEN)
                .contains(e.node.permission))
            return
        val optServer = e.node.server
        if (!optServer.isPresent) {
            if (config.getNode("permissions", "global").getList(TypeTokens.STRING_TOKEN).contains(e.node.permission)) {
                vips.remove(e.target.objectName)
                messageManager.sendUpdate("vips" to vips.toString())
            }
            return
        }

        if (optServer.get() != config.getNode("luckPermsServerName").string!!)
            return

        vips.remove(e.target.objectName)
        messageManager.sendUpdate("vips" to vips.toString())
    }

}