package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.VenuesSounds;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * The ticket scanner pedestal ({@code stadium_ticket_scanner}): a stainless post with a slanted
 * head, its scan window and status ring facing the person coming in. A click with an unscanned
 * {@link ItemVenueTicket} scans it (the ring goes green, an accept tone) and opens the nearest
 * shut ticketed {@link BlockStadiumTurnstile} within two blocks on its level for one person; a
 * stub or anything else is refused (the ring goes red, a low buzz). The light goes back to idle
 * a second later.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the reader; bits 2-3 the
 * ring: 0 idle, 1 accepted, 2 refused (the server's).</p>
 *
 * @since 2026.10
 */
public class BlockTicketScanner extends BlockVenue {

  /** The status ring. Stored, bits 2-3. */
  public static final PropertyInteger RESULT = PropertyInteger.create("result", 0, 2);

  private static final AxisAlignedBB BOX = BlockConcourseRun.box(5, 0, 5, 11, 18, 11);
  private static final int SHOW_TICKS = 20;
  private static final int REACH = 2;

  /**
   * Constructs the scanner.
   *
   * @param registryName its registry name
   */
  public BlockTicketScanner(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 1.5F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(RESULT, 0));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, RESULT);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(RESULT, Math.min(2, (meta >> 2) & 3));
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | state.getValue(RESULT) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOX;
  }

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(RESULT) != 0 ? 5 : 2;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    if (world.isRemote) {
      return true;
    }
    ItemStack held = player.getHeldItem(hand);
    if (!ItemVenueTicket.isUnscanned(held)) {
      show(world, pos, state, 2);
      deny(world, pos, player, held.getItem() instanceof ItemVenueTicket
          ? "csm.venues.ticket.used" : "csm.venues.ticket.none");
      return true;
    }
    BlockPos gate = nearestShutTurnstile(world, pos);
    if (gate == null) {
      show(world, pos, state, 2);
      deny(world, pos, player, "csm.venues.ticket.no_gate");
      return true;
    }
    IBlockState gs = world.getBlockState(gate);
    ((BlockStadiumTurnstile) gs.getBlock()).admit(world, gate, gs);
    ItemVenueTicket.scanOne(player, hand);
    show(world, pos, state, 1);
    world.playSound(null, pos, VenuesSounds.VENUE_SCANNER_ACCEPT.getSoundEvent(),
        SoundCategory.BLOCKS, 0.8F, 1.0F);
    player.sendStatusMessage(new TextComponentTranslation("csm.venues.ticket.accepted"), true);
    return true;
  }

  /** The nearest ticketed turnstile that is shut, within reach on this level, or null. */
  private static BlockPos nearestShutTurnstile(World world, BlockPos pos) {
    BlockPos best = null;
    int bestD = Integer.MAX_VALUE;
    for (int dx = -REACH; dx <= REACH; dx++) {
      for (int dz = -REACH; dz <= REACH; dz++) {
        BlockPos at = pos.add(dx, 0, dz);
        IBlockState s = world.getBlockState(at);
        if (s.getBlock() instanceof BlockStadiumTurnstile
            && s.getValue(BlockStadiumTurnstile.TICKETED)
            && !s.getValue(BlockStadiumTurnstile.OPEN)) {
          int d = dx * dx + dz * dz;
          if (d < bestD) {
            bestD = d;
            best = at;
          }
        }
      }
    }
    return best;
  }

  private void show(World world, BlockPos pos, IBlockState state, int result) {
    world.setBlockState(pos, state.withProperty(RESULT, result), 3);
    world.scheduleUpdate(pos, this, SHOW_TICKS);
  }

  /**
   * Refuses a ticket: the low buzz and the reason on the player's action bar.
   *
   * @param world  the world
   * @param pos    where the buzz comes from
   * @param player the player
   * @param key    the reason's lang key
   */
  static void deny(World world, BlockPos pos, EntityPlayer player, String key) {
    world.playSound(null, pos, VenuesSounds.VENUE_SCANNER_DENY.getSoundEvent(),
        SoundCategory.BLOCKS, 0.7F, 1.0F);
    player.sendStatusMessage(new TextComponentTranslation(key), true);
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (!world.isRemote && state.getValue(RESULT) != 0) {
      world.setBlockState(pos, state.withProperty(RESULT, 0), 3);
    }
  }
}
