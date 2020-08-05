package shadows.click.block.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

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
	public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
		if (this.visible) {
			this.hovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			Minecraft.getInstance().getTextureManager().bindTexture(this.resourceLocation);
			RenderSystem.disableDepthTest();
			int i = this.xTexStart;
			int j = this.yTexStart;

			if (this.hovered || this.stateTriggered) {
				i += this.xDiffTex;
				j += this.yDiffTex;
			}

			this.drawTexture(stack, this.x, this.y, i, j, this.width, this.height);
			RenderSystem.enableDepthTest();
		}
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		controller.actionPerformed(this);
	}

}
