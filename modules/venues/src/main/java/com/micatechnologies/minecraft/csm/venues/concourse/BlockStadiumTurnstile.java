package com.micatechnologies.minecraft.csm.venues.concourse;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import com.micatechnologies.minecraft.csm.venues.VenuesSounds;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
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
 * The stadium turnstile ({@code stadium_turnstile}): a waist-high tripod turnstile, a stainless
 * cabinet down one side of the cell with three arms on a tilted hub reaching across the lane. It
 * lets people through one way only, from its front (the end with the green arrow) to its back:
 * walking in from the front the arm gives way, and the arms index round a third of a turn as you
 * pass (the {@code open} state, held while anyone is in the cell); from the back the arm is a
 * barrier.
 *
 * <p><b>Ticketed</b> (metadata bit 2) it is locked both ways, its indicator red, until a ticket
 * is scanned: a click on it with an unscanned {@link ItemVenueTicket} (its own reader), or a scan
 * at a {@link BlockTicketScanner} beside it. Then it opens for three seconds, or until the person
 * has gone through, for one way as above. A creative player's sneak-click with an empty hand
 * switches between free and ticketed.</p>
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3), towards the people coming in;
 * bit 2 ticketed; bit 3 open (the server's, set and cleared as people pass). Placed by hand it
 * faces the placer, free. Nothing ticks: a pass schedules one block update to close it.</p>
 *
 * @since 2026.10
 */
public class BlockStadiumTurnstile extends BlockVenue {

  /** Locked until a ticket is scanned. Stored, bit 2. */
  public static final PropertyBool TICKETED = PropertyBool.create("ticketed");
  /** Arms indexed round and the lane open one way. Stored, bit 3. */
  public static final PropertyBool OPEN = PropertyBool.create("open");

  /** How long a scanned ticket holds it open with nobody in it, in ticks. */
  static final int SCAN_OPEN_TICKS = 60;
  private static final int RECHECK_TICKS = 5;

  private static final AxisAlignedBB CABINET = BlockConcourseRun.box(0.5, 0, 1, 5, 16, 15);
  private static final AxisAlignedBB ARM = BlockConcourseRun.box(5, 0, 7, 16, 24, 9);
  private static final AxisAlignedBB OUTLINE = BlockConcourseRun.box(0.5, 0, 1, 16, 16, 15);

  /**
   * Constructs the turnstile.
   *
   * @param registryName its registry name
   */
  public BlockStadiumTurnstile(String registryName) {
    super(registryName, Material.IRON, SoundType.METAL, 2.0F);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(TICKETED, false).withProperty(OPEN, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, TICKETED, OPEN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(TICKETED, (meta & 4) != 0).withProperty(OPEN, (meta & 8) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(TICKETED) ? 4 : 0)
        | (state.getValue(OPEN) ? 8 : 0);
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return OUTLINE;
  }

  /**
   * How far an entity is through the lane along the way people pass, in blocks from the cell's
   * middle: negative before it (on the front, entry side), positive past it.
   */
  private static double progress(Entity entity, BlockPos pos, EnumFacing facing) {
    EnumFacing pass = facing.getOpposite();
    double dx = entity.posX - (pos.getX() + 0.5);
    double dz = entity.posZ - (pos.getZ() + 0.5);
    return dx * pass.getXOffset() + dz * pass.getZOffset();
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    EnumFacing facing = state.getValue(FACING);
    addCollisionBoxToList(pos, entityBox, collidingBoxes,
        RotationUtils.rotateBoundingBoxByFacing(CABINET, facing));
    boolean locked = state.getValue(TICKETED) && !state.getValue(OPEN);
    AxisAlignedBB arm = RotationUtils.rotateBoundingBoxByFacing(ARM, facing).offset(pos);
    // One way: the arm stops whoever stands past it (coming back) and gives way to whoever
    // comes from the front. It is never there for someone already overlapping it: the server
    // sends a player back if a move ends inside a box they were not in before, which is what
    // happens the tick their middle crosses the arm.
    if (locked || (entity != null && progress(entity, pos, facing) > 0.0
        && !entity.getEntityBoundingBox().intersects(arm))) {
      if (entityBox.intersects(arm)) {
        collidingBoxes.add(arm);
      }
    }
  }

  @Override
  public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
    if (world.isRemote || !(entity instanceof EntityLivingBase) || state.getValue(OPEN)) {
      return;
    }
    if (state.getValue(TICKETED) || progress(entity, pos, state.getValue(FACING)) > 0.0) {
      return;
    }
    // someone walking in from the front: the arms index round
    open(world, pos, state, RECHECK_TICKS);
  }

  /**
   * Opens the turnstile and schedules it to close.
   *
   * @param world the world
   * @param pos   the turnstile
   * @param state its state
   * @param ticks how long until it checks whether to close
   */
  void open(World world, BlockPos pos, IBlockState state, int ticks) {
    world.setBlockState(pos, state.withProperty(OPEN, true), 3);
    world.playSound(null, pos, VenuesSounds.VENUE_TURNSTILE.getSoundEvent(), SoundCategory.BLOCKS,
        0.7F, 1.0F);
    world.scheduleUpdate(pos, this, ticks);
  }

  @Override
  public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    if (world.isRemote || !state.getValue(OPEN)) {
      return;
    }
    if (!world.getEntitiesWithinAABB(EntityLivingBase.class, new AxisAlignedBB(pos)).isEmpty()) {
      world.scheduleUpdate(pos, this, RECHECK_TICKS);
      return;
    }
    world.setBlockState(pos, state.withProperty(OPEN, false), 3);
  }

  /** Opens a ticketed turnstile that is shut for one person, as a scanned ticket does. */
  boolean admit(World world, BlockPos pos, IBlockState state) {
    if (!state.getValue(TICKETED) || state.getValue(OPEN)) {
      return false;
    }
    open(world, pos, state, SCAN_OPEN_TICKS);
    return true;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack held = player.getHeldItem(hand);
    if (held.isEmpty()) {
      if (!player.isSneaking() || hand != EnumHand.MAIN_HAND) {
        return false;
      }
      if (!world.isRemote) {
        if (!player.capabilities.isCreativeMode) {
          player.sendStatusMessage(new TextComponentTranslation(
              "csm.venues.turnstile.creative_only"), true);
          return true;
        }
        boolean ticketed = !state.getValue(TICKETED);
        world.setBlockState(pos, state.withProperty(TICKETED, ticketed).withProperty(OPEN, false),
            3);
        player.sendStatusMessage(new TextComponentTranslation(ticketed
            ? "csm.venues.turnstile.ticketed" : "csm.venues.turnstile.free"), true);
      }
      return true;
    }
    if (!(held.getItem() instanceof ItemVenueTicket)) {
      return false;
    }
    if (!world.isRemote) {
      if (!state.getValue(TICKETED)) {
        player.sendStatusMessage(new TextComponentTranslation("csm.venues.turnstile.free"), true);
      } else if (ItemVenueTicket.isScanned(held)) {
        BlockTicketScanner.deny(world, pos, player, "csm.venues.ticket.used");
      } else if (admit(world, pos, state)) {
        ItemVenueTicket.scanOne(player, hand);
        world.playSound(null, pos, VenuesSounds.VENUE_SCANNER_ACCEPT.getSoundEvent(),
            SoundCategory.BLOCKS, 0.8F, 1.0F);
        player.sendStatusMessage(new TextComponentTranslation("csm.venues.ticket.accepted"),
            true);
      }
    }
    return true;
  }
}
