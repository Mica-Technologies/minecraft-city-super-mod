package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalHead;
import com.micatechnologies.minecraft.csm.trafficsignals.logic.AbstractBlockControllableSignalHead;
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
 * Abstract base class for traffic signal backplate blocks. A single computed
 * {@link #MODEL_VARIANT} property exposes the adjacent signal's tilt and horizontal-orientation
 * together, so each of the ten (tilt &times; horizontal) pairings can map to its own model in
 * the blockstate JSON. The variant is derived per render frame from the
 * {@link TileEntityTrafficSignalHead} in the block behind this backplate; no tile entity or
 * metadata storage is required on the backplate itself.
 */
public abstract class AbstractBlockSignalBackplate extends AbstractBlockRotatableNSEWUD {

  /**
   * The combined tilt + horizontal-orientation property used for model selection. Forge v1
   * blockstates can only branch model selection on one property at a time, so tilt and
   * horizontal are collapsed into this single enum.
   */
  public static final PropertyEnum<BackplateModelVariant> MODEL_VARIANT =
      PropertyEnum.create("modelvariant", BackplateModelVariant.class);

  public AbstractBlockSignalBackplate(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
        lightLevel, lightOpacity, false);
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(MODEL_VARIANT, BackplateModelVariant.V_NONE));
  }

  @Override
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MODEL_VARIANT);
  }

  /**
   * Computes the actual model variant by reading the signal head behind this backplate.
   * "Behind" is the block one step in the opposite direction of {@link #FACING}, with a
   * forward fallback since the backplate can sit in front of the signal in some configurations.
   */
  @Override
  public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    TrafficSignalBodyTilt tilt = TrafficSignalBodyTilt.NONE;
    boolean horizontal = false;

    BlockPos signalPos = null;
    TileEntity te = worldIn.getTileEntity(pos.offset(facing.getOpposite()));
    if (te instanceof TileEntityTrafficSignalHead) {
      signalPos = pos.offset(facing.getOpposite());
      tilt = ((TileEntityTrafficSignalHead) te).getBodyTilt();
    } else {
      te = worldIn.getTileEntity(pos.offset(facing));
      if (te instanceof TileEntityTrafficSignalHead) {
        signalPos = pos.offset(facing);
        tilt = ((TileEntityTrafficSignalHead) te).getBodyTilt();
      }
    }

    if (signalPos != null) {
      IBlockState signalState = worldIn.getBlockState(signalPos);
      if (signalState.getBlock() instanceof AbstractBlockControllableSignalHead) {
        horizontal = ((AbstractBlockControllableSignalHead) signalState.getBlock())
            .isHorizontal(worldIn, signalPos);
      }
    }

    return state.withProperty(MODEL_VARIANT, BackplateModelVariant.of(tilt, horizontal));
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
