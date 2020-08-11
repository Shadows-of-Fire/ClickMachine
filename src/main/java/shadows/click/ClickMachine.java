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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.ContainerAutoClick;
import shadows.click.block.gui.GuiAutoClick;
import shadows.click.net.MessageUpdateGui;
import shadows.click.util.FakePlayerUtil.UsefulFakePlayer;
import shadows.placebo.config.Configuration;
import shadows.placebo.loot.LootSystem;
import shadows.placebo.recipe.RecipeHelper;
import shadows.placebo.util.NetworkUtils;

@Mod(ClickMachine.MODID)
public class ClickMachine {

	public static final String MODID = "clickmachine";

	public static final Logger LOG = LogManager.getLogger(MODID);

	public static final BlockAutoClick AUTO_CLICKER = new BlockAutoClick();
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, MODID))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on
	public static final ContainerType<ContainerAutoClick> CONTAINER = new ContainerType<>((IContainerFactory<ContainerAutoClick>) ContainerAutoClick::new);
	public static final TileEntityType<TileAutoClick> TILE = new TileEntityType<>(TileAutoClick::new, ImmutableSet.of(AUTO_CLICKER), null);

	public ClickMachine() {
		MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGHEST, this::blockJoin);
		ClickMachineConfig.init(new Configuration(new File(FMLPaths.CONFIGDIR.get().toFile(), "clickmachine.cfg")));
		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
			for (int i = 0; i < 9; i++) {
				if (ClickMachineConfig.usesRF) GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i], ClickMachineConfig.powerPerSpeed[i]);
				else GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i]);
			}
		});
		NetworkUtils.registerMessage(CHANNEL, 0, new MessageUpdateGui());
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
		LootSystem.defaultBlockTable(AUTO_CLICKER);
	}

	public void blockJoin(EntityJoinWorldEvent e) {
		if (e.getEntity() instanceof UsefulFakePlayer) e.setCanceled(true);
	}

}
