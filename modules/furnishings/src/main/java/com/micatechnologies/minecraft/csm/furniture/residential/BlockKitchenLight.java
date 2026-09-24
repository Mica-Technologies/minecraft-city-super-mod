package com.micatechnologies.minecraft.csm.furniture.residential;

import javax.annotation.Nonnull;
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
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A kitchen fitting with a lamp: the range hoods (chimney and under-cabinet) and the
 * under-cabinet light. It is placed lit; right-click switches its lamp off and on. Lit, it
 * gives the light level it was made with and the blockstate shows its lens glowing.
 *
 * <p>{@link #LIT} is stored, in the bit above the facing.</p>
 *
 * @since 2026.9
 */
public class BlockKitchenLight extends BlockResidentialFurniture {

  /** Whether the lamp is on. */
  public static final PropertyBool LIT = PropertyBool.create("lit");

  private final int lightLevel;

  /**
   * Constructs a kitchen light.
   *
   * @param registryName its registry name
   * @param box          its box facing north, in sixteenths
   * @param lightLevel   the light it gives when lit, 0 to 15
   */
  public BlockKitchenLight(String registryName, int[] box, int lightLevel) {
    super(registryName, box, false);
    this.lightLevel = lightLevel;
    setDefaultState(getDefaultState().withProperty(LIT, true));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(LIT, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(LIT) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(LIT, true);
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
          0.3F, lit ? 0.6F : 0.5F);
    }
    return true;
  }
}
