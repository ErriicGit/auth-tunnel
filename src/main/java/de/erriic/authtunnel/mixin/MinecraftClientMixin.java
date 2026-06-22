package de.erriic.authtunnel.mixin;

import com.mojang.authlib.minecraft.client.MinecraftClient;
import com.mojang.logging.LogUtils;
import de.erriic.authtunnel.AuthTunnel;
import de.erriic.authtunnel.AuthTunnelRest;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.Map;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Unique
    private static final Logger LOGGER = LogUtils.getLogger();

    @Shadow
    @Final
    private Proxy proxy;

    @Inject(method = "createUrlConnection", at = @At("HEAD"), cancellable = true)
    private void onCreateUrlConnection(URL url, CallbackInfoReturnable<HttpURLConnection> cir) {
        try {
            Map.Entry<String, String> entry = AuthTunnelRest.findRouteEntry(url);
            if (entry != null) {
                LOGGER.info("Routing URL {} to {}", url, entry.getValue());
                String original = url.toString();
                String replacementBase = entry.getValue();
                String suffix = original.substring(entry.getKey().length());
                String newUrlStr = replacementBase + suffix;
                URI uri = new URI(newUrlStr);
                URL newUrl = uri.toURL();

                final HttpURLConnection connection = (HttpURLConnection) newUrl.openConnection(this.proxy);
                connection.setConnectTimeout(MinecraftClient.CONNECT_TIMEOUT_MS);
                connection.setReadTimeout(MinecraftClient.READ_TIMEOUT_MS);
                connection.setUseCaches(false);
                connection.setRequestProperty("X-AuthTunnel-Routed", "true");
                connection.setRequestProperty("Authorization", "Bearer " + AuthTunnel.getApiKey());
                cir.setReturnValue(connection);
                cir.cancel();
            }
        } catch (Exception e) {
            LOGGER.error("Routing failed in createUrlConnection: {}", e.getMessage());
        }
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
        method = "prepareRequest",
        at = @At(
            value = "INVOKE",
            target = "java/net/HttpURLConnection.setRequestProperty(Ljava/lang/String;Ljava/lang/String;)V"
        )
    )
    private void redirectPostSetRequestProperty(HttpURLConnection connection, String key, String value) {
        if ("Authorization".equalsIgnoreCase(key) && connection.getRequestProperty("X-AuthTunnel-Routed") != null) {
            return;
        }
        connection.setRequestProperty(key, value);
    }
}
