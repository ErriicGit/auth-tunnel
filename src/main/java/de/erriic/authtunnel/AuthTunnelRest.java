package de.erriic.authtunnel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class AuthTunnelRest {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Logger LOGGER = LogUtils.getLogger();

    public static void fetchAccountsAsync(Consumer<List<Account>> callback) {
        EXECUTOR.submit(() -> {
            try {
                List<Account> accounts = fetchAccounts(); // blocking HTTP call

                Minecraft.getInstance().execute(() -> {
                    callback.accept(accounts);
                });

            } catch (Exception e) {
                LOGGER.error("Error fetching accounts", e);
            }
        });
    }

    private static List<Account> fetchAccounts() throws IOException {
        URL url = new URL("https://" + AuthTunnel.getServerAddress() + "/api/get_accounts");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + AuthTunnel.getApiKey());
        conn.setRequestProperty("Accept", "application/json");

        try (InputStream in = conn.getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            JsonArray array = JsonParser.parseString(json).getAsJsonArray();

            List<Account> result = new ArrayList<>();

            for (JsonElement el : array) {
                JsonObject obj = el.getAsJsonObject();

                result.add(new Account(
                        fromDashlessUUID(obj.get("uuid").getAsString()),
                        obj.get("name").getAsString(),
                        Account.Type.REMOTE
                ));
            }

            return result;
        }
    }

    public static Map.Entry<String, String> findRouteEntry(URL original) {
        if(original.toString().equalsIgnoreCase("https://api.minecraftservices.com/player/certificates")){
            return Map.entry("https://api.minecraftservices.com/player/certificates", "https://" + AuthTunnel.getServerAddress() + "/api/certificates?uuid=" + toDashlessUUID(Minecraft.getInstance().getUser().getProfileId()));
        }
        return null;
    }

    public static void authenticate(final UUID profileId, final String serverId) throws AuthenticationException {
        // Directly use ws:// server URL (will perform GET upgrade to websocket)
        String serverUrl = "wss://" + AuthTunnel.getServerAddress() + "/api/authenticate?uuid=" + toDashlessUUID(profileId) + "&server_id=" + serverId;
        // Blocking tunnel on the current thread - if anything fails, we throw AuthenticationException
        try {
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

            // Connect TCP to the Minecraft auth server (defaulting to 443)
            Socket socket = new Socket();
            int authPort = 443;
            socket.connect(new InetSocketAddress(AuthTunnel.minecraftAuthServer, authPort), 10_000);
            InputStream tcpIn = socket.getInputStream();
            OutputStream tcpOut = socket.getOutputStream();

            final WebSocket[] wsHolder = new WebSocket[1];

            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "auth-tunnel-timeouter"));
            AtomicLong lastActivity = new AtomicLong(System.currentTimeMillis());
            // idle timeout: 60s
            Runnable idleCheck = () -> {
                if (System.currentTimeMillis() - lastActivity.get() > 60_000) {
                    LOGGER.info("[AuthTunnel] Idle timeout, closing connections");
                    try {
                        if (wsHolder[0] != null) wsHolder[0].sendClose(WebSocket.NORMAL_CLOSURE, "timeout");
                    } catch (Exception ignored) {
                    }
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                }
            };
            scheduler.scheduleAtFixedRate(idleCheck, 30, 30, TimeUnit.SECONDS);

            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    LOGGER.info("[AuthTunnel] WebSocket opened to {}", serverUrl);
                    webSocket.request(1);
                    wsHolder[0] = webSocket;
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
                    try {
                        byte[] bytes = new byte[data.remaining()];
                        data.get(bytes);
                        tcpOut.write(bytes);
                        tcpOut.flush();
                        lastActivity.set(System.currentTimeMillis());
                    } catch (IOException e) {
                        LOGGER.error("[AuthTunnel] Failed to write to TCP: {}", e.getMessage());
                        try {
                            socket.close();
                        } catch (Exception ignored) {
                        }
                    }
                    webSocket.request(1);
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    LOGGER.info("[AuthTunnel] WebSocket closed: {} {}", statusCode, reason);
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                    scheduler.shutdownNow();
                    return CompletableFuture.completedFuture(null);
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    LOGGER.error("[AuthTunnel] WebSocket error: {}", error.getMessage());
                    try {
                        socket.close();
                    } catch (Exception ignored) {
                    }
                    scheduler.shutdownNow();
                }
            };

            CompletableFuture<WebSocket> wsFuture = httpClient.newWebSocketBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .header("Authorization", "Bearer " + AuthTunnel.getApiKey())
                    .buildAsync(URI.create(serverUrl), listener);

            WebSocket webSocket = wsFuture.join();
            wsHolder[0] = webSocket;

            // Pump TCP -> WebSocket (blocks current thread)
            byte[] buf = new byte[8192];
            int r;
            while ((r = tcpIn.read(buf)) != -1) {
                lastActivity.set(System.currentTimeMillis());
                ByteBuffer bb = ByteBuffer.wrap(Arrays.copyOf(buf, r));
                try {
                    webSocket.sendBinary(bb, true).join();
                } catch (Exception e) {
                    LOGGER.error("[AuthTunnel] Failed to send binary to WebSocket: {}", e.getMessage());
                    break;
                }
            }

            // Clean up
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "tcp closed").join();
            } catch (Exception ignored) {
            }
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            scheduler.shutdownNow();

        } catch (Exception e) {
            throw new AuthenticationException("[AuthTunnel] failed: " + e.getMessage());
        }
    }

    public static UUID fromDashlessUUID(String raw) {
        return UUID.fromString(
                raw.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                )
        );
    }

    public static String toDashlessUUID(UUID uuid) {
        return uuid.toString().replace("-", "");
    }
}
