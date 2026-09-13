package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The crossing gate: the mechanism cabinet is the block, the arm is drawn by the tile entity's
 * renderer, swinging between raised and lowered over several seconds as a real one does. The
 * arm extends to the block's right as you face it, across the lanes it closes; the three
 * lengths are three blocks so the arm's reach is fixed by what was placed rather than
 * configured.
 *
 * @author Mica Technologies
 * @since 2026.9
 */
public abstract class BlockRailroadCrossingGate extends AbstractBlockRailroadCrossing {

  /**
   * @return how many lanes the arm closes
   */
  public abstract int getLanes();

  /**
   * The arm's length in blocks: three and a half a lane, plus the half block from the pivot to
   * the edge of the first lane.
   *
   * @return the arm length, in blocks
   */
  public float getArmLength() {
    return getLanes() * 3.5F + 0.5F;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityRailroadCrossingGate.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityrailroadcrossinggate";
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityRailroadCrossingGate();
  }

  /** The mechanism cabinet; the arm is not solid, as a real one is meant to be driven through
   * in an emergency. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.75D, 0.75D);
  }

  /** One lane. */
  public static class OneLane extends BlockRailroadCrossingGate {
    @Override
    public int getLanes() {
      return 1;
    }

    @Override
    public String getBlockRegistryName() {
      return "railroad_crossing_gate_1";
    }
  }

  /** Two lanes. */
  public static class TwoLane extends BlockRailroadCrossingGate {
    @Override
    public int getLanes() {
      return 2;
    }

    @Override
    public String getBlockRegistryName() {
      return "railroad_crossing_gate_2";
    }
  }

  /** Three lanes. */
  public static class ThreeLane extends BlockRailroadCrossingGate {
    @Override
    public int getLanes() {
      return 3;
    }

    @Override
    public String getBlockRegistryName() {
      return "railroad_crossing_gate_3";
    }
  }
}
