package de.erriic.authtunnel.mixin;

import de.erriic.authtunnel.AuthTunnel;
import net.minecraft.client.multiplayer.AccountProfileKeyPairManager;
import net.minecraft.world.entity.player.ProfileKeyPair;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Mixin(AccountProfileKeyPairManager.class)
public class AccountProfileKeyPairManagerMixin {

    @Inject(
            method = "readOrFetchProfileKeyPair",
            at = @At("HEAD"),
            cancellable = true
    )
    private void tunnelauth$forceRefresh(
            Optional<ProfileKeyPair> cachedKeyPair,
            CallbackInfoReturnable<CompletableFuture<Optional<ProfileKeyPair>>> cir
    ) {

        if (!AuthTunnel.forceKeyRefresh) return;

        AuthTunnel.forceKeyRefresh = false;

        CompletableFuture<Optional<ProfileKeyPair>> forced = CompletableFuture.completedFuture(Optional.empty());

        cir.setReturnValue(forced);
    }
}