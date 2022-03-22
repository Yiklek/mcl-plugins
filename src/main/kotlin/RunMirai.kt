import com.github.yiklek.mcl.plugin.cloud.CloudRecruitPlugin
import com.github.yiklek.mcl.plugin.site_monitor.SiteMonitorPlugin
import com.github.yiklek.mcl.plugin.trigger.ReplyTriggerPlugin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.plugin.jvm.AbstractJvmPlugin
import net.mamoe.mirai.console.plugins.chat.command.PluginMain
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi
import xyz.cssxsh.mirai.plugin.MiraiDevicePlugin

@ConsoleExperimentalApi
suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()
    val pluginList = listOf<AbstractJvmPlugin>(
        PluginMain,
        ReplyTriggerPlugin,
        CloudRecruitPlugin,
        MiraiDevicePlugin,
        SiteMonitorPlugin
    )
    pluginList.forEach {
        it.load()
        it.enable()
    }
    MiraiConsole.job.join()
}