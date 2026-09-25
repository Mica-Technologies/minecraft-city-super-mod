package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A small piece set on a desk or a counter that switches on and off: a desk lamp, or a
 * computer's or a laptop's screen. It stands on whatever it is placed on, as any
 * {@link BlockCounterPiece} does; right-click switches it, with a button's click. On, it gives
 * the light it was made with and the blockstate shows its lamp or screen lit (the {@code glow}
 * texture swapped); off, both go dark. It is placed on.
 *
 * <p>It follows redstone too: a change of power switches it on or off ({@link LampSwitching}).
 * {@link #LIT} is stored in the bit above the facing, {@link LampSwitching#POWERED} in the top
 * bit.</p>
 *
 * @since 2026.9
 */
public class BlockCounterLight extends BlockCounterPiece implements ISwitchable {

  /** Whether the lamp or the screen is on. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  private final int lightLevel;

  /**
   * Constructs a piece that switches on and off.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north standing on the floor, in sixteenths
   * @param material     its material
   * @param sound        its block sound
   * @param layer        the render layer
   * @param lightLevel   the light it gives while on, 0 to 15
   */
  public BlockCounterLight(String registryName, int[] box, Material material, SoundType sound,
      BlockRenderLayer layer, int lightLevel) {
    super(registryName, box, material, sound, layer);
    this.lightLevel = lightLevel;
    setDefaultState(getDefaultState().withProperty(LIT, true)
        .withProperty(LampSwitching.POWERED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, REST, LIT, LampSwitching.POWERED);
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
    return state.getValue(LIT) ? lightLevel : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean lit = !state.getValue(LIT);
      world.setBlockState(pos, state.withProperty(LIT, lit), 3);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.7F : 0.6F);
    }
    return true;
  }

  /**
   * A change of redstone power switches it: on when power comes, off when it goes
   * ({@link LampSwitching}), so a light switch, linked or beside it, works it.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    LampSwitching.follow(world, pos, state, LIT);
  }
}
