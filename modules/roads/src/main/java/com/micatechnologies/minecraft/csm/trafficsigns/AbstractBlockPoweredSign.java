package com.micatechnologies.minecraft.csm.trafficsigns;

import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.SignShift;
import javax.annotation.Nonnull;
import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Abstract base class for redstone-powered two-state traffic signs (electronic / adaptive signs
 * that display one face when energized and another — usually a blank LED matrix — when not).
 *
 * <p>Extends {@link AbstractBlockSign} so these signs keep the full static-sign behavior
 * (eight-direction rotation, setback / back-to-back shifting, downward stacking) and adds a single
 * stored {@link #POWERED} boolean driven by adjacent redstone. The blockstate JSON swaps the sign
 * face texture on that property. {@code POWERED} is packed into the high bit of the block metadata;
 * the eight facings occupy the low three bits, so the two coexist without a tile entity.</p>
 *
 * @author Mica Technologies
 * @since 2026.7
 */
@MethodsReturnNonnullByDefault
public abstract class AbstractBlockPoweredSign extends AbstractBlockSign {

  /**
   * Whether the sign is currently energized by adjacent redstone. Stored in metadata bit 3.
   *
   * @since 2026.7
   */
  public static final PropertyBool POWERED = PropertyBool.create("powered");

  public AbstractBlockPoweredSign() {
    super();
    // AbstractBlockSign's constructor set a default state that predates the POWERED property, so
    // re-establish a fully specified default here (Minecraft requires every property to have a
    // value on the default state).
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, DirectionEight.N)
        .withProperty(DOWNWARD, false)
        .withProperty(SHIFT, SignShift.NONE)
        .withProperty(POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, DOWNWARD, SHIFT, POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState()
        .withProperty(FACING, DirectionEight.values()[meta & 7])
        .withProperty(POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getIndex();
    if (state.getValue(POWERED)) {
      meta |= 8;
    }
    return meta;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState placed =
        super.getStateForPlacement(worldIn, pos, facing, hitX, hitY, hitZ, meta, placer);
    return placed.withProperty(POWERED, worldIn.getRedstonePowerFromNeighbors(pos) > 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    super.neighborChanged(state, worldIn, pos, blockIn, fromPos);
    if (!worldIn.isRemote) {
      boolean powered = worldIn.getRedstonePowerFromNeighbors(pos) > 0;
      if (state.getValue(POWERED) != powered) {
        worldIn.setBlockState(pos, state.withProperty(POWERED, powered), 2);
      }
    }
  }
}
