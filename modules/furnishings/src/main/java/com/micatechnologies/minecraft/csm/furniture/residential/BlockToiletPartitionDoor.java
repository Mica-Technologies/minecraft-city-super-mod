package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import com.micatechnologies.minecraft.csm.novelties.FurnishingsSounds;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A toilet partition's stall door: a door between two narrow pilasters under the headrail,
 * hinged on its left and swinging in, into the stall, as a stall door does. Unlike the other
 * partition pieces it is two blocks tall, placed and broken as one, as {@link
 * BlockResidentialTall} pieces are ({@link #UPPER} in the bit above the facing, only the lower
 * half dropping the item). Right-click either half to open or shut it (except the top of the upper half with a partition
 * piece in hand, which stacks the piece on it), with the steel door
 * sounds of the school locker. {@link #OPEN} is stored in both halves, in the bit above
 * {@link #UPPER}; the open model is the door written out swung a quarter turn about its hinge
 * (a model element turns only to 45 degrees), lying along the stall's left side, clear of the
 * doorway.
 *
 * <p>Otherwise it is a {@link BlockToiletPartition} and joins a run like one: its side panels
 * follow the same rules in each half, and run on through the block behind ({@link #BACK}) unless
 * a panel cell there carries them. A partition cell stacked on it makes a floor-to-ceiling
 * stall: the upper half's {@link #TOP} goes false, its headrail stays as the transom's bottom
 * rail and a transom panel fills the rest of the block, and the side panels run to the block's
 * top to meet the cell's.</p>
 *
 * @since 2026.9
 */
public class BlockToiletPartitionDoor extends BlockToiletPartition {

  /** Whether the door stands open. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  /** The headrail's top, in sixteenths from the floor of the lower half. */
  private static final double HEAD = 30.5;
  /** The headrail's bottom, which is the pilasters' top. */
  private static final double HEAD_BOTTOM = 29;

  /**
   * Constructs a stall door.
   *
   * @param registryName its registry name, ending in its finish
   */
  public BlockToiletPartitionDoor(String registryName) {
    super(registryName, Kind.DOOR);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER, OPEN, LEFT, RIGHT, BACK, TOP);
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

  /** Each half's sides and depth as a cell's; the upper half's top as a column's. */
  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    boolean upper = state.getValue(UPPER);
    return withSides(state, world, pos, facing, Kind.DOOR)
        .withProperty(BACK, runsBack(world, pos, facing))
        .withProperty(TOP, upper && !isPartition(world, pos.up(), facing));
  }

  /**
   * The whole piece's boxes, in sixteenths from the floor of the lower half (so up to 32): the
   * front, shut or open, and the side panels, with this half's sides and depth. The lower half
   * never draws above its block, so its {@link #TOP} (always false) does not matter.
   */
  @Override
  protected List<double[]> parts(IBlockState actual, boolean solid) {
    List<double[]> out = new ArrayList<>();
    boolean upper = actual.getValue(UPPER);
    double top = !upper || actual.getValue(TOP) ? HEAD : 32;
    double z0 = solid ? 0.5 : 0.25;
    double z1 = solid ? 1.5 : 1.75;
    if (actual.getValue(OPEN)) {
      // the pilasters, the headrail (and transom) across, and the door swung in about its
      // hinge; the way in, between the open door and the right pilaster, is 12.95 sixteenths,
      // 0.81 of a block
      out.add(new double[]{0, 0, z0, solid ? 1.25 : 1.5, top, z1});
      out.add(new double[]{15.25, 0, z0, 16, top, z1});
      out.add(new double[]{0, HEAD_BOTTOM, z0, 16, top, z1});
      out.add(solid ? new double[]{1.5, 5, 1.5, 2.0, 28.75, 14.9}
          : new double[]{1.5, 5, 1.4, 2.3, 28.75, 14.9});
    } else {
      out.add(new double[]{0, 0, z0, 16, top, z1});
    }
    double[] panel = sidePanel(false, false, 1.5, actual.getValue(BACK));
    panel[1] = 5;
    panel[4] = top == HEAD ? HEAD_BOTTOM : 32;
    addSides(out, actual, panel, null);
    return out;
  }

  /** This half's share of the whole piece's boxes. */
  @Override
  protected List<AxisAlignedBB> boxes(IBlockState actual, boolean solid) {
    double lo = actual.getValue(UPPER) ? 16 : 0;
    List<AxisAlignedBB> out = new ArrayList<>();
    for (double[] b : parts(actual, solid)) {
      double y0 = Math.max(b[1], lo);
      double y1 = Math.min(b[4], lo + 16);
      if (y1 > y0) {
        out.add(new AxisAlignedBB(b[0] / 16, (y0 - lo) / 16, b[2] / 16, b[3] / 16,
            (y1 - lo) / 16, b[5] / 16));
      }
    }
    return out;
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return pos.getY() < world.getHeight() - 1 && super.canPlaceBlockAt(world, pos)
        && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    world.setBlockState(pos.up(), state.withProperty(UPPER, true), 3);
  }

  /** A creative player breaking the upper half takes the lower with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(UPPER) && player.capabilities.isCreativeMode) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        world.setBlockToAir(below);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /** The other half goes with this one; only the lower half drops the door. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    if (state.getValue(UPPER)) {
      BlockPos below = pos.down();
      if (world.getBlockState(below).getBlock() == this) {
        world.destroyBlock(below, true);
      }
    } else if (world.getBlockState(pos.up()).getBlock() == this) {
      world.setBlockToAir(pos.up());
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(UPPER) ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  /**
   * Whether a click is a partition piece being stacked on the door, to make a floor-to-ceiling
   * stall: the top of the upper half clicked with a partition piece in hand. That places the
   * piece rather than working the door, so the player need not sneak.
   */
  private static boolean stacksOnTop(IBlockState state, EntityPlayer player, EnumHand hand,
      EnumFacing side) {
    if (side != EnumFacing.UP || !state.getValue(UPPER)) {
      return false;
    }
    Item held = player.getHeldItem(hand).getItem();
    return held instanceof ItemBlock
        && ((ItemBlock) held).getBlock() instanceof BlockToiletPartition;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (player.isSneaking() || stacksOnTop(state, player, hand, side)) {
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
