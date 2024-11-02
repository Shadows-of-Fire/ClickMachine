package dev.shadowsoffire.clickmachine.gui;

import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ClickerCheckboxButton extends AbstractButton {

    private static final ResourceLocation CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected_highlighted");
    private static final ResourceLocation CHECKBOX_SELECTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_selected");
    private static final ResourceLocation CHECKBOX_HIGHLIGHTED_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox_highlighted");
    private static final ResourceLocation CHECKBOX_SPRITE = ResourceLocation.withDefaultNamespace("widget/checkbox");

    protected final ClickMachineScreen gui;
    protected final int index;
    protected final BooleanSupplier selected;

    public ClickerCheckboxButton(ClickMachineScreen gui, int x, int y, int width, int height, Component title, int index, BooleanSupplier selected) {
        super(x, y, width, height, title);
        this.gui = gui;
        this.index = index;
        this.selected = selected;
    }

    @Override
    public void onPress() {
        Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.gui.getMenu().containerId, this.index - 1);
    }

    @Override
    public boolean isFocused() {
        return super.isHovered() || super.isFocused();
    }

    public boolean isSelected() {
        return selected.getAsBoolean();
    }

    @Override
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font font = minecraft.font;
        gfx.setColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        ResourceLocation sprite;
        if (this.isSelected()) {
            sprite = this.isFocused() ? CHECKBOX_SELECTED_HIGHLIGHTED_SPRITE : CHECKBOX_SELECTED_SPRITE;
        }
        else {
            sprite = this.isFocused() ? CHECKBOX_HIGHLIGHTED_SPRITE : CHECKBOX_SPRITE;
        }
        gfx.blitSprite(sprite, this.getX(), this.getY(), this.width, this.height);
        gfx.drawString(font, this.getMessage().getString(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 4210752, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {}

}
