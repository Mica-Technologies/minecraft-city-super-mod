package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalSensor;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Base class for traffic signal sensor blocks. Sensors detect vehicle proxies (players and
 * villagers) within configurable scan regions and report counts to the linked
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.TileEntityTrafficSignalController}
 * for sensor-actuated signal control.
 *
 * <p><b>Facing convention:</b> sensors inherit {@code FACING = placer.getHorizontalFacing()
 * .getOpposite()} from {@link AbstractBlockRotatableNSEW}. Sensors <i>must</i> be placed with
 * the same facing convention as the signal heads serving the same approach: stand where the
 * signal head's viewing audience would stand (i.e., on the approach, looking the same
 * direction the controlled vehicle travels) so that placer-opposite gives the sensor a
 * facing that matches the signal head's facing.</p>
 *
 * <p>The per-direction FYA-vs-protected demand arbitration in
 * {@link com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerTickerUtilities#getEffectiveLeftDemand}
 * and {@link com.micatechnologies.minecraft.csm.trafficsignals.logic.TrafficSignalControllerTickerUtilities#getEffectiveRightDemand}
 * correlates each sensor's directional zone count with FYA signals of matching {@code FACING}.
 * A facing mismatch silently breaks the single-vehicle FYA-clearance assumption and inflates
 * phase priority for left/right turn phases. Omnidirectional totals are unaffected.</p>
 *
 * <p>See {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md} ("Sensor Facing Convention") for the
 * full discussion.</p>
 */
public abstract class AbstractBlockTrafficSignalSensor extends AbstractBlockRotatableNSEW
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation, ITrafficSignalSensor {

  public AbstractBlockTrafficSignalSensor(Material materialIn) {
    super(materialIn);
  }

  /**
   * Retrieves the bounding box of the block.
   *
   * @param state  the block state
   * @param source the block access
   * @param pos    the block position
   *
   * @return The bounding box of the block.
   *
   * @since 1.0
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return SQUARE_BOUNDING_BOX;
  }

  /**
   * Retrieves whether the block is an opaque cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is an opaque cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block is a full cube.
   *
   * @param state The block state.
   *
   * @return {@code true} if the block is a full cube, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  /**
   * Retrieves whether the block connects to redstone.
   *
   * @param state  the block state
   * @param access the block access
   * @param pos    the block position
   * @param facing the block facing direction
   *
   * @return {@code true} if the block connects to redstone, {@code false} otherwise.
   *
   * @since 1.0
   */
  @Override
  public boolean getBlockConnectsRedstone(IBlockState state,
      IBlockAccess access,
      BlockPos pos,
      @Nullable
      EnumFacing facing) {
    return false;
  }

  /**
   * Retrieves the block's render layer.
   *
   * @return The block's render layer.
   *
   * @since 1.0
   */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  /**
   * Gets the tile entity class for the block.
   *
   * @return the tile entity class for the block
   *
   * @since 1.0
   */
  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityTrafficSignalSensor.class;
  }

  /**
   * Gets the tile entity name for the block.
   *
   * @return the tile entity name for the block
   *
   * @since 1.0
   */
  @Override
  public String getTileEntityName() {
    return "tileentitytrafficsignalsensor";
  }

  /**
   * Gets a new tile entity for the block.
   *
   * @param worldIn the world
   * @param meta    the block metadata
   *
   * @return the new tile entity for the block
   *
   * @since 1.1
   */
  @Nullable
  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntityTrafficSignalSensor();
  }
}
