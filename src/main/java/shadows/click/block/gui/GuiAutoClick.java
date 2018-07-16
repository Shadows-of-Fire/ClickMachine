package shadows.click.block.gui;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.block.TileAutoClick;
import shadows.click.net.MessageButtonClick;

public class GuiAutoClick extends GuiContainer {

	public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ClickMachine.MODID, "textures/gui/auto_click.png");
	EntityPlayer player = Minecraft.getMinecraft().player;
	TileAutoClick tile;
	BetterButtonToggle[] buttons = new BetterButtonToggle[12];

	public GuiAutoClick(TileAutoClick tile) {
		super(new ContainerAutoClick(tile, Minecraft.getMinecraft().player));
		this.tile = tile;
		++ySize;
	}

	@Override
	public void initGui() {
		super.initGui();
		for (Buttons b : Buttons.values())
			buttons[b.ordinal()] = this.addButton(b.getAndInitButton(this));
		buttons[tile.getSpeedIndex()].setStateTriggered(true);
		buttons[9].setStateTriggered(tile.isSneaking());
		buttons[tile.isRightClicking() ? 11 : 10].setStateTriggered(true);
	}

	public void updateTile(int speedIdx, boolean sneaking, boolean rightClick, int power) {

		for (BetterButtonToggle b : buttons) {
			if (b.id < 9) b.setStateTriggered(b.id == speedIdx);
		}
		buttons[9].setStateTriggered(sneaking);
		for (BetterButtonToggle b : buttons) {
			if (b.id > 9) b.setStateTriggered(rightClick ? b.id == 11 : b.id == 10);
		}

		tile.setPower(power);
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		super.drawScreen(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		mc.renderEngine.bindTexture(GUI_TEXTURE);
		int n = (int) (18 * 4 * Math.min(1, (1 - ((float) tile.getPower() / ClickMachineConfig.maxPowerStorage))));
		this.drawTexturedModalRect(151, 7 + n, 230, n, 18, 18 * 4);
		this.fontRenderer.drawString(I18n.format("gui.clickmachine.autoclick.name"), 8, 6, 4210752);
		this.fontRenderer.drawString(player.inventory.getDisplayName().getUnformattedText(), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		this.mc.getTextureManager().bindTexture(GUI_TEXTURE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.ySize);
	}

	@Override
	protected void renderHoveredToolTip(int x, int y) {
		super.renderHoveredToolTip(x, y);
		for (BetterButtonToggle b : buttons)
			if (b.isMouseOver()) this.drawHoveringText(Buttons.VALUES[b.id].getTooltip(), x, y, fontRenderer);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id < 12) {
			BetterButtonToggle toggle = (BetterButtonToggle) button;
			if (toggle.id < 9) for (BetterButtonToggle b : buttons) {
				if (b.id < 9 && b.id != toggle.id) b.setStateTriggered(false);
				toggle.setStateTriggered(true);
			}
			else if (toggle.id == 9) toggle.setStateTriggered(!toggle.isStateTriggered());
			else if (toggle.id > 9) for (BetterButtonToggle b : buttons) {
				if (b.id > 9 && b.id != toggle.id) b.setStateTriggered(false);
				toggle.setStateTriggered(true);
			}
		}
		ClickMachine.NETWORK.sendToServer(new MessageButtonClick(button.id));
	}

	public void setButtonState(int buttonId, boolean on) {
		buttons[buttonId].setStateTriggered(on);
	}

	public static void setFormatArgs(int button, Object... args) {
		Buttons.VALUES[button].formatArgs = args;
	}

	public TileAutoClick getTile() {
		return tile;
	}

	enum Buttons {
		SPEED_0(18 * 2 - 1, 18, 176, 0),
		SPEED_1(18 * 3 - 1, 18, 176, 18),
		SPEED_2(18 * 4 - 1, 18, 176, 18 * 2),
		SPEED_3(18 * 2 - 1, 36, 176, 18 * 3),
		SPEED_4(18 * 3 - 1, 36, 176, 18 * 4),
		SPEED_5(18 * 4 - 1, 36, 176, 18 * 5),
		SPEED_6(18 * 2 - 1, 54, 176, 18 * 6),
		SPEED_7(18 * 3 - 1, 54, 176, 18 * 7),
		SPEED_8(18 * 4 - 1, 54, 176, 18 * 8),
		SNEAK(18 * 6 - 1, 18, 176, 18 * 9),
		LEFT_CLICK(18 * 6 - 10, 54, 176, 18 * 10),
		RIGHT_CLICK(18 * 7 - 10, 54, 176, 18 * 11);

		static final Buttons[] VALUES = Buttons.values();

		int id;
		int x;
		int y;
		int u;
		int v;
		String unlocalized;
		Object[] formatArgs = new Object[0];

		Buttons(int x, int y, int u, int v) {
			this.id = ordinal();
			this.x = x - 1;
			this.y = y - 2;
			this.u = u;
			this.v = v;
			this.unlocalized = "gui.clickmachine." + name().toLowerCase(Locale.ROOT) + (ClickMachineConfig.usesRF && id < 9 ? "rf.tooltip" : ".tooltip");
		}

		List<String> getTooltip() {
			return Arrays.asList(I18n.format(unlocalized, formatArgs));
		}

		BetterButtonToggle getAndInitButton(GuiAutoClick gui) {
			BetterButtonToggle b = new BetterButtonToggle(id, gui.guiLeft + x, gui.guiTop + y, 18, 18, false);
			b.initTextureValues(u, v, 18, 0, GUI_TEXTURE);
			return b;
		}

	}

}
