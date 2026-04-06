package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalBodyTilt;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Abstract base class for traffic signal backplate blocks. Adds a computed TILT property
 * that mirrors the tilt setting of the signal head behind the backplate. The TILT property
 * is not stored in metadata — it is computed each render frame via {@link #getActualState}
 * by reading the {@link TileEntityTrafficSignalHead} in the block behind this backplate.
 * This requires no tile entity on the backplate itself.
 */
public abstract class AbstractBlockSignalBackplate extends AbstractBlockRotatableNSEWUD {

  /**
   * The tilt property, matching {@link TrafficSignalBodyTilt} values. Computed in
   * {@link #getActualState} — not stored in block metadata.
   */
  public static final PropertyEnum<TrafficSignalBodyTilt> TILT =
      PropertyEnum.create("tilt", TrafficSignalBodyTilt.class);

  public AbstractBlockSignalBackplate(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
        lightLevel, lightOpacity, false);
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(TILT, TrafficSignalBodyTilt.NONE));
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, TILT);
  }

  /**
   * Computes the actual tilt state by reading the signal head behind this backplate.
   * "Behind" is determined by the backplate's facing: the signal is one block in the
   * opposite direction of FACING (since the backplate faces the viewer, the signal is
   * behind it). Only checks the two positions along the facing axis (forward and back),
   * avoiding unnecessary tile entity lookups in the perpendicular directions.
   */
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);

    // Only two positions matter: forward and backward along the facing axis.
    // Check opposite direction first (behind the backplate) since that's the most
    // common mounting position for the signal head.
    TileEntity te = worldIn.getTileEntity(pos.offset(facing.getOpposite()));
    if (te instanceof TileEntityTrafficSignalHead) {
      return state.withProperty(TILT, ((TileEntityTrafficSignalHead) te).getBodyTilt());
    }
    te = worldIn.getTileEntity(pos.offset(facing));
    if (te instanceof TileEntityTrafficSignalHead) {
      return state.withProperty(TILT, ((TileEntityTrafficSignalHead) te).getBodyTilt());
    }

    return state.withProperty(TILT, TrafficSignalBodyTilt.NONE);
  }

  /**
   * Given a block position, checks if the block there is a backplate and, if so, searches
   * along its facing axis for an adjacent signal or controllable signal block. Returns the
   * position of the signal found, or {@code null} if the block is not a backplate or no
   * signal is adjacent.
   *
   * @param world the world
   * @param pos   the position of the potential backplate block
   * @return the BlockPos of the adjacent signal, or null
   */
  public static BlockPos findSignalBehind(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    Block block = state.getBlock();
    if (!(block instanceof AbstractBlockSignalBackplate)) {
      return null;
    }
    EnumFacing facing = state.getValue(FACING);
    EnumFacing[] horizontalDirs = {EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.EAST, EnumFacing.WEST};
    for (EnumFacing checkDir : horizontalDirs) {
      if (checkDir.getAxis() != facing.getAxis()) continue;
      BlockPos signalPos = pos.offset(checkDir);
      TileEntity te = world.getTileEntity(signalPos);
      if (te instanceof com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead) {
        return signalPos;
      }
    }
    return null;
  }

}
