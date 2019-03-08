package shadows.click.proxy;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import shadows.click.ClickMachine;
import shadows.click.ClickMachineConfig;
import shadows.click.block.gui.GuiAutoClick;
import shadows.placebo.Placebo;
import shadows.placebo.util.PlaceboUtil;

@EventBusSubscriber(value = Side.CLIENT, modid = ClickMachine.MODID)
public class ClickClientProxy extends ClickProxy {

	@Override
	public void setupGuiArgs() {
		for (int i = 0; i < 9; i++) {
			if (ClickMachineConfig.usesRF) GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i], ClickMachineConfig.powerPerSpeed[i]);
			else GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i]);
		}
	}

	@SubscribeEvent
	public static void models(ModelRegistryEvent e) {
		if (ClickMachineConfig.classicTex) {
			PlaceboUtil.sMRL(ClickMachine.MODID, "auto_clicker_old", ClickMachine.AUTO_CLICKER, 0, "facing=north");
			Placebo.PROXY.useRenamedMapper(ClickMachine.AUTO_CLICKER, "auto_clicker_old");
		} else PlaceboUtil.sMRL(ClickMachine.AUTO_CLICKER, 0, "facing=north");
	}

}
