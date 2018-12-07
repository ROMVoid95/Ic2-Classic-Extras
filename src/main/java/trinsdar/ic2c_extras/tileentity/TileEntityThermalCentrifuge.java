package trinsdar.ic2c_extras.tileentity;

import ic2.api.classic.item.IMachineUpgradeItem;
import ic2.api.classic.recipe.INullableRecipeInput;
import ic2.api.classic.recipe.machine.IMachineRecipeList;
import ic2.api.classic.recipe.machine.MachineOutput;
import ic2.api.classic.tile.MachineType;
import ic2.api.recipe.IRecipeInput;
import ic2.core.RotationList;
import ic2.core.block.base.tile.TileEntityAdvancedMachine;
import ic2.core.block.machine.recipes.managers.BasicMachineRecipeList;
import ic2.core.inventory.container.ContainerIC2;
import ic2.core.inventory.filters.*;
import ic2.core.inventory.management.AccessRule;
import ic2.core.inventory.management.InventoryHandler;
import ic2.core.inventory.management.SlotType;
import ic2.core.inventory.slots.SlotCustom;
import ic2.core.inventory.slots.SlotDischarge;
import ic2.core.inventory.slots.SlotOutput;
import ic2.core.inventory.slots.SlotUpgrade;
import ic2.core.platform.lang.components.base.LangComponentHolder;
import ic2.core.platform.lang.components.base.LocaleComp;
import ic2.core.platform.registry.Ic2Items;
import ic2.core.platform.registry.Ic2Sounds;
import ic2.core.util.helpers.FilteredList;
import ic2.core.util.math.Box2D;
import ic2.core.util.misc.StackUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import trinsdar.ic2c_extras.container.ContainerThermalCentrifuge;

import java.util.*;

public class TileEntityThermalCentrifuge extends TileEntityAdvancedMachine
{
    public TileEntityThermalCentrifuge() {
        super( 9, 48, 400);
        this.setCustomName("tileThermalCentrifuge");
    }

    @Override
    public IMachineRecipeList.RecipeEntry getOutputFor(ItemStack input) {
        return thermalCentrifuge.getRecipeInAndOutput(input, false);
    }

    public int progressPerTick;

    public static IMachineRecipeList thermalCentrifuge = new BasicMachineRecipeList("thermalCentrifuge");

    public static final int slotFuel = 0;
    public static final int slotInput = 1;
    public static final int slotOutput = 2;
    public static final int slotOutput2 = 3;
    public static final int slotOutput3 = 4;

    @Override
    public int[] getOutputSlots() {
        return new int[]{slotOutput, slotOutput2, slotOutput3};
    }

    @Override
    protected void addSlots(InventoryHandler handler)
    {
        handler.registerDefaultSideAccess(AccessRule.Both, RotationList.ALL);
        handler.registerDefaultSlotAccess(AccessRule.Both, slotFuel);
        handler.registerDefaultSlotAccess(AccessRule.Import, slotInput);
        handler.registerDefaultSlotAccess(AccessRule.Export, slotOutput, slotOutput2, slotOutput3);
        handler.registerDefaultSlotsForSide(RotationList.UP.getOppositeList(), 0, 2, 4);
        handler.registerDefaultSlotsForSide(RotationList.DOWN.getOppositeList(), 1, 3);
        handler.registerInputFilter(new ArrayFilter(CommonFilters.DischargeEU, new BasicItemFilter(Items.REDSTONE), new BasicItemFilter(Ic2Items.suBattery)), slotFuel);
        handler.registerOutputFilter(CommonFilters.NotDischargeEU, slotFuel);
        handler.registerSlotType(SlotType.Fuel, slotFuel);
        handler.registerSlotType(SlotType.Input, slotInput);
        handler.registerSlotType(SlotType.Output, slotOutput, slotOutput2, slotOutput3);
    }

    @Override
    public Slot[] getInventorySlots(InventoryPlayer player) {
        Slot[] slots = new Slot[]{new SlotDischarge(this, 2147483647, 0, 11, 53), new SlotCustom(this, 1, 11, 17, new MachineFilter(this)), new SlotOutput(player.player, this, 2, 113, 13), new SlotOutput(player.player, this, 3, 113, 36), new SlotOutput(player.player, this, 4, 113, 59), new SlotUpgrade(this, 5, 152, 44), new SlotUpgrade(this, 6, 152, 62)};
        return slots;
    }

    @Override
    public Box2D getChargeBox() {
        return ContainerThermalCentrifuge.machineChargeBox;
    }

    @Override
    public Box2D getProgressBox() {
        return ContainerThermalCentrifuge.machineProgressBox;
    }

    public ResourceLocation getStartSoundFile()
    {
        return Ic2Sounds.maceratorOp;
    }

    public ResourceLocation getInterruptSoundFile()
    {
        return Ic2Sounds.interruptingSound;
    }


    @Override
    public ResourceLocation getTexture() {
        return new ResourceLocation("ic2c_extras", "textures/guiSprites/GUIThermalCentrifuge.png");
    }

    public ContainerIC2 getGuiContainer(EntityPlayer player) {
        return new ContainerThermalCentrifuge(player.inventory, this);
    }

    @Override
    public LocaleComp getSpeedName() {
        return new LangComponentHolder.LocaleGuiComp("container.machineHeat.name");
    }

    @Override
    public IMachineRecipeList getRecipeList() {
        return thermalCentrifuge;
    }

    @Override
    public MachineType getType() {
        return MachineType.macerator;
    }

    @Override
    public IMachineRecipeList.RecipeEntry getRecipe()
    {
        if (this.inventory.get(slotInput).isEmpty() && !this.canWorkWithoutItems())
        {
            return null;
        }
        else
        {
            if (this.lastRecipe != null)
            {
                IRecipeInput recipe = this.lastRecipe.getInput();
                if (recipe instanceof INullableRecipeInput)
                {
                    if (!recipe.matches(this.inventory.get(slotInput)))
                    {
                        this.lastRecipe = null;
                    }
                }
                else if (!this.inventory.get(slotInput).isEmpty() && recipe.matches(this.inventory.get(0)))
                {
                    if (recipe.getAmount() > this.inventory.get(slotInput).getCount())
                    {
                        return null;
                    }
                }
                else
                {
                    this.lastRecipe = null;
                }
            }

            if (this.lastRecipe == null)
            {
                IMachineRecipeList.RecipeEntry out = this.getOutputFor(this.inventory.get(slotInput).copy());
                if (out == null)
                {
                    return null;
                }

                this.lastRecipe = out;
                this.handleModifiers(out);
            }

            if (this.lastRecipe == null)
            {
                return null;
            }
            else if (this.inventory.get(slotOutput).getCount() >= this.inventory.get(slotOutput).getMaxStackSize())
            {
                return null;
            }
            else if (this.inventory.get(slotOutput2).getCount() >= this.inventory.get(slotOutput2).getMaxStackSize())
            {
                return null;
            }
            else if (this.inventory.get(slotOutput3).getCount() >= this.inventory.get(slotOutput3).getMaxStackSize())
            {
                return null;
            }
            else if (this.inventory.get(slotOutput).isEmpty())
            {
                return this.lastRecipe;
            }
            else
            {
                Iterator var4 = this.lastRecipe.getOutput().getAllOutputs().iterator();

                ItemStack output;
                do
                {
                    if (!var4.hasNext())
                    {
                        return null;
                    }

                    output = (ItemStack) var4.next();
                }
                while (!StackUtil.isStackEqual(this.inventory.get(slotOutput), output, false, true));

                return this.lastRecipe;
            }
        }
    }

    @Override
    public void update() {
        this.handleChargeSlot(500);
        this.updateNeighbors();

        boolean noRoom = this.addToInventory();
        IMachineRecipeList.RecipeEntry entry = this.getRecipe();
        boolean canWork = this.canWork() && !noRoom;
        boolean operate = canWork && entry != null;
        if (operate)
        {
            this.handleChargeSlot(this.maxEnergy);
        }

        if (operate && this.energy >= this.energyConsume)
        {
            if (!this.getActive())
            {
                this.getNetwork().initiateTileEntityEvent(this, 0, false);
            }

            this.setActive(true);
            this.progress += this.progressPerTick;
            this.useEnergy(this.recipeEnergy);
            if (this.progress >= (float) this.recipeOperation)
            {
                this.operate(0, entry);
                this.progress = 0;
                this.notifyNeighbors();
            }

            this.getNetwork().updateTileGuiField(this, "progress");
        }
        else
        {
            if (this.getActive())
            {
                if (this.progress != 0)
                {
                    this.getNetwork().initiateTileEntityEvent(this, 1, false);
                }
                else
                {
                    this.getNetwork().initiateTileEntityEvent(this, 2, false);
                }
            }

            if (entry == null && this.progress != 0)
            {
                this.progress = 0;
                this.getNetwork().updateTileGuiField(this, "progress");
            }

            this.setActive(false);
        }

        for (int i = 0; i < 4; ++i)
        {
            ItemStack item = this.inventory.get(i + this.inventory.size() - 4);
            if (item.getItem() instanceof IMachineUpgradeItem)
            {
                ((IMachineUpgradeItem) item.getItem()).onTick(item, this);
            }
        }

        this.updateComparators();
    }

    @Override
    public void operateOnce(int slot, IRecipeInput input, MachineOutput output, List<ItemStack> list)
    {
        list.addAll(output.getRecipeOutput(this.getMachineWorld().rand, getTileData()));
        if (!(input instanceof INullableRecipeInput) || !this.inventory.get(slotInput).isEmpty())
        {
            if (this.inventory.get(slotInput).getItem().hasContainerItem(this.inventory.get(slotInput)))
            {
                this.inventory.set(slotInput, this.inventory.get(slotInput).getItem().getContainerItem(this.inventory.get(slotInput)));
            }
            else
            {
                this.inventory.get(slotInput).shrink(input.getAmount());
            }

        }
    }

    @Override
    public void operate(int slot, IMachineRecipeList.RecipeEntry entry) {

        IRecipeInput input = entry.getInput();
        MachineOutput output = entry.getOutput().copy();

        for (int i = 0; i < 4; ++i)
        {
            ItemStack itemStack = this.inventory.get(i + this.inventory.size() - 4);
            if (itemStack.getItem() instanceof IMachineUpgradeItem)
            {
                IMachineUpgradeItem item = (IMachineUpgradeItem) itemStack.getItem();
                item.onProcessEndPre(itemStack, this, output);
            }
        }

        List<ItemStack> list = new FilteredList();
        this.operateOnce(slot, input, output, list);

        for (int i = 0; i < 4; ++i)
        {
            ItemStack itemStack = this.inventory.get(i + this.inventory.size() - 4);
            if (itemStack.getItem() instanceof IMachineUpgradeItem)
            {
                IMachineUpgradeItem item = (IMachineUpgradeItem) itemStack.getItem();
                item.onProcessEndPost(itemStack, this, input, output, list);
            }
        }

        if (list.size() > 0)
        {
            this.results.addAll(list);
            this.addToInventory();
        }
    }

    @Override
    public boolean addToInventory()
    {
        if (this.results.isEmpty())
        {
            return false;
        }
        else
        {
            for (int i = 0; i < this.results.size(); ++i)
            {
                ItemStack item = this.results.get(i);
                if (item.isEmpty())
                {
                    this.results.remove(i--);
                }
                else if (this.inventory.get(slotOutput).isEmpty())
                {
                    this.inventory.set(slotOutput, item.copy());
                    this.results.remove(i--);
                }
                else if (StackUtil.isStackEqual(this.inventory.get(slotOutput), item, false, false))
                {
                    int left = this.inventory.get(slotOutput).getMaxStackSize() - this.inventory.get(slotOutput).getCount();
                    if (left <= 0)
                    {
                        break;
                    }

                    if (left < item.getCount())
                    {
                        int itemLeft = item.getCount() - left;
                        item.setCount(itemLeft);
                        this.inventory.get(slotOutput).setCount(this.inventory.get(slotOutput).getMaxStackSize());
                        break;
                    }

                    this.inventory.get(slotOutput).grow(item.getCount());
                    this.results.remove(i--);
                }
                else if (this.inventory.get(slotOutput2).isEmpty())
                {
                    this.inventory.set(slotOutput2, item.copy());
                    this.results.remove(i--);
                }
                else if (StackUtil.isStackEqual(this.inventory.get(slotOutput2), item, false, false))
                {
                    int left = this.inventory.get(slotOutput2).getMaxStackSize() - this.inventory.get(slotOutput2).getCount();
                    if (left <= 0)
                    {
                        break;
                    }

                    if (left < item.getCount())
                    {
                        int itemLeft = item.getCount() - left;
                        item.setCount(itemLeft);
                        this.inventory.get(slotOutput2).setCount(this.inventory.get(slotOutput2).getMaxStackSize());
                        break;
                    }

                    this.inventory.get(slotOutput2).grow(item.getCount());
                    this.results.remove(i--);
                }
                else if (this.inventory.get(slotOutput3).isEmpty())
                {
                    this.inventory.set(slotOutput3, item.copy());
                    this.results.remove(i--);
                }
                else if (StackUtil.isStackEqual(this.inventory.get(slotOutput3), item, false, false))
                {
                    int left = this.inventory.get(slotOutput3).getMaxStackSize() - this.inventory.get(slotOutput3).getCount();
                    if (left <= 0)
                    {
                        break;
                    }

                    if (left < item.getCount())
                    {
                        int itemLeft = item.getCount() - left;
                        item.setCount(itemLeft);
                        this.inventory.get(slotOutput3).setCount(this.inventory.get(slotOutput3).getMaxStackSize());
                        break;
                    }

                    this.inventory.get(slotOutput3).grow(item.getCount());
                    this.results.remove(i--);
                }
            }
            return this.results.size() > 0;
        }

    }


    public static void init() { //recipes in recipes class now
    }

    public static void addRecipe(IRecipeInput input, MachineOutput output, String id)
    {
        thermalCentrifuge.addRecipe(input, output, id);
    }

    private static String makeString(ItemStack stack)
    {
        return stack.getDisplayName();
    }
}
