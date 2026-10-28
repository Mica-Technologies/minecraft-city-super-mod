package com.micatechnologies.minecraft.csm.furniture.market.pawn;

import com.micatechnologies.minecraft.csm.furniture.market.TileEntityDisplayCase;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * A pawn counter: the display case's 27 slots, which hold its resale stock, and whether it
 * resells at all. With resale off (the default) what it buys is destroyed and the slots stay
 * empty.
 *
 * <p>Nothing outside the counter's own screens reaches the stock: it offers no item handler, so a
 * hopper neither feeds nor drains it. Its stock drops when an admin breaks it.</p>
 *
 * @since 2026.10
 */
public class TileEntityPawnCounter extends TileEntityDisplayCase {

  private static final String KEY_RESALE = "pr";

  private boolean resale;

  public TileEntityPawnCounter() {
    super(27);
  }

  /** Whether it keeps what it buys and sells it back. */
  public boolean isResale() {
    return resale;
  }

  public void setResale(boolean resale) {
    this.resale = resale;
    markDirty();
  }

  /**
   * Puts a pawned stack into the resale stock.
   *
   * @return what did not fit (destroyed by the caller)
   */
  public ItemStack keep(ItemStack stack) {
    return ItemHandlerHelper.insertItemStacked(getItems(), stack, false);
  }

  @Override
  public boolean accepts(@Nonnull ItemStack stack) {
    return true;
  }

  @Override
  public void readNBT(NBTTagCompound compound) {
    super.readNBT(compound);
    resale = compound.getBoolean(KEY_RESALE);
  }

  @Override
  public NBTTagCompound writeNBT(NBTTagCompound compound) {
    super.writeNBT(compound);
    compound.setBoolean(KEY_RESALE, resale);
    return compound;
  }

  @Override
  public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
    return capability != CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
        && super.hasCapability(capability, facing);
  }

  @Nullable
  @Override
  public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
    return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY ? null
        : super.getCapability(capability, facing);
  }
}
