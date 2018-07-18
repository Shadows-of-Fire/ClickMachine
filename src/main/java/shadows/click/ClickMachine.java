package shadows.click;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.ClickGuiHandler;
import shadows.click.net.MessageButtonClick;
import shadows.click.net.MessageUpdateGui;
import shadows.click.net.MessageButtonClick.ButtonClickHandler;
import shadows.click.net.MessageUpdateGui.UpdatePowerHandler;
import shadows.click.proxy.ClickProxy;
import shadows.placebo.registry.RegistryInformationV2;
import shadows.placebo.util.RecipeHelper;

@Mod(modid = ClickMachine.MODID, name = ClickMachine.MODNAME, version = ClickMachine.VERSION, dependencies = "required-after:placebo@[1.4.0,)")
public class ClickMachine {

	public static final String MODID = "clickmachine";
	public static final String MODNAME = "Click Machine";
	public static final String VERSION = "1.0.1";

	public static final Logger LOG = LogManager.getLogger(MODID);
	public static final RegistryInformationV2 INFO = new RegistryInformationV2(MODID, CreativeTabs.REDSTONE);
	public static final RecipeHelper HELPER = new RecipeHelper(MODID, MODNAME, INFO.getRecipeList());

	public static final BlockAutoClick AUTO_CLICKER = new BlockAutoClick();
	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(MODID);

	@Instance
	public static ClickMachine INSTANCE;

	@SidedProxy(serverSide = "shadows.click.proxy.ClickProxy", clientSide = "shadows.click.proxy.ClickClientProxy")
	public static ClickProxy PROXY;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(this);
		GameRegistry.registerTileEntity(TileAutoClick.class, AUTO_CLICKER.getRegistryName());
		NetworkRegistry.INSTANCE.registerGuiHandler(INSTANCE, new ClickGuiHandler());
		ClickMachineConfig.init(new Configuration(e.getSuggestedConfigurationFile()));
		PROXY.setupGuiArgs();
		int x = 0;
		NETWORK.registerMessage(ButtonClickHandler.class, MessageButtonClick.class, x++, Side.SERVER);
		NETWORK.registerMessage(UpdatePowerHandler.class, MessageUpdateGui.class, x++, Side.CLIENT);
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		INFO.getBlockList().register(e.getRegistry());
		INFO.getItemList().register(ForgeRegistries.ITEMS);
	}

	@SubscribeEvent
	public void recipes(Register<IRecipe> e) {
		Ingredient diorite = Ingredient.fromStacks(new ItemStack(Blocks.STONE, 1, 4));
		HELPER.addShaped(AUTO_CLICKER, 3, 3, diorite, diorite, diorite, diorite, Blocks.CHORUS_FLOWER, diorite, diorite, Blocks.REDSTONE_BLOCK, diorite);
		INFO.getRecipeList().register(e.getRegistry());
	}

}
