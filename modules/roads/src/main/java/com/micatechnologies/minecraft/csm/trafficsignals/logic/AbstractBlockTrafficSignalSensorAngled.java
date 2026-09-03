package com.micatechnologies.minecraft.csm.trafficsignals.logic;

import javax.annotation.Nonnull;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Base class for detection-camera sensors that keep a real four-way cardinal
 * {@link net.minecraft.block.BlockHorizontal#FACING} (inherited from
 * {@link AbstractBlockTrafficSignalSensor}, so the controller's directional demand arbitration works)
 * while adding a separate, purely-cosmetic {@link SensorAngle} that swings the camera body
 * +/-45&deg; about its mount point. This lets a camera mounted on a cardinal pole aim diagonally
 * without floating off the pole.
 *
 * <p>The cardinal {@code FACING} is derived from the placer exactly like
 * {@link com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW}. The {@code ANGLE}
 * is set through the sensor configuration GUI (see {@code SensorConfigGui}), defaults to
 * {@link SensorAngle#NONE}, and never changes which approach the controller attributes the sensor
 * to. Rendering is pure blockstate: the blockstate selects the model by {@code ANGLE}
 * (base / {@code *_diagl} / {@code *_diagr}) and rotates it by the cardinal {@code FACING}.</p>
 *
 * <p>The two properties are packed into block metadata as
 * {@code facing.getHorizontalIndex() | (angle.ordinal() << 2)} (range 0..11, fits a nibble).</p>
 */
public abstract class AbstractBlockTrafficSignalSensorAngled extends AbstractBlockTrafficSignalSensor {

  /** Cosmetic aim angle (none / left / right). */
  public static final PropertyEnum<SensorAngle> ANGLE =
      PropertyEnum.create("angle", SensorAngle.class);

  public AbstractBlockTrafficSignalSensorAngled(Material materialIn) {
    super(materialIn);
    // FACING default (NORTH) is set by the parent; pin ANGLE so the default state has every
    // property assigned.
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(ANGLE, SensorAngle.NONE));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, ANGLE);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
    int angleOrdinal = (meta >> 2) & 3;
    if (angleOrdinal < 0 || angleOrdinal >= SensorAngle.values().length) {
      angleOrdinal = SensorAngle.NONE.ordinal();
    }
    return getDefaultState()
        .withProperty(FACING, facing)
        .withProperty(ANGLE, SensorAngle.values()[angleOrdinal]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(ANGLE).ordinal() << 2);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return this.getDefaultState()
        .withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(ANGLE, SensorAngle.NONE);
  }
}
