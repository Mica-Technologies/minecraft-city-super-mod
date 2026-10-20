package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.codeutils.EntityCsmSeat;
import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import javax.annotation.Nonnull;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A hospital bed's head cell. A bed is one block wide and two long like a vanilla bed: this
 * head cell, which carries the item and the bed's state, and a {@link BlockHospitalBedFoot} cell
 * in front of it. The head goes against the wall (under the patient headwall) and the foot faces
 * the room. The transport stretchers ({@code hospital_stretcher}, {@code _trauma}) are beds
 * here too: the same two cells, metadata, clicks and pad heights, drawn as a stretcher.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), the way the foot end faces, the
 * same as the headwall above it; bit 2 the side rails up; bit 3 the bed raised to its high
 * position. The foot cell is at {@code head.offset(facing)} and stores its facing only.</p>
 *
 * <p>The item places the foot where it is used and the head one block beyond it, away from the
 * placer. A generator places the two cells with {@code /setblock}, in either order; nothing
 * removes a cell whose partner is missing, so a half-built bed waits for its other half. Breaking
 * either cell breaks the bed, and only the head drops it.</p>
 *
 * <p>Clicks: a long side raises or lowers the rails, sneaking and clicking raises or lowers the
 * bed, anywhere else sits the player up in it on Core's {@link EntityCsmSeat}, facing the
 * foot.</p>
 *
 * @since 2026.10
 */
public class BlockHospitalBed extends BlockHealthcare {

  /** The side rails are up. */
  public static final PropertyBool RAILS = PropertyBool.create("rails");
  /** The bed is raised to its working height. */
  public static final PropertyBool HIGH = PropertyBool.create("high");

  private static final PropertyBool[] FLAGS = {RAILS, HIGH};

  /** The mattress top above the floor, in sixteenths: lowered and raised. */
  public static final double MATTRESS_LOW = 9;
  public static final double MATTRESS_HIGH = 13;

  private static final AxisAlignedBB LOW = new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0,
      11 / 16.0, 1);
  private static final AxisAlignedBB HIGH_BOX = new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0,
      15 / 16.0, 1);

  private final HospitalBedVariant variant;

  /**
   * Constructs a bed's head block.
   *
   * @param variant which bed
   */
  public BlockHospitalBed(HospitalBedVariant variant) {
    super(variant.registryName(), new int[]{1, 0, 0, 15, 11, 16}, HealthcareMaterial.METAL,
        Mount.FLOOR, true);
    this.variant = variant;
  }

  public HospitalBedVariant getVariant() {
    return variant;
  }

  @Override
  protected PropertyBool[] storedFlags() {
    return FLAGS;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(RAILS, true);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(HIGH) ? HIGH_BOX : LOW;
  }

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemHospitalBed(this);
  }

  /**
   * Where the foot cell of a bed whose head is at {@code head} is.
   *
   * @param head   the head cell
   * @param facing the bed's facing
   *
   * @return the foot cell
   */
  public static BlockPos footOf(BlockPos head, EnumFacing facing) {
    return head.offset(facing);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    EnumFacing facing = state.getValue(FACING);
    world.setBlockState(footOf(pos, facing), BlockHospitalBedFoot.get().getDefaultState()
        .withProperty(FACING, facing), 3);
  }

  /** The foot goes with the head. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    BlockPos foot = footOf(pos, facing);
    IBlockState f = world.getBlockState(foot);
    if (f.getBlock() instanceof BlockHospitalBedFoot && f.getValue(FACING) == facing) {
      world.setBlockToAir(foot);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    return interact(world, pos, state, player, side);
  }

  /**
   * What a click on either cell of the bed does, worked out at the head.
   *
   * @param world  the world
   * @param head   the head cell
   * @param state  the head's state
   * @param player who clicked
   * @param side   the face clicked
   *
   * @return true: the click is used
   */
  public static boolean interact(World world, BlockPos head, IBlockState state,
      EntityPlayer player, EnumFacing side) {
    if (world.isRemote) {
      return true;
    }
    EnumFacing facing = state.getValue(FACING);
    if (player.isSneaking()) {
      world.setBlockState(head, state.cycleProperty(HIGH), 3);
    } else if (side.getAxis().isHorizontal() && side.getAxis() != facing.getAxis()) {
      world.setBlockState(head, state.cycleProperty(RAILS), 3);
    } else {
      double top = (state.getValue(HIGH) ? MATTRESS_HIGH : MATTRESS_LOW) / 16.0;
      EnumFacing beside = facing.rotateYCCW();
      BlockPos out = head.offset(beside);
      // the seat a little towards the foot, so the patient sits up against the raised back
      double sx = head.getX() + 0.5 + facing.getXOffset() * 0.15;
      double sz = head.getZ() + 0.5 + facing.getZOffset() * 0.15;
      EntityCsmSeat.sit(world, head, new AxisAlignedBB(head), "csm.healthcare.seat.taken", sx,
          head.getY() + 0.1, sz, top - 0.22, facing, out.getX() + 0.5, out.getY(),
          out.getZ() + 0.5, player);
    }
    return true;
  }

  /**
   * The bed's item: places the foot where it is used and the head beyond it, and only where
   * both are free.
   */
  public static class ItemHospitalBed extends ItemBlock {

    public ItemHospitalBed(BlockHospitalBed block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY, float hitZ,
        @Nonnull IBlockState newState) {
      EnumFacing facing = newState.getValue(FACING);
      BlockPos head = pos.offset(facing.getOpposite());
      if (!world.getBlockState(head).getBlock().isReplaceable(world, head)
          || !player.canPlayerEdit(head, side, stack)) {
        return false;
      }
      return super.placeBlockAt(stack, player, world, head, side, hitX, hitY, hitZ, newState);
    }
  }
}
