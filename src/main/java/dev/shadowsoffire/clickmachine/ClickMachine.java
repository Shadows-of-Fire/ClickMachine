package dev.shadowsoffire.clickmachine;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dev.shadowsoffire.clickmachine.ClickMachineConfig.ConfigPayload;
import dev.shadowsoffire.clickmachine.block.ClickMachineBlock;
import dev.shadowsoffire.clickmachine.block.ClickMachineTile;
import dev.shadowsoffire.clickmachine.data.ClickRecipes;
import dev.shadowsoffire.clickmachine.data.LootProvider;
import dev.shadowsoffire.clickmachine.gui.ClickMachineMenu;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType.TickSide;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.datagen.DataGenBuilder;
import dev.shadowsoffire.placebo.network.PayloadHelper;
import dev.shadowsoffire.placebo.registry.DeferredHelper;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import dev.shadowsoffire.placebo.util.RunnableReloader;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@Mod(ClickMachine.MODID)
public class ClickMachine {

    public static final String MODID = "clickmachine";

    public static final Logger LOG = LogManager.getLogger(MODID);

    private static final DeferredHelper R = DeferredHelper.create(MODID);

    public static final Holder<Block> CLICK_MACHINE = R.block("click_machine", ClickMachineBlock::new);
    public static final Holder<Item> CLICK_MACHINE_ITEM = R.blockItem("click_machine", CLICK_MACHINE);
    public static final MenuType<ClickMachineMenu> CLICK_MACHINE_MENU = R.menuWithPos("click_machine", ClickMachineMenu::new);
    public static final BlockEntityType<ClickMachineTile> CLICK_MACHINE_TILE = R.tickingBlockEntity("click_machine", ClickMachineTile::new, TickSide.SERVER, CLICK_MACHINE);

    public ClickMachine(IEventBus bus) {
        bus.register(this);
        bus.register(R);
        NeoForge.EVENT_BUS.register(new GameBusEvents());
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        e.enqueueWork(() -> {
            TabFillingRegistry.registerSimple(CreativeModeTabs.REDSTONE_BLOCKS, CLICK_MACHINE_ITEM.value());
        });
        PayloadHelper.registerPayload(new ConfigPayload.Provider());
    }

    @SubscribeEvent
    public void data(GatherDataEvent e) {
        DataGenBuilder.create(MODID)
            .provider(ClickRecipes::new)
            .provider(LootProvider::create)
            .build(e);
    }

    @SubscribeEvent
    public void caps(RegisterCapabilitiesEvent e) {
        e.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, CLICK_MACHINE_TILE, (be, ctx) -> be.getEnergy());
        e.registerBlockEntity(Capabilities.ItemHandler.BLOCK, CLICK_MACHINE_TILE, (be, ctx) -> be.getHandler());
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }

    private class GameBusEvents {

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public void blockJoin(EntityJoinLevelEvent e) {
            if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
        }

        @SubscribeEvent(priority = EventPriority.LOWEST)
        public void dims(EntityEvent.Size e) {
            if (e.getEntity() instanceof UsefulFakePlayer) {
                e.setNewSize(e.getNewSize().withEyeHeight(0));
            }
        }

        @SubscribeEvent
        public void reloads(AddReloadListenerEvent e) {
            e.addListener(RunnableReloader.of(() -> ClickMachineConfig.init(new Configuration(MODID))));
        }

        @SubscribeEvent
        public void sync(OnDatapackSyncEvent e) {
            if (e.getPlayer() != null) {
                PacketDistributor.sendToPlayer(e.getPlayer(), new ConfigPayload());
            }
            else {
                PacketDistributor.sendToAllPlayers(new ConfigPayload());
            }
        }
    }

}
