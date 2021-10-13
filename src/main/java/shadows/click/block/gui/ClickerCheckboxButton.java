package shadows.click.block.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import shadows.placebo.Placebo;
import shadows.placebo.net.MessageButtonClick;

public class ClickerCheckboxButton extends CheckboxButton {

	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/checkbox.png");

	protected final GuiAutoClick gui;
	protected final int index;

	public ClickerCheckboxButton(GuiAutoClick gui, int x, int y, int width, int height, ITextComponent title, int index) {
		super(x, y, width, height, title, false);
		this.gui = gui;
		this.index = index;
	}

	@Override
	public void onPress() {
		Placebo.CHANNEL.sendToServer(new MessageButtonClick(this.index));
	}

	@Override
	public boolean isFocused() {
		return isHovered();
	}

	@Override
	public boolean selected() {
		return gui.getMenu().data.get(index) != 0;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void renderButton(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.getTextureManager().bind(TEXTURE);
		RenderSystem.enableDepthTest();
		FontRenderer fontrenderer = minecraft.font;
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, this.alpha);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		blit(matrixStack, this.x, this.y, this.isFocused() ? 20.0F : 0.0F, this.selected() ? 20.0F : 0.0F, 20, this.height, 64, 64);
		this.renderBg(matrixStack, minecraft, mouseX, mouseY);
		fontrenderer.draw(matrixStack, this.getMessage().getString(), this.x + 24, this.y + (this.height - 8) / 2, 4210752);
	}

}
