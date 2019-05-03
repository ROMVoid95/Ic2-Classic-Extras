package trinsdar.ic2c_extras.blocks.tileentity;

import ic2.api.classic.item.IMachineUpgradeItem;
import ic2.api.classic.network.adv.NetworkField;
import ic2.api.classic.recipe.machine.MachineOutput;
import ic2.api.classic.tile.IStackOutput;
import ic2.api.recipe.IRecipeInput;
import ic2.core.RotationList;
import ic2.core.block.base.util.output.IStackRegistry;
import ic2.core.block.base.util.output.MultiSlotOutput;
import ic2.core.fluid.IC2Tank;
import ic2.core.inventory.container.ContainerIC2;
import ic2.core.inventory.filters.ArrayFilter;
import ic2.core.inventory.filters.BasicItemFilter;
import ic2.core.inventory.filters.CommonFilters;
import ic2.core.inventory.filters.IFilter;
import ic2.core.inventory.management.AccessRule;
import ic2.core.inventory.management.InventoryHandler;
import ic2.core.inventory.management.SlotType;
import ic2.core.platform.lang.components.base.LocaleComp;
import ic2.core.platform.registry.Ic2Items;
import ic2.core.util.misc.StackUtil;
import ic2.core.util.obj.ITankListener;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import trinsdar.ic2c_extras.blocks.container.ContainerFluidCanningMachine;
import trinsdar.ic2c_extras.blocks.container.ContainerMetalBender;
import trinsdar.ic2c_extras.blocks.tileentity.base.TileEntityFluidCannerBase;
import trinsdar.ic2c_extras.util.GuiMachine;
import trinsdar.ic2c_extras.util.recipelists.FluidCanningRecipeList;
import trinsdar.ic2c_extras.util.recipelists.FluidCanningRecipeList.FluidCanningRecipe;
import trinsdar.ic2c_extras.util.references.Ic2cExtrasLang;
import trinsdar.ic2c_extras.util.references.Ic2cExtrasResourceLocations;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

public class TileEntityFluidCanningMachine extends TileEntityFluidCannerBase implements ITankListener {
    @NetworkField(index = 13)
    public IC2Tank inputTank = new IC2Tank(10000);
    @NetworkField(index = 14)
    public IC2Tank outputTank = new IC2Tank(10000);
    public static FluidCanningRecipeList fluidCanning = new FluidCanningRecipeList("fluidCanning");

    public TileEntityFluidCanningMachine() {
        super(3, 4, 1, 400, 32);
        slotInput = 0;
        slotOutput = 2;
        this.inputTank.addListener(this);
        this.outputTank.addListener(this);
        this.inputTank.setCanFill(true);
        this.outputTank.setCanFill(false);
        this.inputTank.setCanDrain(false);
        this.outputTank.setCanDrain(true);
        this.addGuiFields("inputTank", "outputTank");
    }

    @Override
    protected void addSlots(InventoryHandler handler)
    {
        handler.registerDefaultSideAccess(AccessRule.Both, RotationList.ALL);
        handler.registerDefaultSlotAccess(AccessRule.Both, 1);
        handler.registerDefaultSlotAccess(AccessRule.Import, slotInput);
        handler.registerDefaultSlotAccess(AccessRule.Export, slotOutput);
        handler.registerDefaultSlotsForSide(RotationList.UP.getOppositeList(), slotOutput);
        handler.registerDefaultSlotsForSide(RotationList.DOWN.getOppositeList(), slotInput);
        handler.registerInputFilter(new ArrayFilter(CommonFilters.DischargeEU, new BasicItemFilter(Items.REDSTONE), new BasicItemFilter(Ic2Items.suBattery)), 1);
        handler.registerOutputFilter(CommonFilters.NotDischargeEU, 1);
        handler.registerSlotType(SlotType.Fuel, 1);
        handler.registerSlotType(SlotType.Input, slotInput);
        handler.registerSlotType(SlotType.Output, slotOutput);
    }

    @Override
    public void onTankChanged(IFluidTank tank)
    {
        this.getNetwork().updateTileGuiField(this, "inputTank");
        this.getNetwork().updateTileGuiField(this, "outputTank");
    }

    @Override
    public boolean checkRecipe(FluidCanningRecipe entry) {
        if (!entry.matches(inventory.get(slotInput), inputTank.getFluid().getFluid()) && inputTank.getFluidAmount() < entry.getInputFluidAmount()) {
            return false;
        }
        return true;
    }

    @Override
    public void process(FluidCanningRecipe recipe) {
        if (recipe.hasItemOutput()){
            for (ItemStack stack : recipe.getOutputs().getRecipeOutput(getWorld().rand, getTileData())) {
                outputs.add(new MultiSlotOutput(stack, getOutputSlots()));
            }
        }


        IRecipeInput input = recipe.getInput();
        ItemStack stack = inventory.get(slotInput);
        if (stack.getItem().hasContainerItem(stack)) {
            inventory.set(slotInput, stack.getItem().getContainerItem(stack));
        } else {
            stack.shrink(input.getAmount());
        }
        this.inputTank.drain(recipe.getInputFluidAmount(), true);
        if (recipe.hasFluidOutput()){
            this.outputTank.fill(new FluidStack(recipe.getOutputFluid(), recipe.getOutputFluidAmount()), true);
        }
        addToInventory();
        for (int i = 0; i < upgradeSlots; i++) {
            ItemStack item = inventory.get(i + inventory.size() - upgradeSlots);
            if (item.getItem() instanceof IMachineUpgradeItem) {
                ((IMachineUpgradeItem) item.getItem()).onProcessFinished(item, this);
            }
        }
    }

    @Override
    protected FluidCanningRecipe getRecipe() {
        if (inventory.get(slotInput).isEmpty() || inputTank.getFluidAmount() == 0) {
            return null;
        }
        if (lastRecipe == FluidCanningRecipeList.INVALID_RECIPE) {
            return null;
        }
        if (lastRecipe != null) {
            if (!checkRecipe(lastRecipe)) {
                lastRecipe = null;
                applyRecipeEffect(null);
            }
        }
        if (lastRecipe == null) {
            lastRecipe = getRecipeList().getRecipe(new Predicate<FluidCanningRecipe>() {
                @Override
                public boolean test(FluidCanningRecipe t) {
                    if (checkRecipe(t)) {
                        return true;
                    }
                    return false;
                }
            });
            if (lastRecipe == FluidCanningRecipeList.INVALID_RECIPE) {
                return null;
            }
            applyRecipeEffect(lastRecipe.getOutputs());
        }
        if (lastRecipe == null) {
            return null;
        }
        if (getStackInSlot(slotOutput).isEmpty()){
            return lastRecipe;
        }
        if (lastRecipe.hasItemOutput()){
            for (ItemStack output : lastRecipe.getOutputs().getAllOutputs()) {
                if (StackUtil.isStackEqual(inventory.get(slotOutput), output, false, true)) {
                    if (inventory.get(slotOutput).getCount() + output.getCount() <= inventory.get(slotOutput)
                            .getMaxStackSize()) {
                        return lastRecipe;
                    }
                }
            }
        }
        if (lastRecipe.hasFluidOutput()){
            if (outputTank.getFluid().getFluid().equals(lastRecipe.getOutputFluid())){
                if (outputTank.getFluidAmount() + lastRecipe.getOutputFluidAmount() <= outputTank.getCapacity()){
                    return lastRecipe;
                }
            }
        }

        return null;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        this.inputTank.readFromNBT(nbt.getCompoundTag("inputTank"));
        this.outputTank.readFromNBT(nbt.getCompoundTag("outputTank"));
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);
        this.inputTank.writeToNBT(this.getTag(nbt, "inputTank"));
        this.outputTank.writeToNBT(this.getTag(nbt, "outputTank"));
        return nbt;
    }

    @Override
    public int[] getInputSlots() {
        return new int[]{slotInput};
    }

    @Override
    public IFilter[] getInputFilters(int[] slots) {
        return new IFilter[0];
    }

    @Override
    public boolean isRecipeSlot(int slot) {
        return true;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{slotOutput};
    }
    @Override
    public FluidCanningRecipeList getRecipeList() {
        return fluidCanning;
    }

    @Override
    public Set<IMachineUpgradeItem.UpgradeType> getSupportedTypes() {
        return new LinkedHashSet(Arrays.asList(IMachineUpgradeItem.UpgradeType.values()));
    }

    @Override
    public ContainerIC2 getGuiContainer(EntityPlayer player)
    {
        return new ContainerFluidCanningMachine(player.inventory, this);
    }

    @Override
    public Class<? extends GuiScreen> getGuiClass(EntityPlayer player)
    {
        return GuiMachine.FluidCanningGui.class;
    }


    @Override
    public LocaleComp getBlockName()
    {
        return Ic2cExtrasLang.metalBender;
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing)
    {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing)
    {
        return capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY ? (T)this.inputTank : super.getCapability(capability, facing);
    }

    @Override
    public double getWrenchDropRate() {
        return 1.0D;
    }

    public ResourceLocation getGuiTexture()
    {
        return Ic2cExtrasResourceLocations.fluidCanningMachine;
    }

    public static void addRecipe(IRecipeInput input, Fluid inputFluid, int inputFluidAmount,  ItemStack output)
    {
        addRecipe(input, inputFluid, inputFluidAmount,  new MachineOutput(null, output));
    }

    public static void addRecipe(IRecipeInput input, Fluid inputFluid, int inputFluidAmount, Fluid outputFluid, int outputFluidAmount,  ItemStack output)
    {
        addRecipe(input, inputFluid, inputFluidAmount, outputFluid, outputFluidAmount,  new MachineOutput(null, output));
    }

    public static void addRecipe(IRecipeInput input, Fluid inputFluid, int inputFluidAmount, MachineOutput output)
    {
        fluidCanning.addRecipe(input, inputFluid, inputFluidAmount,  output, output.getAllOutputs().get(0).getDisplayName());
    }

    public static void addRecipe(IRecipeInput input, Fluid inputFluid, int inputFluidAmount, Fluid outputFluid, int outputFluidAmount, MachineOutput output)
    {
        fluidCanning.addRecipe(input, inputFluid, inputFluidAmount,  output, outputFluid, outputFluidAmount, output.getAllOutputs().get(0).getDisplayName());
    }

    public static void addRecipe(IRecipeInput input, Fluid inputFluid, int inputFluidAmount, Fluid outputFluid, int outputFluidAmount)
    {
        fluidCanning.addRecipe(input, inputFluid, inputFluidAmount,  outputFluid, outputFluidAmount, outputFluid.getName());
    }
}
