package com.micatechnologies.minecraft.csm.trafficaccessories;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Abstract base class for traffic signal backplate blocks that support both tilt (from
 * adjacent signal heads) and a fitted/full model variant (toggled by sneaking when placed).
 * Used by doghouse and hawk signal backplates which have both properties.
 *
 * <p>Meta encoding: bits 0-2 = facing (6 values), bit 3 = fitted (boolean).
 * The combined tilt + horizontal state is computed via {@link #getActualState} (inherited from
 * {@link AbstractBlockSignalBackplate}) into {@link AbstractBlockSignalBackplate#MODEL_VARIANT}.
 */
public abstract class AbstractBlockSignalBackplateFitted extends AbstractBlockSignalBackplate {

  public static final PropertyBool FITTED = PropertyBool.create("fitted");

  public AbstractBlockSignalBackplateFitted(Material material, SoundType soundType,
      String harvestToolClass, int harvestLevel, float hardness, float resistance,
      float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance,
        lightLevel, lightOpacity);
    this.setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, EnumFacing.NORTH)
        .withProperty(MODEL_VARIANT, BackplateModelVariant.V_NONE)
        .withProperty(FITTED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MODEL_VARIANT, FITTED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int facingVal = meta & 7;
    boolean fittedVal = (meta & 8) != 0;
    return getDefaultState()
        .withProperty(FACING, EnumFacing.byIndex(facingVal))
        .withProperty(FITTED, fittedVal);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex() + (state.getValue(FITTED) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return this.getDefaultState()
        .withProperty(FACING, EnumFacing.getDirectionFromEntityLiving(pos, placer))
        .withProperty(FITTED, placer.isSneaking());
  }
}
