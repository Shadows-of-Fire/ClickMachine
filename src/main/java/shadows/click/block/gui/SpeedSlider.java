package shadows.click.block.gui;

import net.minecraft.client.gui.widget.AbstractSlider;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import shadows.click.ClickMachineConfig;
import shadows.placebo.Placebo;
import shadows.placebo.net.MessageButtonClick;

public class SpeedSlider extends AbstractSlider implements ITickable {

	protected static final int minValue = 0, maxValue = 8;
	protected static final float stepSize = 1 / 9F;

	protected final GuiAutoClick gui;

	public SpeedSlider(GuiAutoClick gui, int x, int y, int width, int height) {
		super(x, y, width, height, StringTextComponent.EMPTY, -0.001);
		this.gui = gui;
	}

	@Override
	public void tick() {
		if (sliderValue == -0.001) {
			int tileVal = this.gui.getContainer().data.get(1);
			sliderValue = normalizeValue(tileVal);
			func_230979_b_();
		}
	}

	/**
	 * MojMap: updateMessage
	 */
	@Override
	protected void func_230979_b_() {
		int spd = denormalizeValue(sliderValue);
		int ticksPerClick = ClickMachineConfig.speeds[spd];
		double cps = (1D / ticksPerClick) * 20;
		this.setMessage(new TranslationTextComponent("gui.clickmachine.speed", String.format("%.2f", cps)));
	}

	/**
	 * MojMap: applyValue
	 */
	@Override
	protected void func_230972_a_() {
		Placebo.CHANNEL.sendToServer(new MessageButtonClick(4 + (int) denormalizeValue(this.sliderValue)));
	}

	/**
	 * Converts an int value within the range into a slider percentage.
	 */
	public static double normalizeValue(double value) {
		return MathHelper.clamp((snapToStepClamp(value) - minValue) / (maxValue - minValue), 0.0D, 1.0D);
	}

	/**
	 * Converts a slider percentage to its bounded int value.
	 */
	public static int denormalizeValue(double value) {
		return (int) snapToStepClamp(MathHelper.lerp(MathHelper.clamp(value, 0.0D, 1.0D), minValue, maxValue));
	}

	private static double snapToStepClamp(double valueIn) {
		if (stepSize > 0.0F) {
			valueIn = (double) (stepSize * (float) Math.round(valueIn / (double) stepSize));
		}

		return MathHelper.clamp(valueIn, minValue, maxValue);
	}

}
