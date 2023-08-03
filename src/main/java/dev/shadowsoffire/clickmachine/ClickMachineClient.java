package dev.shadowsoffire.clickmachine;

import dev.shadowsoffire.clickmachine.block.gui.AutoClickScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(modid = ClickMachine.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClickMachineClient {

	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e) {
		MenuScreens.register(ClickMachine.AUTO_CLICKER_MENU, AutoClickScreen::new);
	}
}
