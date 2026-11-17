package com.micatechnologies.minecraft.csm.transit.fare;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The outdoor fare vending machine: a weatherproof street ticket machine on a short plinth, a
 * hood over its screen, stainless and graphite with a small band in an agency's colours, drawn by
 * {@code gen_transit_fare_vending_outdoor.py}. One block per livery, as the shelters are:
 * {@code fare_vending_machine_outdoor} (CITYLINE) and {@code fare_vending_machine_outdoor_<agency>}
 * for RIVERWAY, VERDANT and EMBERLINE; only their upper half's model differs. It sells exactly what the indoor
 * {@link BlockFareVendingMachine} does, through the same GUI ({@link #GUI_ID}), packets and
 * purchases: it <em>is</em> one, so the GUI provider's and the purchase handler's checks pass for
 * it unchanged.
 *
 * <p>It is two blocks, placed as one piece as a door is: the lower half ({@link #UPPER} false)
 * draws the plinth and the lower cabinet, the upper the screen, hood and cap. Placing the lower
 * places the upper (it needs the room); breaking either takes both, and only the lower drops the
 * machine, so one machine comes back however it is broken (by hand, in creative, or blown up).
 * Clicking either half opens the GUI for the lower. Stored: the facing (always horizontal) and
 * {@link #UPPER} (8).</p>
 *
 * @since 2026.11
 */
public class BlockFareVendingMachineOutdoor extends BlockFareVendingMachine {

  /** The upper half. Stored. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");

  /** The plinth and lower cabinet, facing north (the base class turns it). */
  private static final AxisAlignedBB LOWER_BOX =
      new AxisAlignedBB(1.5 / 16, 0, 3.5 / 16, 14.5 / 16, 1, 13.5 / 16);
  /** The upper cabinet, the hood standing out over the screen, and the cap. */
  private static final AxisAlignedBB UPPER_BOX =
      new AxisAlignedBB(1.5 / 16, 0, 2.0 / 16, 14.5 / 16, 13.5 / 16, 13.5 / 16);

  /** The registry name, while the base constructor (which asks for it) runs. */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;

  /**
   * A machine in one agency's livery.
   *
   * @param registryName {@code fare_vending_machine_outdoor} or
   *                     {@code fare_vending_machine_outdoor_<agency>}
   */
  public BlockFareVendingMachineOutdoor(String registryName) {
    this(stash(registryName));
  }

  private BlockFareVendingMachineOutdoor(Void stashed) {
    super();
    this.registryName = PENDING.get();
    PENDING.remove();
  }

  private static Void stash(String registryName) {
    PENDING.set(registryName);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, UPPER);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int facing = meta & 7;
    return getDefaultState()
        .withProperty(FACING, facing >= 2 && facing <= 5 ? EnumFacing.byIndex(facing)
            : EnumFacing.NORTH)
        .withProperty(UPPER, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getIndex() | (state.getValue(UPPER) ? 8 : 0);
  }

  /** Faces the placer, level. */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite())
        .withProperty(UPPER, false);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return state.getValue(UPPER) ? UPPER_BOX : LOWER_BOX;
  }

  // --- two blocks, as a door ------------------------------------------------------------------

  /** Needs room for its upper half. */
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

  /**
   * One half goes when the other does. The upper goes quietly; the lower, losing its upper
   * (broken by hand in survival), drops the machine, since the upper never does.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    if (state.getValue(UPPER)) {
      IBlockState below = world.getBlockState(pos.down());
      if (below.getBlock() != this || below.getValue(UPPER)) {
        world.setBlockToAir(pos);
      }
    } else {
      IBlockState above = world.getBlockState(pos.up());
      if (above.getBlock() != this || !above.getValue(UPPER)) {
        world.setBlockToAir(pos);
        if (!world.isRemote) {
          dropBlockAsItem(world, pos, state, 0);
        }
      }
    }
  }

  /**
   * Breaking the upper half in creative takes the lower first, quietly, so the lower does not
   * drop the machine when it finds its upper gone.
   */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (player.capabilities.isCreativeMode && state.getValue(UPPER)) {
      BlockPos below = pos.down();
      IBlockState lower = world.getBlockState(below);
      if (lower.getBlock() == this && !lower.getValue(UPPER)) {
        world.setBlockToAir(below);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /** Only the lower half drops the machine. */
  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(UPPER) ? Items.AIR : Item.getItemFromBlock(this);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  /** Either half opens the machine's GUI, for the lower. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    BlockPos lower = state.getValue(UPPER) ? pos.down() : pos;
    return super.onBlockActivated(world, lower, world.getBlockState(lower), player, hand,
        facing, hitX, hitY, hitZ);
  }
}
