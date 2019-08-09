package shadows.click;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent.Register;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.registries.ForgeRegistries;
import shadows.click.block.BlockAutoClick;
import shadows.click.block.TileAutoClick;
import shadows.click.block.gui.ClickGuiHandler;
import shadows.click.block.gui.GuiAutoClick;
import shadows.click.net.MessageButtonClick;
import shadows.click.net.MessageButtonClick.ButtonClickHandler;
import shadows.click.net.MessageUpdateGui;
import shadows.click.net.MessageUpdateGui.UpdatePowerHandler;
import shadows.placebo.config.Configuration;
import shadows.placebo.recipe.RecipeHelper;

@Mod(ClickMachine.MODID)
public class ClickMachine {

	public static final String MODID = "clickmachine";

	public static final Logger LOG = LogManager.getLogger(MODID);
	public static final RecipeHelper HELPER = new RecipeHelper(MODID);

	public static final BlockAutoClick AUTO_CLICKER = new BlockAutoClick();
	//Formatter::off
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(MODID, MODID))
            .clientAcceptedVersions(s->true)
            .serverAcceptedVersions(s->true)
            .networkProtocolVersion(() -> "1.0.0")
            .simpleChannel();
    //Formatter::on

	@EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		MinecraftForge.EVENT_BUS.register(this);
		ClickMachineConfig.init(new Configuration(e.getSuggestedConfigurationFile()));
		DistExecutor.runWhenOn(Dist.CLIENT, () -> () -> {
			for (int i = 0; i < 9; i++) {
				if (ClickMachineConfig.usesRF) GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i], ClickMachineConfig.powerPerSpeed[i]);
				else GuiAutoClick.setFormatArgs(i, ClickMachineConfig.speeds[i]);
			}
		});
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
