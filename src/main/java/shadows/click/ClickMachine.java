package shadows.click;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.proxy.ClickProxy;
import shadows.placebo.registry.RegistryInformationV2;
import shadows.placebo.util.RecipeHelper;

@Mod(modid = ClickMachine.MODID, name = ClickMachine.MODNAME, version = ClickMachine.VERSION)
public class ClickMachine {

	public static final String MODID = "clickmachine";
	public static final String MODNAME = "Click Machine";
	public static final String VERSION = "1.0.0";

	public static final Logger LOG = LogManager.getLogger(MODID);
	public static final RegistryInformationV2 INFO = new RegistryInformationV2(MODID, CreativeTabs.REDSTONE);
	public static final RecipeHelper HELPER = new RecipeHelper(MODID, MODNAME, INFO.getRecipeList());

	public static final BlockAutoClick AUTO_CLICKER = new BlockAutoClick();

	@Instance
	public static ClickMachine INSTANCE;

	@SidedProxy(serverSide = "shadows.click.proxy.ClickProxy", clientSide = "shadows.click.proxy.ClickClientProxy")
	public static ClickProxy PROXY;

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(this);
		GameRegistry.registerTileEntity(TileAutoClick.class, AUTO_CLICKER.getRegistryName());
	}

	@SubscribeEvent
	public void blocks(Register<Block> e) {
		INFO.getBlockList().register(e.getRegistry());
		INFO.getItemList().register(ForgeRegistries.ITEMS);
	}

}
