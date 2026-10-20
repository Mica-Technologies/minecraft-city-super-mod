package com.micatechnologies.minecraft.csm.healthcare.wards;

import com.micatechnologies.minecraft.csm.healthcare.BlockHealthcare;
import com.micatechnologies.minecraft.csm.healthcare.HealthcareMaterial;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The foot cell every hospital bed shares ({@code hospital_bed_foot}, in the hidden tab). It
 * stores its facing only (metadata bits 0-1, S 0, W 1, N 2, E 3, the bed's facing) and reads the
 * rest -- which bed, its rails, its height -- from the head cell behind it
 * ({@code foot.offset(facing.getOpposite())}), so the two halves can never disagree. A foot with
 * no head behind it draws the standard bed with its rails down.
 *
 * <p>Its own name is the generic one ("Hospital Bed or Stretcher (Foot)"), since a block's name
 * has no world to read. Everything that names the foot a player is looking at -- pick block, and
 * the probes that name a block by its pick block -- gets the item of the bed or stretcher it
 * belongs to, read from its head's variant like its model.</p>
 *
 * @since 2026.10
 */
public class BlockHospitalBedFoot extends BlockHealthcare {

  /** Which bed the head behind this foot is. Actual state only. */
  public static final PropertyEnum<HospitalBedVariant> VARIANT =
      PropertyEnum.create("variant", HospitalBedVariant.class);

  private static final IProperty<?>[] ACTUAL = {VARIANT, BlockHospitalBed.RAILS,
      BlockHospitalBed.HIGH};

  private static final AxisAlignedBB LOW = new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0,
      11 / 16.0, 1);
  private static final AxisAlignedBB HIGH_BOX = new AxisAlignedBB(1 / 16.0, 0, 0, 15 / 16.0,
      15 / 16.0, 1);

  private static BlockHospitalBedFoot instance;

  public BlockHospitalBedFoot() {
    super("hospital_bed_foot", new int[]{1, 0, 0, 15, 11, 16}, HealthcareMaterial.METAL,
        Mount.FLOOR, true);
    instance = this;
  }

  /**
   * The foot block.
   *
   * @return the registered foot block
   */
  public static BlockHospitalBedFoot get() {
    return instance;
  }

  @Override
  protected IProperty<?>[] actualProperties() {
    return ACTUAL;
  }

  @Override
  protected IBlockState defaultExtras(IBlockState state) {
    return state.withProperty(VARIANT, HospitalBedVariant.WHITE)
        .withProperty(BlockHospitalBed.RAILS, false).withProperty(BlockHospitalBed.HIGH, false);
  }

  /** The head cell behind {@code foot}. */
  private static BlockPos headOf(BlockPos foot, EnumFacing facing) {
    return foot.offset(facing.getOpposite());
  }

  /** The head's state, or null if no head of the same facing is behind this foot. */
  private static IBlockState head(IBlockAccess world, BlockPos pos, IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    IBlockState h = world.getBlockState(headOf(pos, facing));
    if (h.getBlock() instanceof BlockHospitalBed && h.getValue(FACING) == facing) {
      return h;
    }
    return null;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess world,
      @Nonnull BlockPos pos) {
    IBlockState s = super.getActualState(state, world, pos);
    IBlockState h = head(world, pos, s);
    if (h == null) {
      return defaultExtras(s);
    }
    return s.withProperty(VARIANT, ((BlockHospitalBed) h.getBlock()).getVariant())
        .withProperty(BlockHospitalBed.RAILS, h.getValue(BlockHospitalBed.RAILS))
        .withProperty(BlockHospitalBed.HIGH, h.getValue(BlockHospitalBed.HIGH));
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(BlockHospitalBed.HIGH) ? HIGH_BOX : LOW;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    IBlockState h = head(world, pos, state);
    if (h == null) {
      return false;
    }
    return BlockHospitalBed.interact(world, headOf(pos, state.getValue(FACING)), h, player, side);
  }

  /** A creative player breaking the foot takes the head with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (player.capabilities.isCreativeMode && head(world, pos, state) != null) {
      world.setBlockToAir(headOf(pos, state.getValue(FACING)));
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /** The head goes with the foot, and drops the bed. */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos, @Nonnull IBlockState state) {
    if (head(world, pos, state) != null) {
      world.destroyBlock(headOf(pos, state.getValue(FACING)), true);
    }
    super.breakBlock(world, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return Items.AIR;
  }

  /**
   * The item of the bed or stretcher this foot belongs to: its head's variant, or the standard
   * bed it draws when no head is behind it.
   */
  private ItemStack bedItem(World world, BlockPos pos, IBlockState state) {
    HospitalBedVariant variant = getActualState(state, world, pos).getValue(VARIANT);
    BlockHospitalBed bed = BlockHospitalBed.of(variant);
    return bed == null ? ItemStack.EMPTY : new ItemStack(bed);
  }

  @Override
  @Nonnull
  public ItemStack getPickBlock(@Nonnull IBlockState state, RayTraceResult target,
      @Nonnull World world, @Nonnull BlockPos pos, EntityPlayer player) {
    return bedItem(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public ItemStack getItem(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    return bedItem(world, pos, state);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }
}
