package dev.shadowsoffire.clickmachine.data;

import java.util.concurrent.CompletableFuture;

import dev.shadowsoffire.clickmachine.ClickMachine;
import dev.shadowsoffire.placebo.datagen.LegacyRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

public class ClickRecipes extends LegacyRecipeProvider {

    public ClickRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, ClickMachine.MODID);
    }

    @Override
    protected void genRecipes(RecipeOutput recipeOutput, HolderLookup.Provider registries) {
        addShaped(ClickMachine.CLICK_MACHINE, 3, 3,
            Items.DIORITE, Items.DIORITE, Items.DIORITE,
            Items.DIORITE, Items.CHORUS_FLOWER, Items.DIORITE,
            Items.DIORITE, Tags.Items.STORAGE_BLOCKS_REDSTONE, Items.DIORITE);

    }
}
