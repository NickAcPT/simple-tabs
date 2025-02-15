package at.cath.simpletabs

import at.cath.simpletabs.mixins.MixinHudAccessor
import at.cath.simpletabs.tabs.TabMenu
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStarted
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.exists


object TabsMod : ClientModInitializer {

    const val MOD_ID = "simpletabs"
    val logger: Logger = LogManager.getLogger(MOD_ID)

    private lateinit var configFile: File
    private val TABS_PATH = "${FabricLoader.getInstance().configDir}/${MOD_ID}"

    override fun onInitializeClient() {
        // rudimentary tab save setup; should be structured better if use is expanded
        val configPath = Path(TABS_PATH)
        configFile = File("$configPath/tabs.json")
        if (!configPath.exists()) {
            configFile.parentFile.mkdirs()
            configFile.createNewFile()
        }

        // replace vanilla ChatHud with custom one via mixin accessor
        ClientLifecycleEvents.CLIENT_STARTED.register(ClientStarted { client: MinecraftClient ->
            (client.inGameHud as MixinHudAccessor).setChatHud(
                TabMenu(client, configFile.readText())
            )
        })

        // write on client close
        ClientLifecycleEvents.CLIENT_STOPPING.register(ClientStopping {
            (it.inGameHud.chatHud as? TabMenu)?.let { tabMenu ->
                val tabsEncoded = Json.encodeToString(tabMenu.pageTabs)
                configFile.bufferedWriter().use { out ->
                    out.write(tabsEncoded)
                }
            }
        })

        logger.info("$MOD_ID loaded! Note that this mod applies changes purely client-side.")
    }
}