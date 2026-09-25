package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialRun;
import com.micatechnologies.minecraft.csm.furniture.residential.ISwitchable;
import com.micatechnologies.minecraft.csm.furniture.residential.LampSwitching;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * Garden string lights: a strand of festoon bulbs swagged across the top of the block, placed
 * side by side into a run that reads as one strand ({@link BlockResidentialRun}: a hook where the
 * run stops, nothing where it goes on). Nothing collides with them. They light the garden at
 * level 8 while {@link #LIT}: a right-click, or a change of redstone power to any block of the
 * run ({@link LampSwitching}, so a light switch works them), switches the whole run.
 *
 * <p>{@link #LIT} is stored in the bit above the facing, {@link LampSwitching#POWERED} in the top
 * bit.</p>
 *
 * @since 2026.9
 */
public class BlockStringLights extends BlockResidentialRun implements ISwitchable {

  /** Whether the bulbs are on. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  /** The light a lit block gives. */
  private static final int LIGHT = 8;
  /** The longest run a click or a change of power switches. */
  private static final int MAX_RUN = 64;

  /**
   * Constructs string lights.
   *
   * @param registryName its registry name, ending in its bulbs' colour
   * @param box          its box facing north, in sixteenths
   */
  public BlockStringLights(String registryName, int[] box) {
    super(registryName, box, false);
    setDefaultState(getDefaultState().withProperty(LIT, true)
        .withProperty(LampSwitching.POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, LEFT, RIGHT, LIT, LampSwitching.POWERED);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(LIT, (meta & 4) != 0)
        .withProperty(LampSwitching.POWERED, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIT) ? 4 : 0)
        | (state.getValue(LampSwitching.POWERED) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, true).withProperty(LampSwitching.POWERED, false);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(LIT) ? LIGHT : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** Walked through: the strand hangs out of everyone's way. */
  @Nullable
  @Override
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      setRun(world, pos, state, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.6F : 0.5F);
    }
    return true;
  }

  /** A change of redstone power switches the run: on when power comes, off when it goes. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(LampSwitching.POWERED)) {
      IBlockState next = state.withProperty(LampSwitching.POWERED, powered);
      world.setBlockState(pos, next, 3);
      if (powered != state.getValue(LIT)) {
        setRun(world, pos, next, powered);
      }
    }
  }

  /** Switches every block of the run {@code pos} is in, facing its way, on or off. */
  private void setRun(World world, BlockPos pos, IBlockState state, boolean lit) {
    world.setBlockState(pos, state.withProperty(LIT, lit), 3);
    EnumFacing facing = state.getValue(FACING);
    for (EnumFacing side : new EnumFacing[]{facing.rotateY(), facing.rotateYCCW()}) {
      BlockPos at = pos.offset(side);
      for (int i = 0; i < MAX_RUN; i++) {
        IBlockState s = world.getBlockState(at);
        if (s.getBlock() != this || s.getValue(FACING) != facing) {
          break;
        }
        if (s.getValue(LIT) != lit) {
          world.setBlockState(at, s.withProperty(LIT, lit), 3);
        }
        at = at.offset(side);
      }
    }
  }
}
