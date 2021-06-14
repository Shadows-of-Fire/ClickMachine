package shadows.click;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntityType;
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
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.ContainerAutoClick;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.placebo.config.Configuration;
import shadows.placebo.loot.LootSystem;
import shadows.placebo.recipe.RecipeHelper;

@Mod(ClickMachine.MODID)
public class ClickMachine {

	public static final String MODID = "clickmachine";

	public static final Logger LOG = LogManager.getLogger(MODID);
	public static final RecipeHelper HELPER = new RecipeHelper(MODID);

	public static final BlockAutoClick AUTO_CLICKER = new BlockAutoClick();
	public static final ContainerType<ContainerAutoClick> CONTAINER = new ContainerType<>(ContainerAutoClick::new);
	public static final TileEntityType<TileAutoClick> TILE = new TileEntityType<>(TileAutoClick::new, ImmutableSet.of(AUTO_CLICKER), null);

	public ClickMachine() {
		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::blockJoin);
		ClickMachineConfig.init(new Configuration(new File(FMLPaths.CONFIGDIR.get().toFile(), "clickmachine.cfg")));
		FMLJavaModLoadingContext.get().getModEventBus().register(this);
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		e.getRegistry().register(AUTO_CLICKER.setRegistryName(MODID, "auto_clicker"));
		ForgeRegistries.ITEMS.register(new BlockItem(AUTO_CLICKER, new Item.Properties().group(ItemGroup.REDSTONE)).setRegistryName(AUTO_CLICKER.getRegistryName()));
	}

	@SubscribeEvent
	public void container(Register<ContainerType<?>> e) {
		e.getRegistry().register(CONTAINER.setRegistryName(MODID, "container"));
	}

	@SubscribeEvent
	public void tiles(Register<TileEntityType<?>> e) {
		e.getRegistry().register(TILE.setRegistryName(MODID, "tile"));
	}

	@SubscribeEvent
	public void setup(FMLCommonSetupEvent e) {
		Ingredient diorite = Ingredient.fromItems(Items.DIORITE);
		HELPER.addShaped(AUTO_CLICKER, 3, 3, diorite, diorite, diorite, diorite, Blocks.CHORUS_FLOWER, diorite, diorite, Blocks.REDSTONE_BLOCK, diorite);
		LootSystem.defaultBlockTable(AUTO_CLICKER);
	}

	public void blockJoin(EntityJoinWorldEvent e) {
		if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
	}

}
