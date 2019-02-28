package shadows.click;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;
import net.minecraftforge.fml.relauncher.Side;
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.ClickGuiHandler;
import shadows.click.net.MessageUpdateGui;
import shadows.click.net.MessageUpdateGui.UpdateGuiHandler;
import shadows.placebo.event.MessageRegistryEvent;
import shadows.placebo.item.ItemBlockBase;
import shadows.placebo.util.PlaceboUtil;
import shadows.placebo.util.RecipeHelper;

@Mod(modid = ClickMachine.MODID, name = ClickMachine.MODNAME, version = ClickMachine.VERSION, dependencies = "required-after:placebo@[2.0.0,)")
public class ClickMachine extends RecipeHelper {

	public static final String MODID = "clickmachine";
	public static final String MODNAME = "Click Machine";
	public static final String VERSION = "2.0.0";
	public static final Logger LOG = LogManager.getLogger(MODID);

	@ObjectHolder(MODID + ":auto_clicker")
	public static final BlockAutoClick AUTO_CLICKER = PlaceboUtil.initBlock(new BlockAutoClick(), MODID, "auto_clicker", 5, 5);

	@Instance
	public static ClickMachine INSTANCE;

	public ClickMachine() {
		super(MODID, MODNAME);
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(this);
		GameRegistry.registerTileEntity(TileAutoClick.class, AUTO_CLICKER.getRegistryName());
		NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new ClickGuiHandler());
		ClickMachineConfig.init(new Configuration(e.getSuggestedConfigurationFile()));
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		e.getRegistry().register(AUTO_CLICKER);
	}

	@SubscribeEvent
	public void items(Register<Item> e) {
		e.getRegistry().register(new ItemBlockBase(AUTO_CLICKER));
	}

	@SubscribeEvent
	public void messages(MessageRegistryEvent e) {
		e.registerMessage(MessageUpdateGui.class, UpdateGuiHandler::new, Side.CLIENT);
	}

	@Override
	public void addRecipes() {
		ItemStack diorite = new ItemStack(Blocks.STONE, 1, 4);
		addShaped(AUTO_CLICKER, 3, 3, diorite, diorite, diorite, diorite, Blocks.CHORUS_FLOWER, diorite, diorite, Blocks.REDSTONE_BLOCK, diorite);
	}

}
