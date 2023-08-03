package dev.shadowsoffire.clickmachine;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import dev.shadowsoffire.clickmachine.block.AutoClickerBlock;
import dev.shadowsoffire.clickmachine.block.AutoClickerTile;
import dev.shadowsoffire.clickmachine.block.gui.AutoClickContainer;
import dev.shadowsoffire.clickmachine.util.FakePlayerUtil.UsefulFakePlayer;
import dev.shadowsoffire.placebo.block_entity.TickingBlockEntityType;
import dev.shadowsoffire.placebo.config.Configuration;
import dev.shadowsoffire.placebo.loot.LootSystem;
import dev.shadowsoffire.placebo.menu.MenuUtil;
import dev.shadowsoffire.placebo.tabs.TabFillingRegistry;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ObjectHolder;
import net.minecraftforge.registries.RegisterEvent;

@Mod(ClickMachine.MODID)
public class ClickMachine {

    public static final String MODID = "clickmachine";

    public static final Logger LOG = LogManager.getLogger(MODID);

    @ObjectHolder(registryName = "block", value = ClickMachine.MODID + ":auto_clicker")
    public static final AutoClickerBlock AUTO_CLICKER = null;

    @ObjectHolder(registryName = "menu", value = ClickMachine.MODID + ":auto_clicker")
    public static final MenuType<AutoClickContainer> AUTO_CLICKER_MENU = null;

    @ObjectHolder(registryName = "block_entity_type", value = ClickMachine.MODID + ":auto_clicker")
    public static final BlockEntityType<AutoClickerTile> AUTO_CLICKER_TILE = null;

    public ClickMachine() {
        MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::blockJoin);
        ClickMachineConfig.init(new Configuration(new File(FMLPaths.CONFIGDIR.get().toFile(), "clickmachine.cfg")));
        FMLJavaModLoadingContext.get().getModEventBus().register(this);
    }

    @SubscribeEvent
    public void register(RegisterEvent e) {
        if (e.getForgeRegistry() == (Object) ForgeRegistries.BLOCKS) {
            AutoClickerBlock clicker = new AutoClickerBlock();
            ForgeRegistries.BLOCKS.register("auto_clicker", clicker);
            ForgeRegistries.ITEMS.register("auto_clicker", new BlockItem(clicker, new Item.Properties()));
            ForgeRegistries.MENU_TYPES.register("auto_clicker", MenuUtil.posType(AutoClickContainer::new));
            ForgeRegistries.BLOCK_ENTITY_TYPES.register("auto_clicker", new TickingBlockEntityType<>(AutoClickerTile::new, ImmutableSet.of(clicker), false, true));
        }
    }

    @SubscribeEvent
    public void setup(FMLCommonSetupEvent e) {
        LootSystem.defaultBlockTable(AUTO_CLICKER);
        e.enqueueWork(() -> {
            TabFillingRegistry.register(() -> AUTO_CLICKER, CreativeModeTabs.REDSTONE_BLOCKS);
        });
    }

    public void blockJoin(EntityJoinLevelEvent e) {
        if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
    }

}
