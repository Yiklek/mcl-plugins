package com.github.yiklek.mcl.plugin.cloud

import com.github.yiklek.mcl.plugin.cloud.CloudRecruitPlugin.reload
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.buildMessageChain
import net.mamoe.mirai.utils.info


object CloudRecruitPlugin : KotlinPlugin(JvmPluginDescription(
    id = "com.github.yiklek.mcl.plugin.cloud-recruit", name = "cloud-recruit", version = "0.1.0"
) {
    author("Yiklek")
    info(
        """
            招新工具
        """.trimIndent()
    )
}) {
    override fun onEnable() {
        logger.info { "cloud-recruit plugin enable" }
        reloadConfig()
        val eventChannel = GlobalEventChannel.parentScope(this)
        eventChannel.subscribeAlways<MemberJoinEvent> {
            if (group.id == Config.groupId) {
                group.sendMessage(buildMessageChain {
                    +"欢迎 "
                    +At(user.id)
                    +Config.content
                })
            }
        }

    }

}

fun reloadConfig() {
    Config.reload()
    CloudRecruitPlugin.logger.info { "cloud-recruit config group id: %s".format(Config.groupId) }
}

object Config : ReadOnlyPluginConfig("CloudRecruit") {
    var groupId: Long by value()
    var content: String by value()
}