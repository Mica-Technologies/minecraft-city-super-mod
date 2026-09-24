package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The dishwasher: a built-in appliance under the countertop of a base run, which repairs tools
 * and weapons a little each wash. Its countertop is the one of the cabinet beside it (granite
 * beside oak, quartz beside walnut or white), so the counter runs on over it unbroken; on its own
 * it has its own stainless or white top ({@link #COUNTER}, actual state).
 *
 * @since 2026.9
 */
public class BlockDishwasher extends BlockBuiltInAppliance {

  /** The countertop drawn over it. */
  public static final PropertyEnum<KitchenCountertop> COUNTER =
      PropertyEnum.create("counter", KitchenCountertop.class);

  private static final int[] BOX = {0, 0, 0, 16, 15, 16};

  /**
   * Constructs a dishwasher.
   *
   * @param registryName its registry name, ending in its finish
   * @param spec         what it does
   */
  public BlockDishwasher(String registryName, ApplianceSpec spec) {
    super(registryName, BOX, spec, KitchenLine.BASE);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, RUNNING, COUNTER);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    EnumFacing f = s.getValue(FACING);
    KitchenCountertop top = countertop(world, pos.offset(f.rotateYCCW()));
    if (top == KitchenCountertop.NONE) {
      top = countertop(world, pos.offset(f.rotateY()));
    }
    return s.withProperty(COUNTER, top);
  }

  /** The countertop of the base cabinet at {@code pos}, or none if there is none there. */
  private static KitchenCountertop countertop(IBlockAccess world, BlockPos pos) {
    IBlockState other = world.getBlockState(pos);
    if (other.getBlock() instanceof BlockKitchenCabinet) {
      BlockKitchenCabinet cabinet = (BlockKitchenCabinet) other.getBlock();
      if (cabinet.getLine() == KitchenLine.BASE) {
        return cabinet.getCountertop();
      }
    }
    return KitchenCountertop.NONE;
  }
}
