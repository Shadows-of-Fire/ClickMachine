package shadows.click.block.gui;

import java.util.function.BooleanSupplier;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ClickerCheckboxButton extends Checkbox {

    private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");

    protected final AutoClickScreen gui;
    protected final int index;
    protected final BooleanSupplier selected;

    public ClickerCheckboxButton(AutoClickScreen gui, int x, int y, int width, int height, Component title, int index, BooleanSupplier selected) {
        super(x, y, width, height, title, false);
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

    @Override
    public boolean selected() {
        return selected.getAsBoolean();
    }

    @Override
    public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.enableDepthTest();
        Font font = minecraft.font;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        gfx.blit(TEXTURE, this.getX(), this.getY(), this.isFocused() ? 20.0F : 0.0F, this.selected() ? 20.0F : 0.0F, 20, this.height, 64, 64);
        // this.renderBg(gfx, minecraft, mouseX, mouseY);
        gfx.drawString(font, this.getMessage().getString(), this.getX() + 24, this.getY() + (this.height - 8) / 2, 4210752, false);
    }

}
