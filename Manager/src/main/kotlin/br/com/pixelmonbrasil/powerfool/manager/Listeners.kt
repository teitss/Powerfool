package br.com.pixelmonbrasil.powerfool.manager

import com.google.common.io.ByteStreams
import fr.xephi.authme.api.v3.AuthMeApi
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack

object Listeners : Listener {

    const val INVENTORY_ID = "§0Selecione um servidor"
    val callbackMap: HashMap<Int, String> = hashMapOf()
    val selector = ItemStack(Material.COMPASS, 1).apply {
        val meta = this.itemMeta
        meta.displayName = "§aSelecionar Servidor"
        meta.lore = listOf("§fClique para selecionar", "§fum servidor.")
        this.setItemMeta(meta)
    }

    fun serverSelector(server: Server): ItemStack {
        return getItemStackFromString(server.itemString).apply {
            val meta = this.itemMeta
            meta.displayName = server.serverName
            val serverStatus = if ((System.currentTimeMillis() - server.lastUpdate) > 20000)
                "§cOFFLINE"
            else
                "§aJogadores online: ${server.onlinePlayers}/${server.maxPlayers}"
            meta.lore = listOf(*server.description, "", serverStatus, "")
            if (server.isHighlightServer) {
                meta.addEnchant(Enchantment.FIRE_ASPECT, 1, false)
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
                meta.lore = listOf("§f* §d§nServidor em destaque§r§f *", "", *meta.lore.toTypedArray())
            }
            this.setItemMeta(meta)
        }
    }

    fun createInventory(): Inventory {
        return Bukkit.createInventory(null, 54 , INVENTORY_ID).apply {
            Powerfool.INSTANCE.servers.values.forEach {
                if (this.getItem(it.desiredPosition) != null)
                    Bukkit.getLogger().warning("${it.bungeeId} tried to replace ${callbackMap[it.desiredPosition]}")
                else {
                    if (it.isVisible && (System.currentTimeMillis() - it.lastUpdate < 600000)) {
                        this.setItem(it.desiredPosition, serverSelector(it))
                        insertCallback(it.desiredPosition, it.bungeeId)
                    }
                }
            }
        }
    }

    @EventHandler
    fun onSelectorInteract(e: PlayerInteractEvent) {

        if (e.action != Action.RIGHT_CLICK_AIR)
            return

        if (!e.hasItem())
            return

        if (e.hand != EquipmentSlot.HAND)
            return

        if (!e.item.isSimilar(selector))
            return

        e.player.playSound(e.player.location, Sound.BLOCK_NOTE_HARP, 1F, 1F)
        e.player.openInventory(createInventory())

    }

    @EventHandler
    fun onServerSelect(e: InventoryClickEvent) {

        if (e.clickedInventory == null)
            return

        if (e.clickedInventory.name != INVENTORY_ID)
            return

        e.isCancelled = true

        if (!e.isLeftClick && !e.isRightClick)
            return

        if (e.currentItem == null)
            return


        callbackMap[e.rawSlot]?.let {
            e.whoClicked.closeInventory()
            connectPlayer(e.whoClicked, it)
        }

    }

    @EventHandler
    fun onPlayerConnect(e: PlayerJoinEvent) {
        Bukkit.getScheduler().runTaskLater(Powerfool.INSTANCE, {
            e.player.inventory.setItem(0, selector)
        }, 3)
    }

    @EventHandler
    fun onDropTry(e: PlayerDropItemEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onChat(e: AsyncPlayerChatEvent) {
        e.isCancelled = true
    }

    @EventHandler
    fun onCommand(e: PlayerCommandPreprocessEvent) {
        val command = e.message.toLowerCase().split(" ")

        if (command.size < 2)
            return

        if (command[0] == "/email" && command[1] == "add") {
            if (!AuthMeApi.getInstance().isAuthenticated(e.player))
                e.isCancelled = true
        }

    }

    fun connectPlayer(player: HumanEntity, serverId: String) {

        val server = Powerfool.INSTANCE.servers[serverId]!!

        if (server.vips.contains(player.uniqueId)) {
            (player as Player).sendTitle("§aVocê tem §ePasse VIP", "§aTenha um bom jogo!", 10, 50, 10)
            player.playSound(player.location, Sound.BLOCK_NOTE_PLING, 1F, 1F)
            sendPlayerToServer(player, server)
            return
        }

        if (server.onlyVips) {
            if (Powerfool.INSTANCE.getAllVips().contains(player.uniqueId)) {
                (player as Player).sendTitle("§aVocê tem §ePasse VIP", "§aTenha um bom jogo!", 10, 50, 10)
                player.playSound(player.location, Sound.BLOCK_NOTE_PLING, 1F, 1F)
                sendPlayerToServer(player, server)
            }
            return
        }

        if (server.onlinePlayers >= server.maxPlayers) {
            (player as Player).sendTitle(
                "§cO servidor está cheio.",
                "§eConheça nossos outros servidores!",
                10, 50, 10
            )
            player.playSound(player.location, Sound.BLOCK_NOTE_CHIME, 1F, 1F)
            return
        }
        (player as Player).playSound(player.location, Sound.BLOCK_NOTE_PLING, 1F, 1F)
        sendPlayerToServer(player, server)

    }

    private fun getItemStackFromString(string: String): ItemStack {
        val itemInfo = string.split(":")
        try {
            return ItemStack(Material.matchMaterial(itemInfo[0]), itemInfo[1].toInt(), itemInfo[2].toShort())
        } catch (e: Exception) {
            return ItemStack(Material.STONE)
        }
    }

    fun sendPlayerToServer(player: HumanEntity, server: Server) {
        val bado = ByteStreams.newDataOutput()
        bado.writeUTF("ConnectOther")
        bado.writeUTF(player.name)
        bado.writeUTF(server.bungeeId.toLowerCase())
        (player as Player).sendPluginMessage(Powerfool.INSTANCE, "BungeeCord", bado.toByteArray())
        server.onlinePlayers++
    }

    fun insertCallback(slot: Int, bungeeId: String) {
        if (callbackMap[slot] != bungeeId)
            callbackMap[slot] = bungeeId
    }

}