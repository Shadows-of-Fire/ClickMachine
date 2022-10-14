package shadows.click;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
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
import shadows.click.block.AutoClickerBlock;
import shadows.click.block.AutoClickerTile;
import shadows.click.block.gui.AutoClickContainer;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.placebo.block_entity.TickingBlockEntityType;
import shadows.placebo.config.Configuration;
import shadows.placebo.container.ContainerUtil;
import shadows.placebo.loot.LootSystem;
import shadows.placebo.recipe.RecipeHelper;

@Mod(ClickMachine.MODID)
public class ClickMachine {

	public static final String MODID = "clickmachine";

	public static final Logger LOG = LogManager.getLogger(MODID);
	public static final RecipeHelper HELPER = new RecipeHelper(MODID);

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
			ForgeRegistries.BLOCKS.register("auto_clicker", new AutoClickerBlock());
		}
		if (e.getForgeRegistry() == (Object) ForgeRegistries.ITEMS) {
			ForgeRegistries.ITEMS.register("auto_clicker", new BlockItem(AUTO_CLICKER, new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)));
		}
		if (e.getForgeRegistry() == (Object) ForgeRegistries.MENU_TYPES) {
			ForgeRegistries.MENU_TYPES.register("auto_clicker", ContainerUtil.makeType(AutoClickContainer::new));
		}
		if (e.getForgeRegistry() == (Object) ForgeRegistries.BLOCK_ENTITY_TYPES) {
			ForgeRegistries.BLOCK_ENTITY_TYPES.register("auto_clicker", new TickingBlockEntityType<>(AutoClickerTile::new, ImmutableSet.of(AUTO_CLICKER), false, true));
		}
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		LootSystem.defaultBlockTable(AUTO_CLICKER);
	}

	public void blockJoin(EntityJoinLevelEvent e) {
		if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
	}

}
