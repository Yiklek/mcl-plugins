import com.github.yiklek.mcl.plugin.cloud.CloudRecruitPlugin
import com.github.yiklek.mcl.plugin.trigger.ReplyTriggerPlugin
import net.mamoe.mirai.console.MiraiConsole
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.enable
import net.mamoe.mirai.console.plugin.PluginManager.INSTANCE.load
import net.mamoe.mirai.console.plugins.chat.command.PluginMain
import net.mamoe.mirai.console.terminal.MiraiConsoleTerminalLoader
import net.mamoe.mirai.console.util.ConsoleExperimentalApi

@ConsoleExperimentalApi
suspend fun main() {
    MiraiConsoleTerminalLoader.startAsDaemon()
    PluginMain.load()
    PluginMain.enable()
    ReplyTriggerPlugin.load()
    ReplyTriggerPlugin.enable()
    CloudRecruitPlugin.load()
    CloudRecruitPlugin.enable()
    MiraiConsole.job.join()
}