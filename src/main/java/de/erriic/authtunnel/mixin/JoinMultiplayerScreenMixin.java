package de.erriic.authtunnel.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import de.erriic.authtunnel.AccountSelectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(JoinMultiplayerScreen.class)
public abstract class JoinMultiplayerScreenMixin {

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/LinearLayout;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 8, shift = At.Shift.AFTER))
    private void addButton(CallbackInfo ci, @Local(name = "bottomFooterButtons") LinearLayout bottomFooterButtons) {
        bottomFooterButtons.addChild(
                Button.builder(
                        Component.translatable("accounts.title"),
                        _ -> {
                            Minecraft minecraft = Minecraft.getInstance();
                            minecraft.setScreenAndShow(new AccountSelectionScreen((JoinMultiplayerScreen) (Object) this));
                        }
                ).width(57).build()
        );
    }

    @ModifyArg(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/Button$Builder;width(I)Lnet/minecraft/client/gui/components/Button$Builder;"), index = 0)
    private int modifyWidths(int width) {
        return width == 74 ? 59 : width;
    }
}




