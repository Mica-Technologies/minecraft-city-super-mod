package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bed that is slept in as a vanilla bed is: right-click at night to sleep (every player
 * asleep skips the night), and sleeping through the night sets the player's spawn beside it.
 * It fills two or four blocks ({@link BedLayout}: single, double or king, bunk, day bed),
 * placed as one from the block clicked -- the foot there and the head away from the placer, a
 * wide bed's second column to the placer's right -- and broken as one from any of them; only
 * cell 0 drops the item.
 *
 * <p>It works through Forge's bed hooks ({@link #isBed}, {@link #getBedDirection},
 * {@link #getBedSpawnPosition}, {@link #setBedOccupied}): the bed position a player sleeps at
 * is the head cell of their side (or tier). A bed in a dimension where beds do not work (the
 * Nether, the End) says so rather than exploding. A day bed is also a seat: by day, when no one
 * can sleep, a click sits on it instead.</p>
 *
 * <p>Vanilla lays a sleeper out only in a {@code BlockHorizontal}; this is not one, so the
 * server moves a sleeper onto the pillow and {@link BedSleepClientHandler} gives the body the
 * offset vanilla gives it in its own beds. The cell is {@link #getPart stored} with the facing,
 * in the two bits above it.</p>
 *
 * @since 2026.9
 */
public class BlockResidentialBed extends BlockResidentialFurniture {

  /** Which cell of a two-block bed this is. */
  public static final PropertyInteger PART2 = PropertyInteger.create("part", 0, 1);
  /** Which cell of a four-block bed this is. */
  public static final PropertyInteger PART4 = PropertyInteger.create("part", 0, 3);

  /** The status message for a bed that cannot be slept in here. */
  private static final String NOWHERE = "csm.furnishings.bed.nowhere";

  /** A day bed's seat: the top of its mattress, and a little forward of the middle. */
  private static final double DAY_SEAT_TOP = 7.25;
  private static final double DAY_SEAT_FORWARD = 1.5;

  private static final ThreadLocal<BedLayout> PENDING_LAYOUT = new ThreadLocal<>();

  private final BedLayout layout;
  private final AxisAlignedBB[] boxes;

  /**
   * Constructs a bed.
   *
   * @param registryName its registry name
   * @param layout       its shape
   * @param boxes        each cell's box facing north, in sixteenths: {x0, y0, z0, x1, y1, z1}
   * @param upholstered  whether it is upholstered (cloth) rather than a wooden frame
   */
  public BlockResidentialBed(String registryName, BedLayout layout, int[][] boxes,
      boolean upholstered) {
    super(stashLayout(registryName, layout), boxes[0], upholstered,
        layout == BedLayout.DAY ? DAY_SEAT_TOP : -16, DAY_SEAT_FORWARD, 0);
    this.layout = layout;
    this.boxes = new AxisAlignedBB[boxes.length];
    for (int i = 0; i < boxes.length; i++) {
      int[] b = boxes[i];
      this.boxes[i] = new AxisAlignedBB(b[0] / 16.0, b[1] / 16.0, b[2] / 16.0, b[3] / 16.0,
          b[4] / 16.0, b[5] / 16.0);
    }
    PENDING_LAYOUT.remove();
  }

  private static String stashLayout(String registryName, BedLayout layout) {
    PENDING_LAYOUT.set(layout);
    return registryName;
  }

  private BedLayout layout() {
    return layout != null ? layout : PENDING_LAYOUT.get();
  }

  /**
   * The bed's shape.
   *
   * @return its layout
   */
  public BedLayout getLayout() {
    return layout;
  }

  /**
   * The property holding which cell a block is.
   *
   * @return {@link #PART2} or {@link #PART4}
   */
  public PropertyInteger partProperty() {
    return layout().size() > 2 ? PART4 : PART2;
  }

  /**
   * Which cell of the bed {@code state} is.
   *
   * @param state a state of this block
   *
   * @return the cell
   */
  public int getPart(IBlockState state) {
    return state.getValue(partProperty());
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new CsmBlockStateContainer(this, FACING, partProperty());
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    int part = Math.min(meta >> 2, layout().size() - 1);
    return super.getStateFromMeta(meta & 3).withProperty(partProperty(), part);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return super.getMetaFromState(state) | (getPart(state) << 2);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boxes[getPart(state)];
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  protected ItemBlock createItemBlock() {
    return new ItemResidentialBed(this);
  }

  /**
   * Whether every cell a bed placed at {@code anchor} facing {@code facing} needs is free.
   *
   * @param world  the world
   * @param anchor where cell 0 goes
   * @param facing the way it faces
   *
   * @return true if it fits
   */
  public boolean fits(World world, BlockPos anchor, EnumFacing facing) {
    for (int p = 1; p < layout.size(); p++) {
      BlockPos c = layout.cellPos(anchor, facing, p);
      if (c.getY() >= world.getHeight()
          || !world.getBlockState(c).getBlock().isReplaceable(world, c)) {
        return false;
      }
    }
    return true;
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        .withProperty(partProperty(), 0);
  }

  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    EnumFacing facing = state.getValue(FACING);
    for (int p = 1; p < layout.size(); p++) {
      world.setBlockState(layout.cellPos(pos, facing, p),
          state.withProperty(partProperty(), p), 3);
    }
  }

  /** Whether {@code pos} holds this bed's cell {@code part}, facing {@code facing}. */
  private boolean isCell(IBlockAccess world, BlockPos pos, EnumFacing facing, int part) {
    IBlockState s = world.getBlockState(pos);
    return s.getBlock() == this && s.getValue(FACING) == facing && getPart(s) == part;
  }

  /** A creative player breaking any other cell takes cell 0 with it, dropping nothing. */
  @Override
  public void onBlockHarvested(World world, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    int part = getPart(state);
    if (part != 0 && player.capabilities.isCreativeMode) {
      EnumFacing facing = state.getValue(FACING);
      BlockPos anchor = layout.anchor(pos, facing, part);
      if (isCell(world, anchor, facing, 0)) {
        world.setBlockToAir(anchor);
      }
    }
    super.onBlockHarvested(world, pos, state, player);
  }

  /**
   * The rest of the bed goes with this cell: cell 0 clears the others; any other cell breaks
   * cell 0, which drops the bed and clears the rest.
   */
  @Override
  public void breakBlock(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull IBlockState state) {
    EnumFacing facing = state.getValue(FACING);
    int part = getPart(state);
    BlockPos anchor = layout.anchor(pos, facing, part);
    if (part != 0) {
      if (isCell(world, anchor, facing, 0)) {
        world.destroyBlock(anchor, true);
      }
    } else {
      for (int p = 1; p < layout.size(); p++) {
        BlockPos c = layout.cellPos(anchor, facing, p);
        if (isCell(world, c, facing, p)) {
          world.setBlockToAir(c);
        }
      }
    }
    super.breakBlock(world, pos, state);
  }

  /** A cell whose cell 0 has gone (a command, a piston) goes too. */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    int part = getPart(state);
    if (part != 0) {
      EnumFacing facing = state.getValue(FACING);
      if (!isCell(world, layout.anchor(pos, facing, part), facing, 0)) {
        world.setBlockToAir(pos);
      }
    }
    super.neighborChanged(state, world, pos, block, fromPos);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return getPart(state) != 0 ? Items.AIR : super.getItemDropped(state, rand, fortune);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public EnumPushReaction getPushReaction(@Nonnull IBlockState state) {
    return EnumPushReaction.BLOCK;
  }

  // --- sleeping -------------------------------------------------------------------------

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (world.isRemote) {
      return true;
    }
    EnumFacing facing = state.getValue(FACING);
    int part = getPart(state);
    int spot = layout.spotOf(part);
    BlockPos head = layout.cellPos(layout.anchor(pos, facing, part), facing,
        layout.headOf(spot));
    if (!isCell(world, head, facing, layout.headOf(spot))) {
      return true;
    }
    if (!world.provider.canRespawnHere() || world.getBiome(head) == Biomes.HELL) {
      // Our beds are furniture, not explosives: say so and do nothing.
      return hasSeat() ? sit(world, pos, state, player) : message(player, NOWHERE);
    }
    if (occupied(world, head)) {
      return hasSeat() ? sit(world, pos, state, player) : message(player, "tile.bed.occupied");
    }
    switch (player.trySleep(head)) {
      case OK:
        settle(head, facing, spot, player);
        return true;
      case NOT_POSSIBLE_NOW:
        return hasSeat() ? sit(world, pos, state, player) : message(player, "tile.bed.noSleep");
      case NOT_SAFE:
        return message(player, "tile.bed.notSafe");
      case TOO_FAR_AWAY:
        return message(player, "tile.bed.tooFarAway");
      case NOT_POSSIBLE_HERE:
        return message(player, NOWHERE);
      default:
        return true;
    }
  }

  private static boolean message(EntityPlayer player, String key) {
    player.sendStatusMessage(new TextComponentTranslation(key), true);
    return true;
  }

  /** Whether someone already sleeps with their head in {@code head}. */
  private static boolean occupied(World world, BlockPos head) {
    for (EntityPlayer other : world.playerEntities) {
      if (other.isPlayerSleeping() && head.equals(other.bedLocation)) {
        return true;
      }
    }
    return false;
  }

  /** Moves a player who has just lain down onto the pillow, on the server and their client. */
  private void settle(BlockPos head, EnumFacing facing, int spot, EntityPlayer player) {
    Vec3d at = layout.pillow(head, facing, spot);
    if (player instanceof EntityPlayerMP) {
      ((EntityPlayerMP) player).connection.setPlayerLocation(at.x, at.y, at.z,
          player.rotationYaw, player.rotationPitch);
    } else {
      player.setPosition(at.x, at.y, at.z);
    }
  }

  /**
   * Where a sleeper whose bed position is {@code head} lies, or null if it is not a head cell
   * of this bed.
   *
   * @param state the state at {@code head}
   * @param head  the bed position
   *
   * @return the point, or null
   */
  @Nullable
  public Vec3d pillow(IBlockState state, BlockPos head) {
    int part = getPart(state);
    if (!layout.isHead(part)) {
      return null;
    }
    return layout.pillow(head, state.getValue(FACING), layout.spotOf(part));
  }

  @Override
  public boolean isBed(IBlockState state, IBlockAccess world, BlockPos pos,
      @Nullable Entity player) {
    return true;
  }

  @Override
  public boolean isBedFoot(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == this && !layout.isHead(getPart(state));
  }

  @Override
  @Nonnull
  public EnumFacing getBedDirection(IBlockState state, IBlockAccess world, BlockPos pos) {
    IBlockState s = state.getBlock() == this ? state : world.getBlockState(pos);
    if (s.getBlock() != this) {
      return EnumFacing.NORTH;
    }
    return layout.direction(s.getValue(FACING), layout.spotOf(getPart(s)));
  }

  /** Nothing is stored: whether a bed is taken is read off the players sleeping in it. */
  @Override
  public void setBedOccupied(IBlockAccess world, BlockPos pos, EntityPlayer player,
      boolean occupied) {
  }

  /**
   * Where a player wakes, or respawns: a free spot on the floor beside the bed, the foot end
   * first, then its sides, then the head end.
   */
  @Nullable
  @Override
  public BlockPos getBedSpawnPosition(IBlockState state, IBlockAccess world, BlockPos pos,
      @Nullable EntityPlayer player) {
    IBlockState s = world.getBlockState(pos);
    if (!(world instanceof World) || s.getBlock() != this) {
      return null;
    }
    EnumFacing facing = s.getValue(FACING);
    BlockPos anchor = layout.anchor(pos, facing, getPart(s));
    List<BlockPos> floor = new ArrayList<>();
    for (int p = 0; p < layout.size(); p++) {
      if (layout.onFloor(p)) {
        floor.add(layout.cellPos(anchor, facing, p));
      }
    }
    EnumFacing[] order = {facing, facing.rotateY(), facing.rotateYCCW(), facing.getOpposite()};
    for (EnumFacing d : order) {
      for (BlockPos c : floor) {
        BlockPos n = c.offset(d);
        if (floor.contains(n)) {
          continue;
        }
        for (BlockPos at : new BlockPos[]{n, n.down(), n.up()}) {
          if (safe((World) world, at)) {
            return at;
          }
        }
      }
    }
    return null;
  }

  private static boolean safe(World world, BlockPos pos) {
    return world.getBlockState(pos.down()).isTopSolid()
        && !world.getBlockState(pos).getMaterial().isSolid()
        && !world.getBlockState(pos.up()).getMaterial().isSolid();
  }

  /**
   * The bed's item: places the whole bed, and only where every block of it is free.
   */
  public static class ItemResidentialBed extends ItemBlock {

    /**
     * Constructs the item.
     *
     * @param block the bed
     */
    public ItemResidentialBed(BlockResidentialBed block) {
      super(block);
    }

    @Override
    public boolean placeBlockAt(@Nonnull ItemStack stack, @Nonnull EntityPlayer player,
        World world, @Nonnull BlockPos pos, EnumFacing side, float hitX, float hitY,
        float hitZ, @Nonnull IBlockState newState) {
      BlockResidentialBed bed = (BlockResidentialBed) block;
      EnumFacing facing = newState.getValue(FACING);
      if (!bed.fits(world, pos, facing)) {
        return false;
      }
      for (int p = 1; p < bed.getLayout().size(); p++) {
        if (!player.canPlayerEdit(bed.getLayout().cellPos(pos, facing, p), side, stack)) {
          return false;
        }
      }
      return super.placeBlockAt(stack, player, world, pos, side, hitX, hitY, hitZ, newState);
    }
  }
}
