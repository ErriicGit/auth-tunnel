package de.erriic.authtunnel;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.PlayerFaceWidget;
import net.minecraft.client.gui.components.SelectableEntry;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class AccountSelectionList extends ObjectSelectionList<AccountSelectionList.AccountEntry> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final ArrayList<Account> allAccounts = new ArrayList<>();
    private String filter = "";

    public AccountSelectionList(final Minecraft minecraft, final int width, final int height, final int y, final int itemHeight) {
        super(minecraft, width, height, y, itemHeight);

        this.loadAccounts();
        this.refresh();
    }



    @Override
    public int getRowWidth() {
        return 254;
    }

    public void loadAccounts() {
        allAccounts.clear();
        allAccounts.addAll(AuthTunnel.getLocalAccounts());
        AuthTunnelRest.fetchAccountsAsync((list) -> {
            allAccounts.addAll(list);
            this.refresh();
        });
    }


    public void refresh() {
        clearEntries();
        for (Account account : allAccounts) {
            if (filter.isEmpty() || account.filter(filter)) {
                AccountEntry entry = new AccountEntry(account, AuthTunnel.getActiveAccount().equals(account), this::activateAccount);
                addEntry(entry);
            }
        }
    }

    public void activateFirst() {
        if (!children().isEmpty()) {
            children().getFirst().activate();
        }
    }

    private void activateAccount(Account account) {
        if (account != null) {
            AuthTunnel.setActiveAccount(account);
            refresh();
        }
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }


    @Environment(EnvType.CLIENT)
    public static class AccountEntry extends ObjectSelectionList.Entry<AccountSelectionList.AccountEntry> implements SelectableEntry {

        private final Minecraft minecraft = Minecraft.getInstance();
        private final ResolvableProfile resolvableProfile;
        private final Account account;
        private final boolean active;
        private final Consumer<Account> onActivate;

        protected AccountEntry(Account account, boolean active, Consumer<Account> onActivate) {
            this.account = account;
            this.active = active;
            this.resolvableProfile = ResolvableProfile.createUnresolved(account.uuid());
            this.onActivate = onActivate;
        }

        @Override
        public void extractContent(final @NonNull GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final boolean hovered, final float a) {
            PlayerFaceWidget playerFaceWidget = new PlayerFaceWidget(this.getContentHeight() - 2, this.resolvableProfile);
            playerFaceWidget.setPosition(this.getContentX() + 1, this.getContentY() + 1);
            graphics.fill(this.getContentX(), this.getContentY(), this.getContentX() + this.getContentWidth(), this.getContentY() + this.getContentHeight(), this.active ? new Color(0, 255, 0, 150).getRGB() : new Color(0, 0,0,0).getRGB());
            playerFaceWidget.extractRenderState(graphics, mouseX, mouseY, a);
            graphics.text(this.minecraft.font, this.account.name(), this.getContentX() + this.getContentHeight() + 2, this.getContentY() + 1, new Color(255, 255, 255, 255).getRGB());
            graphics.text(this.minecraft.font, this.account.uuid().toString(), this.getContentX() + this.getContentHeight() + 2, this.getContentY() + 12, new Color(122, 122, 122, 255).getRGB());
            graphics.text(this.minecraft.font, this.account.type() == Account.Type.LOCAL ? "Local" : "Remote", this.getContentX() + this.getContentHeight() + 2, this.getContentY() + 23, this.account.type() == Account.Type.LOCAL ? new Color(0, 255, 0, 255).getRGB() : new Color(255, 0, 0, 255).getRGB());
        }

        @Override
        public boolean mouseClicked(final @NonNull MouseButtonEvent event, final boolean doubleClick) {
            if(doubleClick) {
                this.activate();
            }
            return super.mouseClicked(event, doubleClick);
        }


        @Override
        public boolean keyPressed(final @NonNull KeyEvent event) {
            if (event.isSelection()) {
                this.activate();
                return true;
            } else {
                return super.keyPressed(event);
            }
        }

        private void activate() {
            if (onActivate != null) {
                onActivate.accept(this.account);
            }
            LOGGER.info("Selected account: {} ({})", this.account.name(), this.account.uuid());
        }

        @Override
        public @NonNull Component getNarration() {
            return Component.empty();
        }
    }
}
