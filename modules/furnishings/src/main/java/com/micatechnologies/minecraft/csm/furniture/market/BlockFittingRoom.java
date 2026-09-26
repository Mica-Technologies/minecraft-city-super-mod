package com.micatechnologies.minecraft.csm.furniture.market;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockCloset;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockResidentialRun;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A fitting room booth, two blocks tall: a back wall with a mirror and a bench, a partition on
 * its left, and a curtain on a rod across its front. Booths set side by side join into a row as
 * closets do ({@link BlockResidentialRun#LEFT}, {@link BlockResidentialRun#RIGHT}, actual state):
 * each booth draws the partition on its own left, so a row has one wall between each pair, and
 * the booth at the row's right end also draws its right-hand wall and the FITTING ROOMS sign.
 *
 * <p>A click on either half draws the curtain across or back to its side ({@link #OPEN}, stored
 * in both halves, in the bit above the half). Only the walls and a drawn curtain stop a player,
 * so an open booth can be walked into.</p>
 *
 * @since 2026.9
 */
public class BlockFittingRoom extends BlockCloset {

  /** Whether the curtain is drawn back. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  private static final AxisAlignedBB BACK = box(0, 0, 15.25, 16, 16, 16);
  private static final AxisAlignedBB LEFT_WALL = box(0, 0, 0.5, 0.5, 16, 15.25);
  private static final AxisAlignedBB RIGHT_WALL = box(15.5, 0, 0.5, 16, 16, 15.25);
  private static final AxisAlignedBB CURTAIN = box(0.5, 0, 1.25, 15.5, 16, 2.25);

  /**
   * Constructs a fitting room booth.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths from the floor (up to 32)
   */
  public BlockFittingRoom(String registryName, int[] box) {
    super(registryName, box, 0, null, null);
    setDefaultState(getDefaultState().withProperty(OPEN, true));
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16, y0 / 16, z0 / 16, x1 / 16, y1 / 16, z1 / 16);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER, BlockResidentialRun.LEFT,
        BlockResidentialRun.RIGHT, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return super.getStateFromMeta(meta & 7).withProperty(OPEN, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (state.getValue(OPEN) ? 8 : 0);
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(OPEN, true);
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      boolean open = !state.getValue(OPEN);
      BlockPos other = state.getValue(UPPER) ? pos.down() : pos.up();
      world.setBlockState(pos, state.withProperty(OPEN, open), 3);
      IBlockState otherState = world.getBlockState(other);
      if (otherState.getBlock() == this) {
        world.setBlockState(other, otherState.withProperty(OPEN, open), 3);
      }
      SoundEvent event = FurnishingsSounds.CURTAIN_SLIDE.getSoundEvent();
      if (event != null) {
        world.playSound(null, pos, event, SoundCategory.BLOCKS, 0.7F, open ? 1.0F : 0.9F);
      }
    }
    return true;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> boxes, @Nullable Entity entity, boolean isActualState) {
    IBlockState s = isActualState ? state : state.getActualState(world, pos);
    EnumFacing facing = s.getValue(FACING);
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(BACK, facing));
    addCollisionBoxToList(pos, entityBox, boxes,
        RotationUtils.rotateBoundingBoxByFacing(LEFT_WALL, facing));
    if (!s.getValue(BlockResidentialRun.RIGHT)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(RIGHT_WALL, facing));
    }
    if (!s.getValue(OPEN)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          RotationUtils.rotateBoundingBoxByFacing(CURTAIN, facing));
    }
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT;
  }
}
