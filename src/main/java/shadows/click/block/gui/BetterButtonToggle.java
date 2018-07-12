package shadows.click.block.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButtonToggle;
import net.minecraft.client.renderer.GlStateManager;

public class BetterButtonToggle extends GuiButtonToggle {

	public BetterButtonToggle(int buttonId, int xIn, int yIn, int widthIn, int heightIn, boolean buttonText) {
		super(buttonId, xIn, yIn, widthIn, heightIn, buttonText);
	}

	public void drawButton(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			mc.getTextureManager().bindTexture(this.resourceLocation);
			GlStateManager.disableDepth();
			int i = this.xTexStart;
			int j = this.yTexStart;

			if (this.hovered || this.stateTriggered) {
				i += this.xDiffTex;
				j += this.yDiffTex;
			}

			this.drawTexturedModalRect(this.x, this.y, i, j, this.width, this.height);
			GlStateManager.enableDepth();
		}
	}

}
