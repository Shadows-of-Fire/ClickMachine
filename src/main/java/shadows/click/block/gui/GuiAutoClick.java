package shadows.click.block.gui;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.widget.Widget;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.GuiUtils;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;

public class GuiAutoClick extends ContainerScreen<ContainerAutoClick> {

	public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ClickMachine.MODID, "textures/gui/auto_click.png");
	PlayerEntity player = Minecraft.getInstance().player;

	public GuiAutoClick(ContainerAutoClick container, PlayerInventory inv, ITextComponent name) {
		super(container, inv, name);
		this.xSize = 176;
		this.ySize = 196;
	}

	@Override
	public void init() {
		super.init();
		int x = this.width / 2 - this.xSize / 2 + 30;
		int y = this.height / 2 - this.ySize / 2 + 26;
		this.addButton(new SpeedSlider(this, x, y, 100, 20));
		this.addButton(new ClickerCheckboxButton(this, x, y + 22, 20, 20, new TranslationTextComponent("gui.clickmachine.sneaking"), 2));
		this.addButton(new ClickerCheckboxButton(this, x, y + 44, 20, 20, new TranslationTextComponent("gui.clickmachine.right_click"), 3));
	}

	@Override
	public void tick() {
		super.tick();
		for (Widget b : this.buttons) {
			if (b instanceof ITickable) ((ITickable) b).tick();
		}
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.getListener() != null && this.isDragging() && button == 0 ? this.getListener().mouseDragged(mouseX, mouseY, button, dragX, dragY) : super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
	}

	@Override
	public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
		this.renderBackground(stack);
		super.render(stack, mouseX, mouseY, partialTicks);
		this.renderHoveredTooltip(stack, mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(MatrixStack stack, int mouseX, int mouseY) {
		Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
		this.font.drawString(stack, this.getNarrationMessage(), 8, 6, 4210752);
		this.font.drawString(stack, player.inventory.getDisplayName().getString(), 8, this.ySize - 96 + 2, 4210752);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void drawGuiContainerBackgroundLayer(MatrixStack stack, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(stack, i, j, 0, 0, this.xSize, this.ySize);
		if (ClickMachineConfig.usesRF) {
			int x = i + 150;
			int y = j + 26;
			this.blit(stack, x, y, xSize + 21, 0, 21, 64);
			int maxP = ClickMachineConfig.maxPowerStorage;
			double p = container.data.get(0);
			double ratio = p / maxP;
			this.blit(stack, x, y, xSize, 0, 21, 64 - (int) (ratio * 64));
		}
	}

	@Override
	protected void renderHoveredTooltip(MatrixStack stack, int x, int y) {
		super.renderHoveredTooltip(stack, x, y);
		if (ClickMachineConfig.usesRF && isPointInRegion(150, 26, 21, 64, x, y)) {
			List<ITextComponent> comps = new ArrayList<>(2);
			comps.add(new TranslationTextComponent("gui.clickmachine.power", container.data.get(0), ClickMachineConfig.maxPowerStorage));
			comps.add(new TranslationTextComponent("gui.clickmachine.power.usage", ClickMachineConfig.powerPerSpeed[container.data.get(1)]));
			GuiUtils.drawHoveringText(stack, comps, x, y, width, height, 0xFFFFFF, font);
		}
	}

}
