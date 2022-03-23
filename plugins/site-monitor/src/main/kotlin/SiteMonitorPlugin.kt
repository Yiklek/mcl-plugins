package com.github.yiklek.mcl.plugin.site_monitor

import app.cash.barber.Barber
import app.cash.barber.BarbershopBuilder
import app.cash.barber.locale.Locale.Companion.EN_US
import app.cash.barber.models.Document
import app.cash.barber.models.DocumentData
import app.cash.barber.models.DocumentTemplate
import com.github.difflib.DiffUtils
import com.github.difflib.UnifiedDiffUtils
import com.github.yiklek.mcl.plugin.site_monitor.SiteMonitorPlugin.reload
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.mamoe.mirai.Bot
import net.mamoe.mirai.console.data.AutoSavePluginData
import net.mamoe.mirai.console.data.ReadOnlyPluginConfig
import net.mamoe.mirai.console.data.value
import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.utils.debug
import net.mamoe.mirai.utils.info
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.lang3.StringUtils
import org.jsoup.Jsoup
import kotlin.time.Duration.Companion.seconds


object Config : ReadOnlyPluginConfig("SiteMonitor") {
    var rules: List<Rule>? by value()
    var botIds: Set<Long> by value()
}

object DataCache : AutoSavePluginData("SiteMonitor") {
    var md5cache: MutableMap<String, RuleCache> by value()
}

@Serializable
class RuleCache(var md5: String?, var text: String?)

fun reloadConfig() {
    Config.reload()
    SiteMonitorPlugin.logger.info { "config rules: %s".format(Json.encodeToString(Config.rules)) }
    SiteMonitorPlugin.logger.info { "config botIds: %s".format(Json.encodeToString(Config.botIds)) }
}

fun reloadDataCache() {
    DataCache.reload()
    SiteMonitorPlugin.logger.info { "data cache md5cache: %s".format(DataCache.md5cache) }
}

@Serializable
class Rule(
    val name: String = "",
    val url: String = "",
    val selector: String = "",
    val interval: Long = 60,
    val fromBots: Set<Long> = setOf(),
    val toGroups: Set<Long>? = null,
    val toFriends: Set<Long>? = null,
    val notifyTemplate: String
)

data class TemplateFields(
    val ruleName: String,
    val ruleUrl: String,
    val ruleSector: String,
    val fromBot: Long,
    val toFriend: Long?,
    val toGroup: Long?,
    val to: Long,
    val text: String,
    val oldText: String?,
    val newText: String,
    val oldHash: String?,
    val newHash: String,
    val diff: String?
) : DocumentData

data class NotifyDocument(
    val notify: String,
) : Document


fun getNotifyTemplate(notify: String): Barber<NotifyDocument> {
    val notifyTemplate = DocumentTemplate(
        fields = mapOf(
            "notify" to notify
        ), source = TemplateFields::class, targets = setOf(NotifyDocument::class), locale = EN_US
    )
    val barbershop =
        BarbershopBuilder().installDocumentTemplate<TemplateFields>(notifyTemplate).installDocument<NotifyDocument>()
            .build()
    return barbershop.getBarber(TemplateFields::class, NotifyDocument::class)
}

object SiteMonitorPlugin : KotlinPlugin(JvmPluginDescription(
    id = "com.github.yiklek.mcl.plugin.site-monitor", name = "site-monitor", version = "0.1.0"
) {
    author("Yiklek")
    info(
        """
            站点监控
        """.trimIndent()
    )
}) {
    private suspend fun loopRule(client: HttpClient, rule: Rule) {
        val ruleConfigHash = DigestUtils.md5Hex(Json.encodeToString(rule))
        while (true) {
            try {
                val content: String = client.request<HttpResponse>(rule.url).receive()
                val selected = Jsoup.parse(content).selectFirst(rule.selector)
                val md5Hex = DigestUtils.md5Hex(selected?.toString())
                var itemCache = DataCache.md5cache[ruleConfigHash]
                if (itemCache == null) {
                    itemCache = RuleCache(null, null)
                    DataCache.md5cache[ruleConfigHash] = itemCache
                }
                val contentHash = itemCache.md5
                logger.debug { "checking content hash ${rule.name} ==>> old:${contentHash} new:${md5Hex}" }
                if (selected == null) {
                    logger.warning("can't found item via selector: ${rule.selector}")
                    continue
                }
                if (!StringUtils.isAllEmpty(contentHash) && md5Hex != contentHash) {
                    // notify
                    rule.fromBots.forEach { botId ->
                        val bot = Bot.getInstance(botId)
                        // group
                        val render = getNotifyTemplate(rule.notifyTemplate)
                        rule.toGroups?.forEach { groupId ->
                            val group = bot.getGroup(groupId)
                            val notify = render.render(
                                TemplateFields(
                                    ruleName = rule.name,
                                    ruleUrl = rule.url,
                                    ruleSector = rule.selector,
                                    fromBot = botId,
                                    toFriend = null,
                                    toGroup = groupId,
                                    to = groupId,
                                    text = selected.text(),
                                    oldText = itemCache.text,
                                    newText = selected.text(),
                                    oldHash = itemCache.md5,
                                    newHash = md5Hex,
                                    diff = getDiff(itemCache.text, selected.text())
                                ), EN_US
                            ).notify
                            group?.sendMessage(notify)
                        }
                        rule.toFriends?.forEach { friendId ->
                            val friend = bot.getFriend(friendId)
                            val notify = render.render(
                                TemplateFields(
                                    ruleName = rule.name,
                                    ruleUrl = rule.url,
                                    ruleSector = rule.selector,
                                    fromBot = botId,
                                    toFriend = friendId,
                                    toGroup = null,
                                    to = friendId,
                                    text = selected.text(),
                                    oldText = itemCache.text,
                                    newText = selected.text(),
                                    oldHash = itemCache.md5,
                                    newHash = md5Hex,
                                    diff = getDiff(itemCache.text, selected.text())
                                ), EN_US
                            ).notify
                            friend?.sendMessage(notify)
                        }
                    }
                    break
                }
                itemCache.md5 = md5Hex
                itemCache.text = selected.text()
                DataCache.md5cache[ruleConfigHash] = itemCache
                logger.debug { Json.encodeToString(DataCache.md5cache) }
                DataCache.save()
            } catch (e: Exception) {
                SiteMonitorPlugin.logger.error(e)
            } finally {
                delay(rule.interval.seconds)
            }
        }
    }

    override fun onEnable() {
        logger.info { "plugin enable" }
        reloadConfig()
        reloadDataCache()

        val client = HttpClient()
        Config.rules?.let { it ->
            it.forEach { rule ->
                launch {
                    loopRule(client, rule)
                }
            }
        }
    }
}

fun getDiff(old: String?, new: String?): String {
    if (old == null || new == null) {
        return ""
    }
    val oldList = old.split("\n")
    val newList = new.split("\n")
    val diff = DiffUtils.diff(oldList, newList)
    val unifiedDiff = UnifiedDiffUtils.generateUnifiedDiff("old", "new", oldList, diff, 0)
    return unifiedDiff.joinToString("\n")
}
