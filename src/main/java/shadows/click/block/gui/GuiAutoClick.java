package shadows.click.block.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.mojang.blaze3d.platform.GlStateManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.config.GuiUtils;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.block.TileAutoClick;
import shadows.placebo.Placebo;
import shadows.placebo.net.MessageButtonClick;

public class GuiAutoClick extends ContainerScreen<ContainerAutoClick> {

	public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(ClickMachine.MODID, "textures/gui/auto_click.png");
	PlayerEntity player = Minecraft.getInstance().player;
	TileAutoClick tile;
	BetterButtonToggle[] buttons = new BetterButtonToggle[12];

	public GuiAutoClick(ContainerAutoClick container, PlayerInventory inv, ITextComponent name) {
		super(container, inv, name);
		this.tile = container.tile;
		++ySize;
	}

	@Override
	public void init() {
		super.init();
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
	public void render(int mouseX, int mouseY, float partialTicks) {
		this.renderBackground();
		super.render(mouseX, mouseY, partialTicks);
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
		int n = (int) (18 * 4 * Math.min(1, (1 - ((float) tile.getPower() / ClickMachineConfig.maxPowerStorage))));
		this.blit(151, 7 + n, 230, n, 18, 18 * 4);
		this.font.drawString(this.getNarrationMessage(), 8, 6, 4210752);
		this.font.drawString(player.inventory.getDisplayName().getFormattedText(), 8, this.ySize - 96 + 2, 4210752);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		Minecraft.getInstance().getTextureManager().bindTexture(GUI_TEXTURE);
		int i = (this.width - this.xSize) / 2;
		int j = (this.height - this.ySize) / 2;
		this.blit(i, j, 0, 0, this.xSize, this.ySize);
	}

	@Override
	protected void renderHoveredToolTip(int x, int y) {
		super.renderHoveredToolTip(x, y);
		for (BetterButtonToggle b : buttons)
			if (b.isMouseOver(x, y)) Buttons.VALUES[b.id].getTooltip().forEach(s -> GuiUtils.drawHoveringText(Arrays.asList(s), x, y, width, height, 0xFFFFFF, font));
		if (isPointInRegion(151, 7, 18, 18 * 4 - 1, x, y)) {
			GuiUtils.drawHoveringText(Arrays.asList(I18n.format("gui.clickmachine.power.tooltip", tile.getPower(), ClickMachineConfig.usesRF ? ClickMachineConfig.maxPowerStorage : 0)), x, y, width, height, 0xFFFFFF, font);
		}
	}

	protected void actionPerformed(BetterButtonToggle toggle) {
		if (toggle.id < 9) for (BetterButtonToggle b : buttons) {
			if (b.id < 9 && b.id != toggle.id) b.setStateTriggered(false);
			toggle.setStateTriggered(true);
		}
		else if (toggle.id == 9) toggle.setStateTriggered(!toggle.isStateTriggered());
		else if (toggle.id > 9) for (BetterButtonToggle b : buttons) {
			if (b.id > 9 && b.id != toggle.id) b.setStateTriggered(false);
			toggle.setStateTriggered(true);
		}
		Placebo.CHANNEL.sendToServer(new MessageButtonClick(toggle.id));
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
			this.unlocalized = "gui.clickmachine." + name().toLowerCase(Locale.ROOT) + (ClickMachineConfig.usesRF && id < 9 ? ".rf.tooltip" : ".tooltip");
		}

		List<String> getTooltip() {
			return Arrays.asList(I18n.format(unlocalized, formatArgs));
		}

		BetterButtonToggle getAndInitButton(GuiAutoClick gui) {
			BetterButtonToggle b = new BetterButtonToggle(id, gui.guiLeft + x, gui.guiTop + y, 18, 18, false);
			b.initTextureValues(u, v, 18, 0, GUI_TEXTURE);
			b.controller = gui;
			return b;
		}

	}

}
