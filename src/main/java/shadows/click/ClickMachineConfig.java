package shadows.click;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dev.shadowsoffire.placebo.config.Configuration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public class ClickMachineConfig {

	public static int[] speeds = new int[] { 500, 200, 100, 50, 20, 10, 5, 2, 1 };
	public static boolean usesRF = false;
	public static int maxPowerStorage = 50000;
	public static int[] powerPerSpeed = new int[] { 0, 3, 5, 10, 25, 50, 100, 250, 500 };
	public static Set<Item> blacklistedItems = new HashSet<>();

	public static void init(Configuration cfg) {

		String[] def = new String[9];
		for (int i = 0; i < 9; i++)
			def[i] = ((Integer) speeds[i]).toString();

		String[] unparsed = cfg.getStringList("Speeds", Configuration.CATEGORY_GENERAL, def, "The possible speeds of the auto clicker, from 0-8.  Must have 9 values.");

		for (int i = 0; i < 9; i++)
			speeds[i] = Integer.parseInt(unparsed[i]);

		usesRF = cfg.getBoolean("Uses RF", Configuration.CATEGORY_GENERAL, usesRF, "If the auto clicker uses RF");

		def = new String[9];
		for (int i = 0; i < 9; i++)
			def[i] = ((Integer) powerPerSpeed[i]).toString();

		unparsed = cfg.getStringList("RF Costs", Configuration.CATEGORY_GENERAL, def, "The RF cost per tick for each speed, from 0-8.  Must have 9 values.  Unused if \"Uses RF\" = false");

		for (int i = 0; i < 9; i++)
			powerPerSpeed[i] = Integer.parseInt(unparsed[i]);

		maxPowerStorage = cfg.getInt("Max Power Storage", Configuration.CATEGORY_GENERAL, maxPowerStorage, 0, Integer.MAX_VALUE, "How much power the auto clicker can store.  Also the max input rate.  Unused if \"Uses RF\" = false");

		String[] blacklist = cfg.getStringList("Item Blacklist", Configuration.CATEGORY_GENERAL, new String[] { "minecraft:bedrock" }, "Items that may not be held by the clicker");
		blacklistedItems = Arrays.stream(blacklist).map(ResourceLocation::new).map(ForgeRegistries.ITEMS::getValue).filter(i -> !Items.AIR.equals(i)).collect(Collectors.toSet());

		if (cfg.hasChanged()) cfg.save();
	}

}
