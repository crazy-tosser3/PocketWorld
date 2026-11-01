package dev.crzto3.pocketworld.client;

import dev.crzto3.pocketworld.dimension.PocketActions;
import dev.crzto3.pocketworld.dimension.PocketDimensionManager;
import dev.crzto3.pocketworld.menu.DimensionConfiguratorMenu;
import dev.crzto3.pocketworld.network.ConfiguratorActionPacket;
import dev.crzto3.pocketworld.network.PocketNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Окно конфигуратора с вкладками: остров, биом, небо и погода, животные, гости.
 * Все варианты показаны сразу, выбранный подсвечен зелёным.
 */
public class DimensionConfiguratorScreen
        extends AbstractContainerScreen<DimensionConfiguratorMenu> {

    private static final int TAB_ISLAND = 0;
    private static final int TAB_BIOME = 1;
    private static final int TAB_SKY = 2;
    private static final int TAB_ANIMALS = 3;
    private static final int TAB_GUESTS = 4;

    private static final String[] TAB_KEYS = {"island", "biome", "sky", "animals", "guests"};
    private static final int GUEST_ROWS = 4;
    private static final int REFRESH_INTERVAL = 20;

    /** Кнопки каждой вкладки; видна только выбранная. */
    private final List<List<Button>> tabWidgets = new ArrayList<>();
    private final List<Button> tabButtons = new ArrayList<>();

    /** Кнопки с выбором варианта: группа данных, индекс данных, количество. */
    private record Option(Button button, int dataIndex, int value, Component label) {
    }

    private final List<Option> options = new ArrayList<>();

    private final Button[] invitedButtons = new Button[GUEST_ROWS];
    private final Button[] onlineButtons = new Button[GUEST_ROWS];
    private final String[] invitedNames = new String[GUEST_ROWS];
    private final String[] onlineNames = new String[GUEST_ROWS];

    private Button regenerateButton;

    private int tab = TAB_ISLAND;

    public DimensionConfiguratorScreen(
            DimensionConfiguratorMenu menu,
            Inventory inventory,
            Component title
    ) {
        super(menu, inventory, title);

        this.imageWidth = 230;
        this.imageHeight = 238;
        this.inventoryLabelX = 34;
        this.inventoryLabelY = 148;
    }

    @Override
    protected void init() {
        super.init();

        tabWidgets.clear();
        tabButtons.clear();
        options.clear();

        for (int i = 0; i < TAB_KEYS.length; i++) {
            final int index = i;
            tabWidgets.add(new ArrayList<>());
            tabButtons.add(addRenderableWidget(Button.builder(
                            Component.translatable("gui.pocketworld.tab." + TAB_KEYS[i]), button -> selectTab(index))
                    .bounds(leftPos + 30 + i * 39, topPos + 20, 37, 18).build()));
        }

        buildIslandTab();
        buildBiomeTab();
        buildSkyTab();
        buildAnimalsTab();
        buildGuestsTab();

        selectTab(tab);
    }

    private <T extends Button> T inTab(int tabIndex, T button) {
        addRenderableWidget(button);
        tabWidgets.get(tabIndex).add(button);
        return button;
    }

    private void send(int action, int value) {
        PocketNetwork.CHANNEL.sendToServer(new ConfiguratorActionPacket(action, value));
    }

    /** Кнопка-вариант: нажатие отправляет действие, выбранный вариант подсвечивается. */
    private void addOption(int tabIndex, int x, int y, int width, int dataIndex, int value, int action, Component label) {
        Button button = inTab(tabIndex, Button.builder(label, pressed -> send(action, value))
                .bounds(leftPos + x, topPos + y, width, 18).build());

        options.add(new Option(button, dataIndex, value, label));
    }

    private void buildIslandTab() {
        List<String> terrains = PocketDimensionManager.AVAILABLE_TERRAINS;
        List<String> sizes = PocketDimensionManager.AVAILABLE_SIZES;

        for (int i = 0; i < terrains.size(); i++) {
            addOption(TAB_ISLAND, 8 + i * 54, 56, 52, 0, i, ConfiguratorActionPacket.TERRAIN,
                    Component.translatable("gui.pocketworld.terrain." + terrains.get(i)));
        }

        for (int i = 0; i < sizes.size(); i++) {
            addOption(TAB_ISLAND, 8 + i * 54, 86, 52, 3, i, ConfiguratorActionPacket.SIZE,
                    Component.translatable("gui.pocketworld.size." + sizes.get(i)));
        }

        regenerateButton = inTab(TAB_ISLAND, Button.builder(Component.empty(), button ->
                        send(ConfiguratorActionPacket.REGENERATE, 0))
                .bounds(leftPos + 8, topPos + 112, 214, 18).build());
    }

    private void buildBiomeTab() {
        List<String> biomes = PocketDimensionManager.PRESET_BIOMES;

        for (int i = 0; i < biomes.size(); i++) {
            addOption(TAB_BIOME, 8 + (i % 4) * 54, 48 + (i / 4) * 20, 52, 1, i, ConfiguratorActionPacket.BIOME,
                    Component.translatable("biome." + biomes.get(i).replace(':', '.')));
        }
    }

    private void buildSkyTab() {
        List<String> skies = PocketDimensionManager.AVAILABLE_SKIES;
        List<String> weather = PocketDimensionManager.AVAILABLE_WEATHER;

        for (int i = 0; i < skies.size(); i++) {
            addOption(TAB_SKY, 8 + (i % 3) * 72, 56 + (i / 3) * 20, 70, 2, i, ConfiguratorActionPacket.SKY,
                    Component.translatable("gui.pocketworld.sky." + skies.get(i)));
        }

        for (int i = 0; i < weather.size(); i++) {
            addOption(TAB_SKY, 8 + i * 72, 108, 70, 5, i, ConfiguratorActionPacket.WEATHER,
                    Component.translatable("gui.pocketworld.weather." + weather.get(i)));
        }
    }

    private void buildAnimalsTab() {
        for (int i = 0; i < PocketActions.ANIMALS.size(); i++) {
            final int index = i;
            inTab(TAB_ANIMALS, Button.builder(PocketActions.animalName(i),
                            button -> send(ConfiguratorActionPacket.BUY_ANIMAL, index))
                    .bounds(leftPos + 8 + (i % 4) * 54, topPos + 56 + (i / 4) * 20, 52, 18).build());
        }
    }

    private void buildGuestsTab() {
        for (int row = 0; row < GUEST_ROWS; row++) {
            final int index = row;

            invitedButtons[row] = inTab(TAB_GUESTS, Button.builder(Component.empty(), button -> {
                        if (invitedNames[index] != null) {
                            PocketNetwork.CHANNEL.sendToServer(new ConfiguratorActionPacket(
                                    ConfiguratorActionPacket.UNINVITE, 0, invitedNames[index]));
                        }
                    })
                    .bounds(leftPos + 8, topPos + 58 + row * 20, 104, 18).build());

            onlineButtons[row] = inTab(TAB_GUESTS, Button.builder(Component.empty(), button -> {
                        if (onlineNames[index] != null) {
                            PocketNetwork.CHANNEL.sendToServer(new ConfiguratorActionPacket(
                                    ConfiguratorActionPacket.INVITE, 0, onlineNames[index]));
                        }
                    })
                    .bounds(leftPos + 118, topPos + 58 + row * 20, 104, 18).build());
        }
    }

    private void selectTab(int index) {
        tab = index;

        for (int i = 0; i < tabWidgets.size(); i++) {
            for (Button button : tabWidgets.get(i)) {
                button.visible = i == index;
            }

            tabButtons.get(i).active = i != index;
        }

        ClientPocketState.clearMessage();
        send(ConfiguratorActionPacket.REFRESH, 0);
    }

    @Override
    protected void containerTick() {
        super.containerTick();

        if (menu.getData(4) == DimensionConfiguratorMenu.STATUS_OK && tab == TAB_GUESTS
                && minecraft != null && minecraft.player != null
                && minecraft.player.tickCount % REFRESH_INTERVAL == 0) {
            send(ConfiguratorActionPacket.REFRESH, 0);
        }

        refresh();
    }

    private void refresh() {
        boolean ready = menu.getData(4) == DimensionConfiguratorMenu.STATUS_OK;

        for (Option option : options) {
            boolean selected = menu.getData(option.dataIndex()) == option.value();

            option.button().setMessage(selected
                    ? option.label().copy().withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    : option.label());
            option.button().active = ready;
        }

        regenerateButton.setMessage(Component.translatable("gui.pocketworld.regenerate"));
        regenerateButton.active = ready;

        for (Button button : tabWidgets.get(TAB_ANIMALS)) {
            button.active = ready;
        }

        refreshGuests(ready);
    }

    private void refreshGuests(boolean ready) {
        List<String> invited = ClientPocketState.getInvited();
        List<String> online = new ArrayList<>();

        if (minecraft != null && minecraft.getConnection() != null) {
            for (PlayerInfo info : minecraft.getConnection().getOnlinePlayers()) {
                String name = info.getProfile().getName();

                if (minecraft.player != null
                        && !name.equals(minecraft.player.getGameProfile().getName())
                        && invited.stream().noneMatch(name::equalsIgnoreCase)) {
                    online.add(name);
                }
            }
        }

        for (int row = 0; row < GUEST_ROWS; row++) {
            invitedNames[row] = row < invited.size() ? invited.get(row) : null;
            onlineNames[row] = row < online.size() ? online.get(row) : null;

            invitedButtons[row].setMessage(invitedNames[row] == null
                    ? Component.empty()
                    : Component.literal("x " + invitedNames[row]).withStyle(ChatFormatting.RED));
            invitedButtons[row].active = ready && invitedNames[row] != null;

            onlineButtons[row].setMessage(onlineNames[row] == null
                    ? Component.empty()
                    : Component.literal("+ " + onlineNames[row]).withStyle(ChatFormatting.GREEN));
            onlineButtons[row].active = ready && onlineNames[row] != null;
        }
    }

    @Override
    protected void renderBg(
            GuiGraphics graphics,
            float partialTick,
            int mouseX,
            int mouseY
    ) {
        int left = this.leftPos;
        int top = this.topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, 0xFF252535);
        graphics.fill(left, top, left + imageWidth, top + 14, 0xFF171725);
        graphics.fill(left + 7, top + 19, left + 25, top + 37, 0xFF555566);
        graphics.fill(left + 33, top + 157, left + 197, top + 235, 0xFF353545);
    }

    private void caption(GuiGraphics graphics, String key, int x, int y) {
        graphics.drawString(this.font, Component.translatable(key), x, y, 0xAAAAAA, false);
    }

    @Override
    protected void renderLabels(
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        graphics.drawString(this.font, "Pocket World", 8, 4, 0xFFFFFF, false);

        int status = menu.getData(4);

        if (status != DimensionConfiguratorMenu.STATUS_OK) {
            graphics.drawString(this.font, Component.translatable(
                    status == DimensionConfiguratorMenu.STATUS_NONE
                            ? "gui.pocketworld.status_none"
                            : "gui.pocketworld.status_foreign"), 8, 48, 0xFF7777, false);
        } else {
            switch (tab) {
                case TAB_ISLAND -> {
                    caption(graphics, "gui.pocketworld.caption.terrain", 8, 46);
                    caption(graphics, "gui.pocketworld.caption.size", 8, 76);
                    caption(graphics, "gui.pocketworld.hint", 8, 134);
                }
                case TAB_BIOME -> caption(graphics, "gui.pocketworld.hint", 8, 134);
                case TAB_SKY -> {
                    caption(graphics, "gui.pocketworld.caption.sky", 8, 46);
                    caption(graphics, "gui.pocketworld.caption.weather", 8, 98);
                }
                case TAB_ANIMALS -> {
                    graphics.drawString(this.font, Component.translatable("gui.pocketworld.animals_count",
                            ClientPocketState.getAnimals(), PocketActions.MAX_ANIMALS,
                            PocketActions.ANIMAL_COST), 8, 46, 0xAAAAAA, false);
                    caption(graphics, "gui.pocketworld.animals_hint", 8, 100);
                }
                case TAB_GUESTS -> {
                    caption(graphics, "gui.pocketworld.caption.invited", 8, 46);
                    caption(graphics, "gui.pocketworld.caption.online", 118, 46);
                }
                default -> {
                }
            }

            String message = ClientPocketState.getMessage();

            if (!message.isEmpty()) {
                graphics.drawString(this.font, Component.translatable(message), 8, 138, 0xFFDD66, false);
            }
        }

        graphics.drawString(this.font, this.playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFFFFF, false);
    }

    @Override
    public void render(
            GuiGraphics graphics,
            int mouseX,
            int mouseY,
            float partialTick
    ) {
        this.renderBackground(graphics);

        super.render(graphics, mouseX, mouseY, partialTick);

        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
