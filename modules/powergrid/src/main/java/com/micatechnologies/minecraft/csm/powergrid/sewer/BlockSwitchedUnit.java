package com.micatechnologies.minecraft.csm.powergrid.sewer;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.streetscape.BlockUtilityBox;
import com.micatechnologies.minecraft.csm.streetscape.UtilityBoxSpec;
import javax.annotation.Nonnull;
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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A lift station's unit that is on or off: Roads' utility box multi-block (the root draws the
 * whole unit, invisible parts fill the rest, placing is all or nothing) with {@link #ON} stored
 * in the bit above the facing, picking the parts that show it.
 *
 * <ul>
 *   <li>The control panel follows redstone at its root: powered, its alarm beacon flashes (an
 *   animated texture) and its flood light is lit and lights the ground round it.</li>
 *   <li>The standby generator starts and stops at a click on any of its cells: running, its
 *   controller shows RUN and its readings and the exhaust's rain cap stands open.</li>
 * </ul>
 *
 * <p>Nothing ticks and there is no tile entity: eight states.</p>
 *
 * @since 2026.9
 */
public class BlockSwitchedUnit extends BlockUtilityBox {

  /** Whether the unit is on: the panel alarmed and lit, the generator running. */
  public static final PropertyBool ON = PropertyBool.create("on");

  private final boolean clickToggles;
  private final int lightWhenOn;

  /**
   * @param registryName its registry name
   * @param spec         its size and shape
   * @param clickToggles true for a unit started and stopped by a click, false for one that
   *                     follows redstone at its root
   * @param lightWhenOn  the light it gives while on (0 for none)
   */
  public BlockSwitchedUnit(String registryName, UtilityBoxSpec spec, boolean clickToggles,
      int lightWhenOn) {
    super(registryName, spec);
    this.clickToggles = clickToggles;
    this.lightWhenOn = lightWhenOn;
    setDefaultState(getDefaultState().withProperty(ON, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, ON);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(ON, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(ON) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(ON, !clickToggles && world.isBlockPowered(pos));
  }

  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote || clickToggles) {
      return;
    }
    boolean powered = world.isBlockPowered(pos);
    if (powered != state.getValue(ON)) {
      world.setBlockState(pos, state.withProperty(ON, powered), 3);
    }
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (!clickToggles || player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean on = !state.getValue(ON);
      world.setBlockState(pos, state.withProperty(ON, on), 3);
      toggled(world, pos, player, on);
    }
    return true;
  }

  /**
   * Tells the player a click turned the unit on or off, on the server: the generator's lever
   * click and its status message. A unit whose click does something else (a cabinet's doors)
   * says so here.
   *
   * @param world  the world
   * @param pos    the root's position
   * @param player the player who clicked
   * @param on     the unit's new state
   */
  protected void toggled(World world, BlockPos pos, EntityPlayer player, boolean on) {
    world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.6F,
        on ? 0.8F : 0.6F);
    player.sendStatusMessage(new TextComponentTranslation(
        on ? "csm.utilities.generator.running" : "csm.utilities.generator.stopped"), true);
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(IBlockState state) {
    return lightWhenOn > 0 && state.getValue(ON) ? lightWhenOn : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world, @Nonnull BlockPos pos) {
    return getLightValue(state);
  }
}
