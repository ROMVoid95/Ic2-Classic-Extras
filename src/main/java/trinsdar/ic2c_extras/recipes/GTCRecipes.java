package trinsdar.ic2c_extras.recipes;

import gtclassic.api.material.GTMaterial;
import gtclassic.api.material.GTMaterialGen;
import gtclassic.common.GTItems;
import gtclassic.common.tile.GTTileCentrifuge;
import ic2.core.platform.registry.Ic2Items;
import net.minecraft.item.ItemStack;
import trinsdar.ic2c_extras.util.Registry;

public class GTCRecipes {
    public static void init() {
        CraftingRecipes.recipes.addRecipe(new ItemStack(GTItems.heatStorageHelium1), " T ", "THT", " T ", 'T', "casingTin", 'H', GTMaterialGen.getTube(GTMaterial.Helium, 1));
        CraftingRecipes.recipes.addRecipe(new ItemStack(GTItems.heatStorageHelium3), "TTT", "HHH", "TTT", 'T', "casingTin", 'H', GTMaterialGen.get(GTItems.heatStorageHelium1));
        CraftingRecipes.recipes.addRecipe(new ItemStack(GTItems.heatStorageHelium6), "THT", "TCT", "THT", 'T', "casingTin", 'H', GTMaterialGen.get(GTItems.heatStorageHelium3), 'C', Ic2Items.denseCopperPlate);
        GTTileCentrifuge.addRecipe("crushedCentrifugedUranium", 1, 0, GTTileCentrifuge.totalEu(6000), GTMaterialGen.getDust(GTMaterial.Uranium, 1), new ItemStack(Registry.uranium238, 2), new ItemStack(Registry.uranium235TinyDust));
    }
}
