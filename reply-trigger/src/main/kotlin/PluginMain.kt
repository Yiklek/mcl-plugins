package io.github.yiklek.mcl.plugin

import io.github.yiklek.mcl.plugin.PluginMain.reload
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.console.command.CommandManager
import net.mamoe.mirai.console.command.CommandSender
import net.mamoe.mirai.console.command.SimpleCommand
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.Contact
import net.mamoe.mirai.contact.Friend
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
}

object Command : SimpleCommand(
    PluginMain, "reload",
    description = "reload config"
) {
    @Suppress("unused")
    @Handler // 标记这是指令处理器  // 函数名随意
    fun CommandSender.handle() { // 这两个参数会被作为指令参数要求
        reloadConfig()
    }
}

fun reloadConfig() {
    Config.reload()
    PluginMain.logger.info { "reply-trigger config rules: %s".format(Json.encodeToString(Config.rules)) }
    PluginMain.logger.info { "reply-trigger config botIds: %s".format(Json.encodeToString(Config.botIds)) }
}

@Serializable
class Rule(val groups: Set<Long>? = null, val friends: Set<Long>? = null, val triggers: Set<String>, val reply: String)

object PluginMain : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.yiklek.mcl.plugin",
        name = "trigger-reply",
        version = "0.1.0"
    ) {
        author("Yiklek")
        info(
            """
            关键字触发回复
        """.trimIndent()
        )
    }
) {
    private fun checkContact(
        list: Collection<Long>?,
        code: String,
        gId: Long,
        botId: Long,
        isFriend: Boolean
    ): Boolean {
        return list != null && (list.isEmpty() || list.contains(gId))
            && (isFriend ||
            code.contains(At(botId).serializeToMiraiCode()))
    }


    private suspend fun loopRule(
        encodedMessage: String,
        botId: Long,
        contact: Contact,
        supplier: (Rule) -> Collection<Long>?
    ) {
        LOOP_RULES@
        for (rule in Config.rules!!) {
            for (it in rule.triggers) {
                if (checkContact(
                        supplier.invoke(rule),
                        encodedMessage,
                        contact.id, botId, contact is Friend
                    ) && encodedMessage.contains(it)
                ) {
                    contact.sendMessage(rule.reply)
                    break@LOOP_RULES
                }
            }
        }
    }

    override fun onEnable() {
        logger.info { "reply-trigger plugin enable" }
        reloadConfig()
        CommandManager.registerCommand(Command)
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {
            if (Config.botIds.contains(bot.id)) {
                loopRule(message.serializeToMiraiCode(), bot.id, group) { it.groups }
            }
        }
        eventChannel.subscribeAlways<FriendMessageEvent> {
            if (Config.botIds.contains(bot.id)) {
                loopRule(message.serializeToMiraiCode(), bot.id, friend) { it.friends }
            }
        }
    }

}
