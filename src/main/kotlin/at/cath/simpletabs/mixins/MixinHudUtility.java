package at.cath.simpletabs.mixins;

import net.minecraft.client.gui.hud.*;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatHud.class)
public interface MixinHudUtility {
    @Invoker("addMessage")
    void addMessageWithoutLog(Text message, @Nullable MessageSignatureData signature, int ticks, @Nullable MessageIndicator indicator, boolean refresh);

    @Accessor("visibleMessages")
    List<ChatHudLine.Visible> getVisibleMessages();

    @Accessor("messages")
    List<ChatHudLine> getLocalMessageHistory();

    @Accessor("scrolledLines")
    int getScrolledLines();

}
