package at.cath.simpletabs.mixins;


import at.cath.simpletabs.tabs.TabMenu;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ClientPlayerEntity.class)
public class MixinPrefixChat {

    @Shadow
    @Final
    protected MinecraftClient client;

    @ModifyArg(method = "sendChatMessage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;sendChatMessageInternal(Ljava/lang/String;Lnet/minecraft/text/Text;)V", ordinal = 0))
    private String prefixChatMessage(String message, Text preview) {
        var prefix = "";
        if (client.inGameHud.getChatHud() instanceof TabMenu tabMenu) {
            var selectedTab = tabMenu.getSelectedTab();
            if (selectedTab != null) {
                prefix = selectedTab.getPrefix();
            }
        }
        return prefix + message;
    }
}