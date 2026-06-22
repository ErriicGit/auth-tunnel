package de.erriic.authtunnel;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

@Environment(EnvType.CLIENT)
public class AccountSelectionScreen extends Screen {
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 33, 30);
    private final Screen lastScreen;
    private static final Identifier SEARCH_SPRITE = Identifier.withDefaultNamespace("icon/search");
    private static final Component SEARCH_HINT = Component.translatable("accounts.input.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
    private static final Component SERVER_HINT = Component.translatable("accounts.input.server").withStyle(EditBox.DEFAULT_HINT_STYLE);
    private static final Component API_KEY_HINT = Component.translatable("accounts.input.api_key").withStyle(EditBox.DEFAULT_HINT_STYLE);
    protected AccountSelectionList accountSelectionList;
    protected EditBox inputServer;
    protected EditBox inputApiKey;
    protected EditBox searchBar;

    public AccountSelectionScreen(final Screen lastScreen) {
        super(Component.translatable("accounts.title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(this.title, this.font);
        this.accountSelectionList = this.layout.addToContents(new AccountSelectionList(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), 36));
        LinearLayout searchBarLayout = this.layout.addToContents(LinearLayout.horizontal().spacing(4), LayoutSettings::alignVerticallyTop);
        searchBarLayout.addChild(ImageWidget.sprite(20, 20, SEARCH_SPRITE));
        this.searchBar = searchBarLayout.addChild(
                new EditBox(this.font, 0, 0, 200, 20, SEARCH_HINT) {

                    @Override
                    public boolean keyPressed(@NonNull KeyEvent event) {
                        if (event.isSelection()) {
                            accountSelectionList.activateFirst();
                            this.setCursorPosition(0);
                            this.setHighlightPos(this.getValue().length());
                        }
                        return super.keyPressed(event);
                    }
                }
        );
        LinearLayout footer = this.layout.addToFooter(LinearLayout.horizontal().spacing(4));
        this.inputServer = footer.addChild(new EditBox(this.font, 0, 0, 125, 20, SERVER_HINT));
        this.inputApiKey = footer.addChild(new EditBox(this.font, 0, 0, 125, 20, API_KEY_HINT));
        this.inputServer.setHint(SERVER_HINT);
        this.inputApiKey.setHint(API_KEY_HINT);
        this.inputServer.setValue(AuthTunnel.getServerAddress());
        this.inputApiKey.setValue(AuthTunnel.getApiKey());
        this.searchBar.setHint(SEARCH_HINT);
        this.inputServer.setMaxLength(60);
        this.inputApiKey.setMaxLength(40);
        this.inputServer.setResponder(this::updateServerAddress);
        this.inputApiKey.setResponder(this::updateApiKey);
        this.searchBar.setResponder(text -> {
            if (this.accountSelectionList != null) {
                this.accountSelectionList.setFilter(text);
                this.accountSelectionList.refresh();
            }
        });
        this.layout.visitWidgets(this::addRenderableWidget);
        this.setFocused(this.searchBar);
        this.repositionElements();
    }

    private void updateServerAddress(String serverAddress) {
        AuthTunnel.setServerAddress(serverAddress);
        this.refreshAccountList();
    }

    private void updateApiKey(String apiKey) {
        AuthTunnel.setApiKey(apiKey);
        this.refreshAccountList();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();

        int searchBarHeight = 26;
        this.accountSelectionList.updateSizeAndPosition(this.width, this.layout.getContentHeight() - searchBarHeight, 0, this.layout.getHeaderHeight() + searchBarHeight);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.lastScreen);
    }


    private void refreshAccountList() {
        if (this.accountSelectionList != null) {
            this.searchBar.setValue("");
            this.accountSelectionList.loadAccounts();
            this.accountSelectionList.refresh();
        }
    }

    @Override
    public boolean keyPressed(final @NonNull KeyEvent event) {
        if (super.keyPressed(event)) {
            return true;
        } else if (event.key() == 294) {
            this.refreshAccountList();
            return true;
        } else {
            return false;
        }
    }
}
