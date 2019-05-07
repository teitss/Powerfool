package br.com.pixelmonbrasil.powerfool.node

import org.spongepowered.api.Sponge
import org.spongepowered.api.service.sql.SqlService
import org.spongepowered.api.util.TypeTokens
import javax.sql.DataSource

object VipRepository {

    fun getVipsQuery() : String {
        return "SELECT uuid FROM luckperms_user_permissions" +
                " WHERE permission" +
                " IN (${getGlobalPermissions()})" +
                " UNION" +
                " SELECT uuid FROM luckperms_user_permissions" +
                " WHERE permission" +
                " IN (${getServerSpecificPermissions()})" +
                " AND server=?"
    }

    private val dataSource: DataSource = Sponge.getServiceManager().provide(SqlService::class.java).get().run {
        this.getDataSource("jdbc:mysql:"+
                "//${Powerfool.INSTANCE.config.getNode("database", "host").string!!}" +
                "/${Powerfool.INSTANCE.config.getNode("database", "db").string!!}" +
                "?user=${Powerfool.INSTANCE.config.getNode("database", "username").string!!}" +
                "&password=${Powerfool.INSTANCE.config.getNode("database", "password").string!!}")
    }

    fun getVipsForServer(): HashSet<String> {

       dataSource.connection.use { conn ->
            conn.prepareStatement(getVipsQuery()).use { ps ->
                ps.setString(1, Powerfool.INSTANCE.config.getNode("luckPermsServerName").string!!)
                ps.executeQuery().use {
                        return hashSetOf<String>().apply {
                        while (it.next()) {
                            this.add(it.getString(1))
                        }
                    }
                }
            }
        }
    }

    private fun getGlobalPermissions() : String {
        val sb = StringBuilder()
        Powerfool.INSTANCE.config.getNode("permissions", "global").getList(TypeTokens.STRING_TOKEN).forEach {
            sb.append("'$it',")
        }
        return sb.deleteCharAt(sb.length - 1).toString()
    }

    private fun getServerSpecificPermissions() : String {
        val sb = StringBuilder()
        Powerfool.INSTANCE.config.getNode("permissions", "server-specific").getList(TypeTokens.STRING_TOKEN).forEach {
            sb.append("'$it',")
        }
        return sb.deleteCharAt(sb.length - 1).toString()
    }


}