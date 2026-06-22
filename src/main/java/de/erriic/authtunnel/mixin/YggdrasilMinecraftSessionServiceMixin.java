package de.erriic.authtunnel.mixin;

import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import de.erriic.authtunnel.AuthTunnel;
import de.erriic.authtunnel.AuthTunnelRest;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(YggdrasilMinecraftSessionService.class)
public class YggdrasilMinecraftSessionServiceMixin {

	@Inject(at = @At("HEAD"), method = "joinServer", cancellable = true)
	private void init(final UUID profileId, final String authenticationToken, final String serverId, CallbackInfo ci) throws AuthenticationException {
		if(AuthTunnel.getLocalAccounts().stream().noneMatch(account -> account.uuid().equals(profileId))) {
			ci.cancel();
			AuthTunnelRest.authenticate(profileId, serverId);
		}
	}
}