package com.micatechnologies.minecraft.csm.furniture.appliance;

import com.micatechnologies.minecraft.csm.codeutils.AbstractTileEntity;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A working appliance: an input, an output and (for one that burns it) a fuel slot, a cycle
 * timer, and for one that uses water a tank. Whatever it is -- oven, toaster, dishwasher,
 * washing machine -- is decided by its block's {@link ApplianceSpec}; this class is the machine
 * they all share.
 *
 * <p>Every tick on the server: if the input holds enough of something its recipe book takes,
 * there is room in the output for what that makes, and there is water (and fuel burning) if it
 * needs them (and its supply, for one that uses one up), it works, advancing the cycle; when the
 * cycle completes it takes the input (unless its recipe keeps it) and one supply, puts
 * the result in the output (or back into the input, for a repair not yet finished), uses a unit
 * of water, and plays its done sound. Take the input away and the cycle starts again from
 * nothing. While it works its block's {@link IAppliance#RUNNING} is true.</p>
 *
 * <p>Hoppers and pipes reach it through the item handler capability on every side: they can put
 * into the input and fuel slots what those take, and take only from the output. The contents
 * never go to clients in the block's sync; the screen's container sends them to the player
 * looking, and the cycle and the tank as window properties.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class TileEntityAppliance extends AbstractTileEntity implements ITickable {

  private static final String KEY_ITEMS = "i";
  private static final String KEY_PROGRESS = "p";
  private static final String KEY_WATER = "w";
  private static final String KEY_BURN = "b";
  private static final String KEY_BURN_MAX = "bm";

  private final ApplianceInventory items = new ApplianceInventory(this::spec) {
    @Override
    protected void onContentsChanged(int slot) {
      markDirty();
      recipeStale = true;
    }
  };

  /** What hoppers see: in through the input and fuel slots, out only from the output. */
  private final IItemHandler automation = new IItemHandler() {
    @Override
    public int getSlots() {
      return items.getSlots();
    }

    @Nonnull
    @Override
    public ItemStack getStackInSlot(int slot) {
      return items.getStackInSlot(slot);
    }

    @Nonnull
    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
      return slot == ApplianceInventory.OUTPUT ? stack : items.insertItem(slot, stack, simulate);
    }

    @Nonnull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
      return slot == ApplianceInventory.OUTPUT ? items.extractItem(slot, amount, simulate)
          : ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
      return items.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
      return items.isItemValid(slot, stack);
    }
  };

  private int progress;
  private int water;
  private int burn;
  private int burnMax;

  // Worked out again whenever the slots change; never saved.
  private transient boolean recipeStale = true;
  @Nullable
  private transient ApplianceRecipe recipe;
  private transient ItemStack pending = ItemStack.EMPTY;
  private transient int total;

  /**
   * The appliance's spec, from its block, or null if the block is not (or no longer) one.
   *
   * @return the spec
   */
  @Nullable
  public ApplianceSpec spec() {
    if (world == null) {
      return null;
    }
    Block block = world.getBlockState(pos).getBlock();
    return block instanceof IAppliance ? ((IAppliance) block).getApplianceSpec() : null;
  }

  /**
   * The slots.
   *
   * @return the inventory
   */
  public ApplianceInventory getItems() {
    return items;
  }

  /**
   * How far through the current cycle it is, in ticks.
   *
   * @return the progress
   */
  public int getProgress() {
    return progress;
  }

  /**
   * How long the current cycle takes, in ticks (0 while idle).
   *
   * @return the cycle's length
   */
  public int getTotal() {
    return total;
  }

  /**
   * How many cycles' water is in the tank.
   *
   * @return the water
   */
  public int getWater() {
    return water;
  }

  public int getBurn() {
    return burn;
  }

  public int getBurnMax() {
    return burnMax;
  }

  /**
   * Adds water to the tank, up to its capacity.
   *
   * @param cycles how many cycles' water
   *
   * @return true if any went in
   */
  public boolean addWater(int cycles) {
    ApplianceSpec spec = spec();
    if (spec == null || !spec.usesWater() || water >= spec.getWaterCapacity()) {
      return false;
    }
    water = Math.min(spec.getWaterCapacity(), water + cycles);
    markDirty();
    return true;
  }

  @Override
  public void update() {
    if (world == null || world.isRemote) {
      return;
    }
    ApplianceSpec spec = spec();
    if (spec == null) {
      return;
    }
    if (spec.usesWater() && water < spec.getWaterCapacity()
        && Math.floorMod(world.getTotalWorldTime() + pos.hashCode(), 20L) == 0
        && spec.isPlumbed(world, pos)) {
      water = spec.getWaterCapacity();
      markDirty();
    }
    if (recipeStale) {
      findRecipe(spec);
    }
    ItemStack in = items.getStackInSlot(ApplianceInventory.INPUT);
    boolean ready = recipe != null && !pending.isEmpty()
        && in.getCount() >= recipe.getInputCount(in) && fits(pending)
        && (!spec.usesWater() || water > 0)
        && (!spec.usesSupply()
        || !items.getStackInSlot(ApplianceInventory.FUEL).isEmpty());
    if (burn > 0) {
      burn--;
      if (burn == 0) {
        markDirty();
      }
    }
    if (ready && spec.usesFuel() && burn == 0) {
      burnFuel();
    }
    boolean running = ready && (!spec.usesFuel() || burn > 0);
    if (running) {
      if (progress % spec.getRunSoundEvery() == 0) {
        play(spec.getRunSound(), spec.getRunVolume(), spec.getRunPitch());
      }
      if (++progress >= total) {
        finishCycle(spec, in);
      }
    } else if (progress != 0) {
      progress = 0;
      markDirty();
    }
    setRunning(spec.usesFuel() ? burn > 0 : running);
  }

  private void findRecipe(ApplianceSpec spec) {
    recipeStale = false;
    ItemStack in = items.getStackInSlot(ApplianceInventory.INPUT);
    recipe = spec.getBook().find(in);
    pending = recipe == null ? ItemStack.EMPTY : recipe.getResult(in);
    total = recipe == null ? 0
        : Math.max(1, Math.round(recipe.getTicks(in) * spec.getTimeFactor()));
  }

  /** Whether {@code result} has somewhere to go. */
  private boolean fits(ItemStack result) {
    if (recipe != null && recipe.staysInInput(result)) {
      return true;
    }
    ItemStack out = items.getStackInSlot(ApplianceInventory.OUTPUT);
    return out.isEmpty() || (ItemHandlerHelper.canItemStacksStack(out, result)
        && out.getCount() + result.getCount() <= out.getMaxStackSize());
  }

  private void burnFuel() {
    ItemStack fuel = items.getStackInSlot(ApplianceInventory.FUEL);
    int time = TileEntityFurnace.getItemBurnTime(fuel);
    if (time <= 0) {
      return;
    }
    burn = burnMax = time;
    ItemStack left = fuel.getItem().getContainerItem(fuel);
    if (fuel.getCount() == 1 && !left.isEmpty()) {
      items.setStackInSlot(ApplianceInventory.FUEL, left);
    } else {
      items.extractItem(ApplianceInventory.FUEL, 1, false);
    }
  }

  private void finishCycle(ApplianceSpec spec, ItemStack in) {
    ApplianceRecipe done = recipe;
    ItemStack result = pending.copy();
    progress = 0;
    if (spec.usesWater()) {
      water--;
    }
    if (done != null && done.staysInInput(result)) {
      items.setStackInSlot(ApplianceInventory.INPUT, result);
      markDirty();
      return;
    }
    if (spec.usesSupply()) {
      items.extractItem(ApplianceInventory.FUEL, 1, false);
    }
    if (done == null || done.consumesInput()) {
      items.extractItem(ApplianceInventory.INPUT, done == null ? 1 : done.getInputCount(in),
          false);
    }
    ItemStack out = items.getStackInSlot(ApplianceInventory.OUTPUT);
    if (out.isEmpty()) {
      items.setStackInSlot(ApplianceInventory.OUTPUT, result);
    } else {
      ItemStack grown = out.copy();
      grown.grow(result.getCount());
      items.setStackInSlot(ApplianceInventory.OUTPUT, grown);
    }
    play(spec.getDoneSound(), 0.8F, spec.getDonePitch());
  }

  private void play(@Nullable ICsmSound sound, float volume, float pitch) {
    SoundEvent event = sound == null ? null : sound.getSoundEvent();
    if (event != null) {
      world.playSound(null, pos, event, SoundCategory.BLOCKS, volume, pitch);
    }
  }

  /** Sets the block's {@link IAppliance#RUNNING}, if it has one and it differs. */
  private void setRunning(boolean running) {
    IBlockState state = world.getBlockState(pos);
    if (state.getPropertyKeys().contains(IAppliance.RUNNING)
        && state.getValue(IAppliance.RUNNING) != running) {
      world.setBlockState(pos, state.withProperty(IAppliance.RUNNING, running), 3);
    }
  }

  /**
   * Calls {@code action} with each stack held, for dropping them when the block is broken.
   *
   * @param action what to do with each
   */
  public void forEachItem(java.util.function.Consumer<ItemStack> action) {
    for (int s = 0; s < items.getSlots(); s++) {
      ItemStack st = items.getStackInSlot(s);
      if (!st.isEmpty()) {
        action.accept(st);
      }
    }
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    if (compound.hasKey(KEY_ITEMS)) {
      items.deserializeNBT(compound.getCompoundTag(KEY_ITEMS));
    }
    progress = compound.getInteger(KEY_PROGRESS);
    water = compound.getInteger(KEY_WATER);
    burn = compound.getInteger(KEY_BURN);
    burnMax = compound.getInteger(KEY_BURN_MAX);
    recipeStale = true;
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    compound.setTag(KEY_ITEMS, items.serializeNBT());
    compound.setInteger(KEY_PROGRESS, progress);
    compound.setInteger(KEY_WATER, water);
    compound.setInteger(KEY_BURN, burn);
    compound.setInteger(KEY_BURN_MAX, burnMax);
    return compound;
  }

  /** The block's sync to clients: its position only, never the contents. */
  @Override
  @Nonnull
  public NBTTagCompound getUpdateTag() {
    NBTTagCompound tag = new NBTTagCompound();
    tag.setInteger("x", pos.getX());
    tag.setInteger("y", pos.getY());
    tag.setInteger("z", pos.getZ());
    return tag;
  }

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        || super.hasCapability(capability, facing);
  }

  @Override
  @Nullable
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
      return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(automation);
    }
    return super.getCapability(capability, facing);
  }
}
