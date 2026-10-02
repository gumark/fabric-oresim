package dev.gumark.oresim;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The {@code /oresim} overlay configuration screen: one on/off toggle per
 * ore type, plus water and lava.
 */
public class OreSimScreen extends Screen {
    private static final Component TITLE = Component.literal("OreSim by gumark");
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;

    public OreSimScreen() {
        super(TITLE);
    }

    @Override
    protected void init() {
        OreType[] types = OreType.values();
        int rows = (types.length + 1) / 2;
        int gridHeight = rows * (BUTTON_HEIGHT + GAP) - GAP;
        int startY = (this.height - gridHeight) / 2 + 8;
        int startX = (this.width - (2 * BUTTON_WIDTH + GAP)) / 2;

        for (int i = 0; i < types.length; i++) {
            OreType type = types[i];
            int col = i / rows;
            int row = i % rows;
            Button button = Button.builder(label(type), b -> {
                OverlayToggles.toggle(type);
                b.setMessage(label(type));
            }).bounds(startX + col * (BUTTON_WIDTH + GAP), startY + row * (BUTTON_HEIGHT + GAP), BUTTON_WIDTH, BUTTON_HEIGHT).build();
            this.addRenderableWidget(button);
        }

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
            .bounds((this.width - BUTTON_WIDTH) / 2, startY + gridHeight + 12, BUTTON_WIDTH, BUTTON_HEIGHT).build());
    }

    private static Component label(OreType type) {
        return Component.literal(type.label + ": " + (OverlayToggles.isEnabled(type) ? "ON" : "OFF"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, TITLE, this.width / 2, 15, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
