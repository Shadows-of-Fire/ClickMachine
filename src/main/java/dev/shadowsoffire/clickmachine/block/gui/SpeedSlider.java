package dev.shadowsoffire.clickmachine.block.gui;

import dev.shadowsoffire.clickmachine.ClickMachineConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/// NOTE TO URSELF - update the slider on first load of world - either needs packet notif on change or slider needs to update when data is received on client!:tm:
public class SpeedSlider extends AbstractSliderButton {

	protected static final int minValue = 0, maxValue = 8;
	protected static final float stepSize = 1 / 9F;

	protected final AutoClickScreen gui;

	public SpeedSlider(AutoClickScreen gui, int x, int y, int width, int height) {
		super(x, y, width, height, CommonComponents.EMPTY, normalizeValue(gui.getMenu().getSpeedIdx()));
		this.gui = gui;
		this.updateMessage();
	}

	@Override
	protected void updateMessage() {
		int spd = denormalizeValue(this.value);
		int ticksPerClick = ClickMachineConfig.speeds[spd];
		double cps = 1D / ticksPerClick * 20;
		this.setMessage(Component.translatable("gui.clickmachine.speed", String.format("%.2f", cps)));
	}

	@Override
	protected void applyValue() {
		Minecraft.getInstance().gameMode.handleInventoryButtonClick(this.gui.getMenu().containerId, 4 + denormalizeValue(this.value));
	}

	public void setValue(int value) {
		if (!this.gui.isDragging()) {
			this.value = normalizeValue(value);
			this.updateMessage();
		}
	}

	/**
	 * Converts an int value within the range into a slider percentage.
	 */
	public static double normalizeValue(double value) {
		return Mth.clamp((snapToStepClamp(value) - minValue) / (maxValue - minValue), 0.0D, 1.0D);
	}

	/**
	 * Converts a slider percentage to its bounded int value.
	 */
	public static int denormalizeValue(double value) {
		return (int) snapToStepClamp(Mth.lerp(Mth.clamp(value, 0.0D, 1.0D), minValue, maxValue));
	}

	private static double snapToStepClamp(double valueIn) {
		if (stepSize > 0.0F) {
			valueIn = stepSize * Math.round(valueIn / stepSize);
		}

		return Mth.clamp(valueIn, minValue, maxValue);
	}

}
