package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.ICsmGlassFronted;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialStorage;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A store fixture set side by side into a run, drawn by {@code gen_furniture_market.py}: the
 * produce stands, the bulk bins, the checkout's belt, scanner and bagging counters, the cart
 * corral. Unlike the Residential run pieces it joins any fixture of its <em>group</em>, not only
 * itself: a stand of apples beside one of oranges is one stand, and a belt counter, a scanner
 * counter and a bagging end are one checkout lane, with end panels only where the lane stops
 * ({@link #LEFT}, {@link #RIGHT}, actual state).
 *
 * <p>A fixture given slots stores things (the bagging end's shelf of bags); one given a sound
 * plays it on right-click instead (the scanner's beep). The checkout counters are a countertop's
 * height, so small pieces rest on them ({@code SurfaceRest}).</p>
 *
 * @since 2026.9
 */
public class BlockMarketRun extends BlockResidentialStorage implements ICsmGlassFronted {

  private final String group;
  private final BlockRenderLayer layer;
  @Nullable
  private final ICsmSound clickSound;
  private final float clickPitch;

  /**
   * Constructs a fixture that joins its group.
   *
   * @param registryName its registry name, ending in its finish or variant
   * @param box          its box facing north, in sixteenths
   * @param group        the run it joins: fixtures of the same group
   * @param slots        how many slots it holds, a multiple of nine, or zero for none
   * @param openSound    the sound of opening it, or null
   * @param closeSound   the sound of closing it, or null
   * @param clickSound   what it plays on right-click when it holds nothing, or null
   * @param clickPitch   the pitch it plays it at
   * @param layer        the render layer (translucent for the bulk bins' clear fronts)
   */
  public BlockMarketRun(String registryName, int[] box, String group, int slots,
      @Nullable ICsmSound openSound, @Nullable ICsmSound closeSound,
      @Nullable ICsmSound clickSound, float clickPitch, BlockRenderLayer layer) {
    super(registryName, box, slots, openSound, closeSound);
    this.group = group;
    this.layer = layer;
    this.clickSound = clickSound;
    this.clickPitch = clickPitch;
  }

  /**
   * Constructs a fixture that joins its group and does nothing when clicked.
   *
   * @param registryName its registry name, ending in its finish or variant
   * @param box          its box facing north, in sixteenths
   * @param group        the run it joins
   * @param layer        the render layer
   */
  public BlockMarketRun(String registryName, int[] box, String group, BlockRenderLayer layer) {
    this(registryName, box, group, 0, null, null, null, 1.0F, layer);
  }

  /**
   * The run this fixture joins.
   *
   * @return its group
   */
  public String getGroup() {
    return group;
  }

  @Override
  protected boolean continues(IBlockAccess world, BlockPos pos, EnumFacing facing,
      EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    return other.getBlock() instanceof BlockMarketRun
        && ((BlockMarketRun) other.getBlock()).group.equals(group)
        && other.getValue(FACING) == facing;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (getSlots() > 0 || clickSound == null) {
      return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      SoundEvent event = clickSound.getSoundEvent();
      if (event != null) {
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.7F, clickPitch);
      }
    }
    return true;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return layer;
  }

  /**
   * Glass-fronted when drawn in the translucent layer (the bulk bins, the pastry and tobacco
   * cases): their glass apart from what is behind it.
   */
  @Override
  public boolean isGlassFronted() {
    return layer == BlockRenderLayer.TRANSLUCENT;
  }
}
