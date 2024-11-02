package dev.shadowsoffire.clickmachine;

import dev.shadowsoffire.clickmachine.gui.ClickMachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = ClickMachine.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public class ClickMachineClient {

    @SubscribeEvent
    public static void setup(RegisterMenuScreensEvent e) {
        e.register(ClickMachine.CLICK_MACHINE_MENU, ClickMachineScreen::new);
    }
}
