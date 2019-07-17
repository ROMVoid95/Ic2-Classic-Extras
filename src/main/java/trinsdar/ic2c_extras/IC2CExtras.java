package trinsdar.ic2c_extras;

import ic2.api.classic.addon.IC2Plugin;
import ic2.api.classic.addon.PluginBase;
import ic2.api.classic.addon.misc.IOverrideObject;
import ic2.api.classic.addon.misc.SideGateway;
import ic2.api.recipe.IBasicMachineRecipeManager;
import ic2.api.recipe.Recipes;
import ic2.core.IC2;
import ic2.core.platform.registry.Ic2Items;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.Logger;
import trinsdar.ic2c_extras.items.override.ItemMisc2;
import trinsdar.ic2c_extras.proxy.CommonProxy;
import trinsdar.ic2c_extras.util.CreativeTabIC2CExtras;
import trinsdar.ic2c_extras.recipes.Ic2cExtrasRecipes;
import trinsdar.ic2c_extras.util.Icons;
import trinsdar.ic2c_extras.util.references.RodLang;

import java.util.Map;

@Mod(name = IC2CExtras.NAME, modid = IC2CExtras.MODID, version = IC2CExtras.VERSION, dependencies = IC2CExtras.DEPENDS)
public class IC2CExtras
{
    public static final String MODID = "ic2c_extras";
    public static final String NAME = "IC2CExtras";
    public static final String VERSION = "@VERSION@";
    public static final String DEPENDS = "required-after:ic2;required-after:ic2-classic-spmod;after:gtclassic";
    public static final CreativeTabs creativeTab = new CreativeTabIC2CExtras(MODID);

    @SidedProxy(clientSide = "trinsdar.ic2c_extras.proxy.ClientProxy", serverSide = "trinsdar.ic2c_extras.proxy.ServerProxy")
    public static CommonProxy proxy;

    @Mod.Instance
    public static IC2CExtras instance;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        proxy.preInit(event);
        if (!IC2.config.getFlag("NonRadiation")){
            Recipes.metalformerExtruding = (IBasicMachineRecipeManager)Ic2cExtrasRecipes.extruding.toIC2Exp();
            Recipes.metalformerCutting = (IBasicMachineRecipeManager) Ic2cExtrasRecipes.cutting.toIC2Exp();
            Recipes.metalformerRolling = (IBasicMachineRecipeManager)Ic2cExtrasRecipes.rolling.toIC2Exp();
            Recipes.oreWashing = (IBasicMachineRecipeManager)Ic2cExtrasRecipes.oreWashingPlant.toIC2Exp();
            Recipes.centrifuge = (IBasicMachineRecipeManager)Ic2cExtrasRecipes.thermalCentrifuge.toIC2Exp();
        }
        RodLang.overrideLang();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent fmlInitializationEvent)
    {
        proxy.init();
        MinecraftForge.EVENT_BUS.register(new Ic2cExtrasRecipes());
        MinecraftForge.EVENT_BUS.register(new Radiation());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent fmlPostInitializationEvent)
    {
        proxy.postInit();
    }


}
