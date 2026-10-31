package com.micatechnologies.minecraft.csm.venues.arena;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An event floor block whose top is chosen by its metadata: plain rink ice and its paints, the
 * rubber matting, the ice lines, maple in four finishes and the court lines. The whole metadata
 * is one {@code variant} property, packed as the block's {@link Orient} says:
 *
 * <ul>
 *   <li>{@link Orient#NONE}: the metadata is the kind;</li>
 *   <li>{@link Orient#AXIS}: {@code kind * 2 + axis}, axis 0 running along x (east-west) and 1
 *   along z (north-south);</li>
 *   <li>{@link Orient#FACING}: {@code facing | kind << 2}, the facing in bits 0-1 as every
 *   rotatable block keeps it (S 0, W 1, N 2, E 3).</li>
 * </ul>
 *
 * <p>Placed by hand, a line or a board runs the way the placer looks, and a court line's edge is
 * on the side they look towards. A sneak-click with an empty hand steps the kind.</p>
 *
 * @since 2026.10
 */
public class BlockArenaFloor extends BlockArenaFloorBase {

  /** How a block's metadata holds its orientation. */
  public enum Orient {
    /** No orientation. */
    NONE(1),
    /** An axis, bit 0. */
    AXIS(2),
    /** A horizontal facing, bits 0-1. */
    FACING(4);

    final int count;

    Orient(int count) {
      this.count = count;
    }
  }

  private PropertyInteger variant;
  private final int kinds;
  private final Orient orient;

  /**
   * Constructs a floor block.
   *
   * @param registryName its registry name
   * @param kinds        how many kinds it has
   * @param orient       how it is turned
   * @param surface      what it is made of
   */
  public BlockArenaFloor(String registryName, int kinds, Orient orient, Surface surface) {
    super(registryName, surface, kinds * orient.count);
    this.kinds = kinds;
    this.orient = orient;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    int n = (Integer) pendingArg();
    if (n <= 1) {
      return new CsmBlockStateContainer(this, new IProperty<?>[0]);
    }
    variant = PropertyInteger.create("variant", 0, n - 1);
    return new CsmBlockStateContainer(this, variant);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    if (variant == null) {
      return getDefaultState();
    }
    int n = kinds * orient.count;
    return getDefaultState().withProperty(variant, meta >= 0 && meta < n ? meta : 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return variant == null ? 0 : state.getValue(variant);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing look = placer.getHorizontalFacing();
    switch (orient) {
      case AXIS:
        return getStateFromMeta(look.getAxis() == EnumFacing.Axis.Z ? 1 : 0);
      case FACING:
        return getStateFromMeta(look.getHorizontalIndex());
      default:
        return getDefaultState();
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (variant == null || kinds < 2 || !player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int meta = state.getValue(variant);
      int o = meta % orient.count;
      int k = (meta / orient.count + 1) % kinds;
      if (orient == Orient.FACING) {
        o = meta & 3;
        k = ((meta >> 2) + 1) % kinds;
      }
      world.setBlockState(pos, getStateFromMeta(k * orient.count + o), 3);
    }
    return true;
  }
}
