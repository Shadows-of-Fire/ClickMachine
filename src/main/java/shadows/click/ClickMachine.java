package shadows.click;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
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

	public static final AutoClickerBlock AUTO_CLICKER = new AutoClickerBlock();
	public static final MenuType<AutoClickContainer> CONTAINER = ContainerUtil.makeType(AutoClickContainer::new);
	public static final BlockEntityType<AutoClickerTile> TILE = new TickingBlockEntityType<>(AutoClickerTile::new, ImmutableSet.of(AUTO_CLICKER), false, true);

	public ClickMachine() {
		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::blockJoin);
		ClickMachineConfig.init(new Configuration(new File(FMLPaths.CONFIGDIR.get().toFile(), "clickmachine.cfg")));
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		e.getRegistry().register(AUTO_CLICKER.setRegistryName(MODID, "auto_clicker"));
		ForgeRegistries.ITEMS.register(new BlockItem(AUTO_CLICKER, new Item.Properties().tab(CreativeModeTab.TAB_REDSTONE)).setRegistryName(AUTO_CLICKER.getRegistryName()));
	}

	@SubscribeEvent
	public void container(Register<MenuType<?>> e) {
		e.getRegistry().register(CONTAINER.setRegistryName(MODID, "container"));
	}

	@SubscribeEvent
	public void tiles(Register<BlockEntityType<?>> e) {
		e.getRegistry().register(TILE.setRegistryName(MODID, "tile"));
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		Ingredient diorite = Ingredient.of(Items.DIORITE);
		HELPER.addShaped(AUTO_CLICKER, 3, 3, diorite, diorite, diorite, diorite, Blocks.CHORUS_FLOWER, diorite, diorite, Blocks.REDSTONE_BLOCK, diorite);
		LootSystem.defaultBlockTable(AUTO_CLICKER);
	}

	public void blockJoin(EntityJoinWorldEvent e) {
		if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
	}

}
