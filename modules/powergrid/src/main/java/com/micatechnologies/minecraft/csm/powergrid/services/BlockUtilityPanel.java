package com.micatechnologies.minecraft.csm.powergrid.services;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
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
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The building's main breaker panel: a right-click swings its door open on the breakers and the
 * next shuts it. {@link #OPEN} is stored in the bit above the facing and picks the model (door
 * shut, or door swung a quarter turn out on its hinge with the dead front and breakers behind
 * it). Its box follows the door: the open door stands out past the tub and should not be
 * walked through, and a shut panel should not catch the player where the door is not.
 *
 * @since 2026.9
 */
public class BlockUtilityPanel extends BlockUtilityFixture {

  /** Whether the door is open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  private final AxisAlignedBB openBox;

  /**
   * Constructs a panel.
   *
   * @param registryName its registry name
   * @param shutBox      its box with the door shut, facing north, in sixteenths
   * @param openBox      its box with the door open, facing north, in sixteenths
   */
  public BlockUtilityPanel(String registryName, double[] shutBox, double[] openBox) {
    super(registryName, shutBox);
    this.openBox = new AxisAlignedBB(openBox[0] / 16.0, openBox[1] / 16.0, openBox[2] / 16.0,
        openBox[3] / 16.0, openBox[4] / 16.0, openBox[5] / 16.0);
    setDefaultState(getDefaultState().withProperty(OPEN, false));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(OPEN) ? openBox : super.getBlockBoundingBox(state, source, pos);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 3).withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, false);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      world.playSound(null, pos, open ? SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN
              : SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.6F,
          open ? 1.2F : 1.3F);
    }
    return true;
  }
}
