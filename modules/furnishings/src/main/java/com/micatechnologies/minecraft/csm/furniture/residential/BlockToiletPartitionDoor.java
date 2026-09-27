package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.List;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A toilet partition's stall door: a door between two narrow pilasters under the headrail,
 * hinged on its left and swinging in, into the stall, as a stall door does. Right-click either
 * half to open or shut it, with the steel door sounds of the school locker. {@link #OPEN} is
 * stored in both halves, in the bit above {@link #UPPER}; the open model is the door written
 * out swung a quarter turn about its hinge (a model element turns only to 45 degrees), lying
 * along the stall's left side, clear of the doorway. Otherwise it is a {@link BlockToiletPartition}
 * and joins a run like one.
 *
 * @since 2026.9
 */
public class BlockToiletPartitionDoor extends BlockToiletPartition {

  /** Whether the door stands open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  /** The front with the door open, facing north, in sixteenths: its pilasters, the headrail
   * across, and the door swung in about its hinge (the click boxes). The way in, between the
   * open door and the right pilaster, is 12.95 sixteenths, 0.81 of a block. */
  private static final double[][] OPEN_FRONT = {
      {0, 0, 0.25, 1.5, TOP, 1.75},
      {15.25, 0, 0.25, 16, TOP, 1.75},
      {0, 29, 0.25, 16, TOP, 1.75},
      {1.5, 5, 1.4, 2.3, 28.75, 14.9}};
  /** The same as collision boxes: the pilasters and headrail only as deep as their panels, the
   * door thinner than it is drawn. */
  private static final double[][] OPEN_FRONT_SOLID = {
      {0, 0, 0.5, 1.25, TOP, 1.5},
      {15.25, 0, 0.5, 16, TOP, 1.5},
      {0, 29, 0.5, 16, TOP, 1.5},
      {1.5, 5, 1.5, 2.0, 28.75, 14.9}};

  /**
   * Constructs a stall door.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockToiletPartitionDoor(String registryName) {
    super(registryName, Kind.DOOR);
    setDefaultState(getDefaultState().withProperty(OPEN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER, OPEN, LEFT, RIGHT);
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
        .withProperty(OPEN, false);
  }

  @Override
  protected List<double[]> parts(IBlockState actual, boolean solid) {
    List<double[]> out = super.parts(actual, solid);
    if (actual.getValue(OPEN)) {
      out.remove(solid ? FRONT_SOLID : FRONT);
      for (double[] part : solid ? OPEN_FRONT_SOLID : OPEN_FRONT) {
        out.add(part);
      }
    }
    return out;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking()) {
      return false;
    }
    if (!world.isRemote) {
      BlockPos lower = state.getValue(UPPER) ? pos.down() : pos;
      boolean open = !state.getValue(OPEN);
      for (BlockPos half : new BlockPos[]{lower, lower.up()}) {
        IBlockState s = world.getBlockState(half);
        if (s.getBlock() == this) {
          world.setBlockState(half, s.withProperty(OPEN, open), 3);
        }
      }
      ICsmSound sound = open ? FurnishingsSounds.LOCKER_DOOR_OPEN
          : FurnishingsSounds.LOCKER_DOOR_CLOSE;
      SoundEvent event = sound.getSoundEvent();
      if (event != null) {
        world.playSound(null, lower, event, SoundCategory.BLOCKS, 0.7F, open ? 1.1F : 1.05F);
      }
    }
    return true;
  }
}
