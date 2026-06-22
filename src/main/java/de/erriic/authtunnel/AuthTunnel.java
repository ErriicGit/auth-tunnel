package de.erriic.authtunnel;

import de.erriic.authtunnel.config.ConfigManager;
import de.erriic.authtunnel.mixin.MinecraftMixin;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AuthTunnel implements ClientModInitializer {

	public static String minecraftAuthServer = "sessionserver.mojang.com";

	public static Account activeAccount;

	public static List<Account> localAccounts = new ArrayList<>();

	public static boolean forceKeyRefresh = false;

	public static Account getActiveAccount() {
		return activeAccount;
	}

	public static void setActiveAccount(Account account) {
		activeAccount = account;
		User user = new User(
				account.name(),
				account.uuid(),
				"",
				Optional.empty(),
				Optional.empty()
		);
		((MinecraftMixin) Minecraft.getInstance()).setUser(user);
		forceKeyRefresh = true;
	}

	public static String getApiKey() {
		return ConfigManager.get().apiKey;
	}

	public static void setApiKey(String key) {
		ConfigManager.get().apiKey = key;
		ConfigManager.save();
	}

	public static void setServerAddress(String addr) {
		ConfigManager.get().server = addr;
		ConfigManager.save();
	}

	public static String getServerAddress() {
		return ConfigManager.get().server;
	}


	public static List<Account> getLocalAccounts() {
		return localAccounts;
	}

	@Override
	public void onInitializeClient() {
		User user = Minecraft.getInstance().getUser();
		activeAccount = new Account(user.getProfileId(), user.getName(), Account.Type.LOCAL);
		localAccounts.add(activeAccount);
		ConfigManager.load();
	}
}