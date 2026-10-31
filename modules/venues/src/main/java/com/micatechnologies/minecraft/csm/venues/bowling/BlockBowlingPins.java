package com.micatechnologies.minecraft.csm.venues.bowling;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.VenuesSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The ten pins standing on a pin deck, in their triangle at real size and spacing (12 in apart,
 * 15 in tall), or lying where a strike left them. One block, in the air over the front pin deck
 * block of the half the lane's centre is beside: it draws all ten, reaching into the other half.
 * Decor: a baked model, no tile entity.
 *
 * <p>A click rolls a ball (the rumble down the lane) and a moment later knocks the pins down with
 * a crash; a click on fallen pins stands them again. Nothing ticks: the crash is one scheduled
 * update.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing, towards the pins as the lane's (S 0, W 1, N 2, E 3);
 * bit 2 the right half; bit 3 fallen. Placed by hand on a lane block it takes that block's facing
 * and half. No collision.</p>
 *
 * @since 2026.10
 */
public class BlockBowlingPins extends BlockVenue {

  /** On the right half of the lane. Stored. */
  public static final PropertyBool RIGHT = BlockBowlingLane.RIGHT;
  /** Knocked down. Stored. */
  public static final PropertyBool FALLEN = PropertyBool.create("fallen");

  /** From the click to the crash: the ball's roll down 60 ft. */
  private static final int ROLL_TICKS = 34;

  private static final AxisAlignedBB LEFT_BOX = new AxisAlignedBB(8 / 16.0, 0, 0, 1, 6.5 / 16.0,
      1);
  private static final AxisAlignedBB RIGHT_BOX = new AxisAlignedBB(0, 0, 0, 8 / 16.0, 6.5 / 16.0,
      1);

  /**
   * Constructs the pins.
   *
   * @param registryName its registry name
   */
  public BlockBowlingPins(String registryName) {
    super(registryName, Material.WOOD, SoundType.WOOD, 0.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(RIGHT, false).withProperty(FALLEN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, RIGHT, FALLEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(RIGHT, (meta & 4) != 0).withProperty(FALLEN, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(RIGHT) ? 4 : 0)
        | (state.getValue(FALLEN) ? 8 : 0);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing side, float hitX,
      float hitY, float hitZ, int meta, EntityLivingBase placer) {
    IBlockState below = world.getBlockState(pos.down());
    if (below.getBlock() instanceof BlockBowlingLane) {
      return getDefaultState().withProperty(FACING, below.getValue(FACING))
          .withProperty(RIGHT, below.getValue(BlockBowlingLane.RIGHT));
    }
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(RIGHT) ? RIGHT_BOX : LEFT_BOX;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return true;
    }
    if (!world.isRemote) {
      if (state.getValue(FALLEN)) {
        world.setBlockState(pos, state.withProperty(FALLEN, false), 3);
        world.playSound(null, pos, net.minecraft.init.SoundEvents.BLOCK_WOOD_PLACE,
            SoundCategory.BLOCKS, 0.6F, 1.4F);
      } else if (!world.isUpdateScheduled(pos, this)) {
        world.playSound(null, pos, VenuesSounds.BOWLING_ROLL.getSoundEvent(),
            SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.scheduleUpdate(pos, this, ROLL_TICKS);
      }
    }
    return true;
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && !state.getValue(FALLEN)) {
      world.setBlockState(pos, state.withProperty(FALLEN, true), 3);
      world.playSound(null, pos, VenuesSounds.BOWLING_STRIKE.getSoundEvent(),
          SoundCategory.BLOCKS, 1.2F, 0.95F + rand.nextFloat() * 0.1F);
    }
  }
}
