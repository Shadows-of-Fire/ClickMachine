package shadows.click.block.gui;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.widget.ToggleWidget;

public class BetterButtonToggle extends ToggleWidget {

	int id;
	GuiAutoClick controller;

	public BetterButtonToggle(int id, int xIn, int yIn, int widthIn, int heightIn, boolean buttonText) {
		super(xIn, yIn, widthIn, heightIn, buttonText);
		this.id = id;
	}

	@Override
	public void renderButton(int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			Minecraft.getInstance().getTextureManager().bindTexture(this.resourceLocation);
			GlStateManager.disableDepthTest();
			int i = this.xTexStart;
			int j = this.yTexStart;

			if (this.isHovered || this.stateTriggered) {
				i += this.xDiffTex;
				j += this.yDiffTex;
			}

			this.blit(this.x, this.y, i, j, this.width, this.height);
			GlStateManager.enableDepthTest();
		}
	}

	@Override
	public void onClick(double p_onClick_1_, double p_onClick_3_) {
		controller.actionPerformed(this);
	}

}
