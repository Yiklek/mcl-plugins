package com.github.yiklek.mcl.plugin.trigger

import com.github.yiklek.mcl.plugin.trigger.ReplyTriggerPlugin.reload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.permission.AbstractPermitteeId
import net.mamoe.mirai.console.permission.Permission
import net.mamoe.mirai.console.permission.PermissionService
import net.mamoe.mirai.console.permission.PermissionService.Companion.hasPermission
import net.mamoe.mirai.console.permission.PermissionService.Companion.testPermission
import net.mamoe.mirai.console.permission.PermitteeId.Companion.permitteeId
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.User
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.FriendMessageEvent
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.utils.info

/**
 * 使用 kotlin 版请把
 * `src/main/resources/META-INF.services/net.mamoe.mirai.console.plugin.jvm.JvmPlugin`
 * 文件内容改成 `PluginMain` 也就是当前主类全类名
 *
 * 使用 kotlin 可以把 java 源集删除不会对项目有影响
 *
 * 在 `settings.gradle.kts` 里改构建的插件名称、依赖库和插件版本
 *
 * 在该示例下的 [JvmPluginDescription] 修改插件名称，id和版本，etc
 *
 * 可以使用 `src/test/kotlin/RunMirai.kt` 在 ide 里直接调试，
 * 不用复制到 mirai-console-loader 或其他启动器中调试
 */
object Config : ReadOnlyPluginConfig("ReplyTrigger") {
    var rules: List<Rule>? by value()
    var botIds: Set<Long> by value()
    var prefix: String? by value()
}

fun Contact.hasPermission(perm: Permission): Boolean {
    return this is Group && perm.testPermission(permitteeId)
        || this is User && perm.testPermission(permitteeId)
}

object ReplyCommand : SimpleCommand(
    ReplyTriggerPlugin, "reply", description = "admin reply conmand"
) {
    @Suppress("unused")
    @Handler
    suspend// 标记这是指令处理器  // 函数名随意
    fun CommandSender.handle(trigger: String) { // 这两个参数会被作为指令参数要求
        if (bot == null) {
            return
        }


        checkBotId(bot!!.id) {
            if (this.subject != null) {
                if (!AbstractPermitteeId.ExactUser(bot!!.id)
                        .hasPermission(ReplyTriggerPlugin.PERMISSION_CAN_REPLY)
                    || !subject!!.hasPermission(ReplyTriggerPlugin.PERMISSION_CAN_RECEIVE_REPLY)
                ) {
                    return@checkBotId
                }
                ReplyTriggerPlugin.loopRule(trigger, 0, subject!!, {
                    if (subject!! is Group) {
                        it.groups
                    } else {
                        it.friends
                    }
                }, { false }, false)
            }
        }
    }
}

object SendCommand : SimpleCommand(
    ReplyTriggerPlugin, "send", description = "admin send conmand"
) {
    @Suppress("unused")
    @Handler
    suspend// 标记这是指令处理器  // 函数名随意
    fun CommandSender.handle(source: Long, target: Long, trigger: String) { // 这两个参数会被作为指令参数要求
        Bot.getInstance(source).getGroup(target)?.let { group ->
            ReplyTriggerPlugin.loopRule(trigger, 0, group, { it.groups }, { false }, false)
        }
    }
}

fun reloadConfig() {
    Config.reload()
    ReplyTriggerPlugin.logger.info { "config rules: %s".format(Json.encodeToString(Config.rules)) }
    ReplyTriggerPlugin.logger.info { "config botIds: %s".format(Json.encodeToString(Config.botIds)) }
}

@Serializable
class Rule(
    val groups: Set<Long>? = null, val friends: Set<Long>? = null,
    val triggers: Set<String>, val reply: String, val requireAt: Boolean = true,
    val prefix: String? = null, val botIds: Set<Long>? = null
)


suspend fun checkBotId(id: Long, run: suspend () -> Unit) {
    if (Config.botIds.contains(id)) {
        run.invoke()
    }
}

object ReplyTriggerPlugin : KotlinPlugin(
    JvmPluginDescription(
        id = "com.github.yiklek.mcl.plugin.reply-trigger", name = "reply-trigger", version = "0.1.0"
    ) {
        author("Yiklek")
        info(
            """
            关键字触发回复
        """.trimIndent()
        )
    }) {
    private fun checkContact(
        list: Collection<Long>?, code: String, contactId: Long, botId: Long, requireAt: Boolean
    ): Boolean {
        return list != null && (list.isEmpty() || list.contains(contactId)) && (!requireAt || code.contains(At(botId).serializeToMiraiCode()))
    }


    suspend fun loopRule(
        encodedMessage: String,
        botId: Long,
        contact: Contact,
        supplier: (Rule) -> Collection<Long>?,
        requireAt: (Contact) -> Boolean,
        checkPrefix: Boolean
    ) {
        LOOP_RULES@ for (rule in Config.rules!!) {
            if (rule.botIds != null && !(rule.botIds.contains(botId) || botId == 0L)) {
                continue@LOOP_RULES
            }
            for (it in rule.triggers) {
                if (checkContact(
                        supplier.invoke(rule),
                        encodedMessage,
                        contact.id,
                        botId,
                        rule.requireAt && requireAt.invoke(contact)
                    ) && encodedMessage.contains(
                        if (checkPrefix) {
                            rule.prefix ?: Config.prefix ?: ""
                        } else {
                            ""
                        } + it
                    )
                ) {
                    contact.sendMessage(rule.reply)
                    break@LOOP_RULES
                }
            }
        }
    }

    val PERMISSION_CAN_REPLY by lazy {
        PermissionService.INSTANCE.register(permissionId("can-reply"), "可响应reply命令")
    }
    val PERMISSION_CAN_RECEIVE_REPLY by lazy {
        PermissionService.INSTANCE.register(permissionId("can-receive-reply"), "可响应reply命令")
    }

    override fun onEnable() {
        logger.info { "plugin enable" }
        reloadConfig()
        PERMISSION_CAN_REPLY
        CommandManager.registerCommand(ReplyCommand)
        CommandManager.registerCommand(SendCommand)
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
            checkBotId(bot.id) {
                loopRule(message.serializeToMiraiCode(), bot.id, group, { it.groups }, { it is Group }, true)
            }
        }
        eventChannel.subscribeAlways<FriendMessageEvent> {
            checkBotId(bot.id) {
                loopRule(message.serializeToMiraiCode(), bot.id, friend, { it.friends }, { it is Friend }, true)
            }
        }
    }

}
