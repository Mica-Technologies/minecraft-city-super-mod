package com.micatechnologies.minecraft.csm.hvac;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEWUD;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The cabinet and rooftop heaters and coolers: an outdoor unit that stands upright, its front
 * (the condensing unit's service panel, the rooftop unit's air hood) toward the placer. Models
 * from {@code dev-env-utils/scripts/gen_hvac_units.py}.
 *
 * <p>The blocks keep the six-way facing they were registered with, so worlds load unchanged, but
 * a unit is only ever placed turned to one of the four compass directions now. One placed facing
 * up or down before stands upright facing north, and its box is the north box.</p>
 *
 * @author Mica Technologies
 * @since 2026.11
 */
public abstract class AbstractBlockHvacUnit extends AbstractBlockRotatableNSEWUD
    implements ICsmTileEntityProvider {

  /** The rooftop unit's box, facing north: 2 1/8 blocks wide, 2 tall, 3 long. */
  static final AxisAlignedBB RTU_BOX = new AxisAlignedBB(-0.5625, 0.0, -1.0, 1.5625, 2.0, 2.0);

  protected AbstractBlockHvacUnit(Material material, SoundType soundType, String harvestToolClass,
      int harvestLevel, float hardness, float resistance, float lightLevel, int lightOpacity) {
    super(material, soundType, harvestToolClass, harvestLevel, hardness, resistance, lightLevel,
        lightOpacity);
  }

  /** The unit's facing on the compass: up and down (placed before 2026.11) read as north. */
  static EnumFacing compass(IBlockState state) {
    if (!state.getPropertyKeys().contains(FACING)) {
      return EnumFacing.NORTH;
    }
    EnumFacing f = state.getValue(FACING);
    return f.getAxis() == EnumFacing.Axis.Y ? EnumFacing.NORTH : f;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState here = source.getBlockState(pos);
    IBlockState s = here.getBlock() == this ? here : state;
    return RotationUtils.rotateBoundingBoxByFacing(getBlockBoundingBox(s, source, pos),
        compass(s));
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return true;
  }

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
