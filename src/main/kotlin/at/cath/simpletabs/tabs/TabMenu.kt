package at.cath.simpletabs.tabs

import at.cath.simpletabs.TabsMod
import at.cath.simpletabs.mixins.MixinHudUtility
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.hud.ChatHud
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.text.TranslatableTextContent
import net.minecraft.util.Formatting
import kotlin.math.min


class TabMenu(var client: MinecraftClient, serialized: String? = null) : ChatHud(client) {

    var activeGroup: Int = 0
    private var activePage: Int = 0
    private var selectedTab: String = ""

    val showPerPage = 4
    var pageTabs = mutableListOf(linkedMapOf<String, ChatTab>())

    private val visibleMessages = (this as MixinHudUtility).visibleMessages
    private val localMessageHistory = (this as MixinHudUtility).localMessageHistory

    init {
        if (serialized != null) {
            try {
                this.pageTabs = Json.decodeFromString(serialized)
            } catch (ex: Exception) {
                TabsMod.logger.error("Encountered invalid tabs config format")
            }
        }

        if (pageTabs.all { it.isEmpty() }) {
            val defaultTab = ChatTab("General")
            pageTabs[0] = linkedMapOf(defaultTab.uuid to defaultTab)
            navigateTo(defaultTab.uuid)
        } else if (selectedTab.isEmpty()) {
            navigateTo(pageTabs.first { it.isNotEmpty() }.keys.first())
        }
    }

    override fun addMessage(message: Text) {
        pageTabs.forEach { tabMap ->
            tabMap.values.forEach {
                var repeatCount = 0
                if (it.acceptsMessage(message.string)) {
                    val incoming = message.copy()

                    if (it.messages.isNotEmpty()) {
                        val (extractedMsg, repeats) = extractRepeatMsg(it.messages.last())
                        if (message.string == extractedMsg.string) {
                            repeatCount = repeats
                            repeatCount += 1
                        }
                    }

                    if (repeatCount > 0)
                        incoming.appendRepeatMsg(repeatCount + 1)

                    if (it.uuid == selectedTab) {
                        if (repeatCount > 0) {
                            visibleMessages.removeFirst()
                            localMessageHistory.removeFirst()
                        }
                        super.addMessage(incoming)

                    } else if (!it.muted && pageTabs[activeGroup].containsKey(it.uuid)) {
                        it.unreadCount++
                    }

                    if (repeatCount > 0) it.messages.removeLast()
                    it.messages += incoming

                }
            }
        }
    }

    fun addTab(chatTab: ChatTab) {
        pageTabs[activeGroup][chatTab.uuid] = chatTab
    }

    fun removeTab(tabId: String): Boolean {
        with(pageTabs[activeGroup]) {
            if (containsKey(tabId)) {
                remove(tabId)

                if (selectedTab == tabId) {
                    selectedTab = ""
                    clear(false)
                }
                return true
            }
            return false
        }
    }

    fun nextPage(): Boolean {
        if (pageTabs[activeGroup].size > showPerPage * (activePage + 1)) {
            activePage++
            return true
        }
        return false
    }

    fun previousPage(): Boolean {
        if (activePage - 1 >= 0) {
            activePage--
            return true
        }
        return false
    }

    fun getActiveTabs(): List<ChatTab> {
        val tabsInGroup = pageTabs[activeGroup].values.toList()
        val from = activePage * showPerPage
        val to = from + showPerPage

        return tabsInGroup.slice(from until min(tabsInGroup.size, to))
    }

    fun cycleForward(): Boolean {
        if (activeGroup + 1 < pageTabs.size) {
            activeGroup++
            selectedTab = ""
            clear(false)
            return true
        } else if (pageTabs[activeGroup].size > 0) {
            pageTabs.add(++activeGroup, linkedMapOf())

            clear(false)
            return true
        }
        return false
    }

    fun cycleBackward(): Boolean {
        if (activeGroup - 1 >= 0) {
            activeGroup--
            selectedTab = ""
            clear(false)
            return true
        }
        return false
    }

    fun navigateTo(tabId: String) {
        if (pageTabs[activeGroup].containsKey(tabId) && selectedTab != tabId) {
            val tab = pageTabs[activeGroup][tabId]!!
            clear(false)

            // mixin invoker to avoid printing a new message to logs when switching tabs
            tab.messages.forEach {
                (this as MixinHudUtility).addMessageWithoutLog(
                    it,
                    null,
                    client.inGameHud.ticks,
                    null,
                    false
                )
            }

            tab.unreadCount = 0
            selectedTab = tab.uuid
        }
    }

    fun getSelectedTab(): ChatTab? = pageTabs[activeGroup][selectedTab]

    private fun extractRepeatMsg(msg: Text): Pair<Text, Int> {
        with(msg.siblings) {
            if (size > 0) {
                val lastComponent = find { it is TranslatableTextContent && it.key == "chat.simpletabs.repeat" }
                if (lastComponent != null) {
                    val repeatCount = lastComponent.string.filter(Char::isDigit).toInt() - 1
                    val extractedMsg = msg.copy()
                    extractedMsg.siblings.removeIf { it == lastComponent }
                    return Pair(extractedMsg, repeatCount)
                }
            }
        }
        return Pair(msg, 0)
    }

    private fun Text.appendRepeatMsg(repeatCount: Int): Text {
        this.siblings.add(
            Text.translatable(
                "chat.simpletabs.repeat",
                repeatCount
            ).setStyle(Style.EMPTY.withColor(Formatting.GRAY))
        )
        return this
    }

}