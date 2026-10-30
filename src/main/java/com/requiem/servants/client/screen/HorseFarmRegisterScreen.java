package com.requiem.servants.client.screen;

import com.requiem.servants.block.HorseFarmRegisterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * 马场登记站屏幕：委任状槽位 + 繁殖开关按钮。
 */
public class HorseFarmRegisterScreen extends AbstractContainerScreen<HorseFarmRegisterMenu> {
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("requiem", "textures/gui/common_block_gui.png");

    private Button toggleButton;

    public HorseFarmRegisterScreen(HorseFarmRegisterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 222;
    }

    @Override
    protected void init() {
        super.init();
        this.toggleButton = Button.builder(Component.empty(), b -> {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                })
                .bounds(this.leftPos + 48, this.topPos + 45, 80, 20)
                .build();
        this.addRenderableWidget(this.toggleButton);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 96 + 2, 0x404040, false);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        this.renderBackground(graphics);
        graphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        this.updateButton();
    }

    private void updateButton() {
        boolean breeding = this.menu.isBreeding();
        this.toggleButton.setMessage(Component.translatable(
                breeding ? "gui.requiem.horse_farm_register.breeding_on"
                        : "gui.requiem.horse_farm_register.breeding_off"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
