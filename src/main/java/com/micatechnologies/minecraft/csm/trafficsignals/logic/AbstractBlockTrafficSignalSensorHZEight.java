package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableHZEight;
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
 * Base class for traffic signal sensor blocks that use eight-direction horizontal
 * ({@link AbstractBlockRotatableHZEight}) rotation rather than the four-direction
 * {@link com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW} rotation used by
 * {@link AbstractBlockTrafficSignalSensor}. This exists for sensors (e.g. the Miovision SmartView
 * 360 omnidirectional camera) whose realistic placement benefits from finer 45&deg; rotation steps.
 *
 * <p>Functionally identical to {@link AbstractBlockTrafficSignalSensor}: it reuses the same
 * {@link TileEntityTrafficSignalSensor}, whose detection is corner-region based and therefore
 * independent of the block's {@code FACING}. Because an HZEight block's {@code FACING} is a
 * {@link com.micatechnologies.minecraft.csm.codeutils.DirectionEight} property (a different
 * property object than {@link net.minecraft.block.BlockHorizontal#FACING}), the controller's
 * directional FYA-vs-protected correlation (which keys off {@code BlockHorizontal.FACING} via
 * {@code TrafficSignalControllerTickerUtilities.signalFacingOrNull}) simply treats these sensors as
 * having no cardinal facing: they are skipped by the per-direction demand arbitration and counted in
 * the omnidirectional totals. That is the correct behavior for an omnidirectional 360&deg; camera,
 * and {@code validateSensorFacings} likewise skips them (no false mismatch warnings).</p>
 *
 * <p>For the standard 4-way sensor facing convention, see {@link AbstractBlockTrafficSignalSensor}
 * and {@code assets/docs/TRAFFIC_SIGNAL_SYSTEM.md}.</p>
 */
public abstract class AbstractBlockTrafficSignalSensorHZEight extends AbstractBlockRotatableHZEight
    implements ICsmTileEntityProvider, ICsmNoSnowAccumulation, ITrafficSignalSensor {

  public AbstractBlockTrafficSignalSensorHZEight(Material materialIn) {
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
   * @return {@code false} -- sensors are not opaque cubes.
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
   * @return {@code false} -- sensors are not full cubes.
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
   * @return {@code false} -- sensors do not connect to redstone.
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
