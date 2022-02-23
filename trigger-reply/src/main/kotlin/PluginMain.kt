package io.github.yiklek.mcl.plugin

import kotlinx.serialization.Serializable
import net.mamoe.mirai.console.data.*
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.utils.info
import java.util.stream.Stream

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
object Config : ReadOnlyPluginConfig("TriggerReply") {
    var list: List<Item>? by value()
}

@Serializable
class Item(val triggers: List<String>, val reply: String)

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
        // author 和 info 可以删除.
    }
) {
    override fun onEnable() {
        logger.info { "trigger-reply Plugin enable" }
        Config.reload()
        val replyMap = mutableMapOf<String, String>()
        Config.list?.forEach { item ->
            item.triggers.forEach {
                replyMap[it] = item.reply
            }
        }

        logger.info { Config.list.toString() }
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<GroupMessageEvent> {

        }
    }
}
