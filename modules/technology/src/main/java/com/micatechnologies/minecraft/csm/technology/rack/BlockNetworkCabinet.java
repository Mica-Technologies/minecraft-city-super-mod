package com.micatechnologies.minecraft.csm.technology.rack;

import com.micatechnologies.minecraft.csm.codeutils.CsmExtendedBlockState;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * The 12U wall-mount network cabinet: one block, hung on the wall behind it at head height,
 * with a glass door. It takes the same units as the 42U racks at the same scale, but it is only
 * 400 mm deep, so only the shallow ones fit ({@link RackFrame#WALL_12U}): the switches, patch
 * panel, firewall, gateway, PDU, cable manager and blanking plate.
 *
 * @since 2026.10
 */
public class BlockNetworkCabinet extends AbstractBlockRack {

  private static final AxisAlignedBB BOX =
      new AxisAlignedBB(0, 3 / 16.0, 8 / 16.0, 1, 13 / 16.0, 1);

  /**
   * @param registryName its registry name
   * @param frame        {@link RackFrame#WALL_12U}
   */
  public BlockNetworkCabinet(String registryName, RackFrame frame) {
    super(registryName, frame, true);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmExtendedBlockState(this, new IProperty[]{FACING},
        new IUnlistedProperty[]{CONTENTS});
  }

  @Override
  public int cellOf(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    dropContents(world, pos);
    super.breakBlock(world, pos, state);
  }
}
