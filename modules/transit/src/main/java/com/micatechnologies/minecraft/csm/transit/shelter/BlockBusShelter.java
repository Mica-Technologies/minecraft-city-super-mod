package com.micatechnologies.minecraft.csm.transit.shelter;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRotatableNSEW;
import com.micatechnologies.minecraft.csm.codeutils.RotationUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A bus shelter, two blocks tall, drawn by {@code gen_transit_shelters.py}: glass and steel, a
 * cantilever canopy or a flat roof ({@link BusShelterStyle}), in one agency's livery each. It is
 * placed as one piece into its block and the one above, as a door is, and broken as one piece
 * from either half; only the lower half drops the item.
 *
 * <p>Shelters join. Set side by side, facing the same way, they read as one shelter of any
 * length: {@link #LEFT} and {@link #RIGHT} say whether the same block continues on the sitter's
 * left and right, and end walls, end posts and the roof's end overhang are drawn only where it
 * does not. Set one in front of another, they make a deeper shelter: {@link #AHEAD} and
 * {@link #BEHIND} say whether it continues in front and behind, so the back wall and bench are
 * drawn only in the back row and the fascia only along the front. All four are actual state.</p>
 *
 * <p>The roof light is lit when the shelter is placed. A click with an empty hand switches it,
 * and so does a change of redstone power at either half -- on when power comes, off when it
 * goes, the Residential lamps' pattern -- so a daylight sensor or a switch works it. Either
 * switches the whole shelter the block is part of, up to {@link #REACH} blocks. Stored, in the
 * bits above the facing: {@link #UPPER}, then {@link #LIT} in the upper half and
 * {@link #POWERED} (whether redstone powered it when it last looked) in the lower.</p>
 *
 * <p>The glass shelter leaves the sitter's left end of its back row open, in an empty frame, for
 * a Signage ad panel set against it. Nothing here knows about that panel: the two meet only in
 * the world.</p>
 *
 * @since 2026.9
 */
public class BlockBusShelter extends AbstractBlockRotatableNSEW {

  /** Whether this is the upper half. */
  public static final PropertyBool UPPER = PropertyBool.create("upper");
  /** The roof light is on; stored in the upper half. */
  public static final PropertyBool LIT = PropertyBool.create("lit");
  /** Redstone powered the shelter when it last looked; stored in the lower half. */
  public static final PropertyBool POWERED = PropertyBool.create("powered");
  /** The shelter continues to the sitter's left. */
  public static final PropertyBool LEFT = PropertyBool.create("left");
  /** The shelter continues to the sitter's right. */
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  /** The shelter continues in front of this block: this is not its front row. */
  public static final PropertyBool AHEAD = PropertyBool.create("ahead");
  /** The shelter continues behind this block: this is not its back row. */
  public static final PropertyBool BEHIND = PropertyBool.create("behind");

  /** The most blocks of one shelter a switch reaches. */
  public static final int REACH = 64;

  /** The light a lit roof light gives. */
  private static final int LIGHT = 8;

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final BusShelterStyle style;

  /**
   * Constructs a shelter.
   *
   * @param registryName its registry name
   * @param style        its style
   */
  public BlockBusShelter(String registryName, BusShelterStyle style) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0.0F, 0);
    this.registryName = registryName;
    this.style = style;
    setDefaultState(getDefaultState().withProperty(UPPER, false).withProperty(LIT, false)
        .withProperty(POWERED, false).withProperty(LEFT, false).withProperty(RIGHT, false)
        .withProperty(AHEAD, false).withProperty(BEHIND, false));
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  /** The shelter's style. */
  public BusShelterStyle getStyle() {
    return style;
  }

  // --- state ---------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, UPPER, LIT, POWERED, LEFT, RIGHT, AHEAD,
        BEHIND);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    boolean upper = (meta & 4) != 0;
    boolean bit = (meta & 8) != 0;
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(UPPER, upper).withProperty(LIT, upper && bit)
        .withProperty(POWERED, !upper && bit);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    boolean upper = state.getValue(UPPER);
    boolean bit = upper ? state.getValue(LIT) : state.getValue(POWERED);
    return state.getValue(FACING).getHorizontalIndex() | (upper ? 4 : 0) | (bit ? 8 : 0);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    EnumFacing facing = state.getValue(FACING);
    boolean upper = state.getValue(UPPER);
    return state
        .withProperty(LEFT, continues(world, pos.offset(facing.rotateYCCW()), facing, upper))
        .withProperty(RIGHT, continues(world, pos.offset(facing.rotateY()), facing, upper))
        .withProperty(AHEAD, continues(world, pos.offset(facing), facing, upper))
        .withProperty(BEHIND, continues(world, pos.offset(facing.getOpposite()), facing, upper));
  }

  /** Whether the same shelter, facing the same way, the same half, is at {@code at}. */
  private boolean continues(IBlockAccess world, BlockPos at, EnumFacing facing, boolean upper) {
    IBlockState other = world.getBlockState(at);
    return other.getBlock() == this && other.getValue(FACING) == facing
        && other.getValue(UPPER) == upper;
  }

  // --- placing and breaking as one piece ---------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World world, @Nonnull BlockPos pos) {
    return pos.getY() < world.getHeight() - 1 && super.canPlaceBlockAt(world, pos)
        && world.getBlockState(pos.up()).getBlock().isReplaceable(world, pos.up());
  }

  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
  }

  /**
   * Places the upper half. Its light follows the shelter it joins, if it joins one, or is on.
   */
  @Override
  public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    super.onBlockPlacedBy(world, pos, state, placer, stack);
    boolean lit = true;
    EnumFacing facing = state.getValue(FACING);
    for (EnumFacing side : EnumFacing.HORIZONTALS) {
      IBlockState other = world.getBlockState(pos.up().offset(side));
      if (other.getBlock() == this && other.getValue(FACING) == facing
          && other.getValue(UPPER)) {
        lit = other.getValue(LIT);
        break;
      }
    }
    world.setBlockState(pos.up(), state.withProperty(UPPER, true).withProperty(LIT, lit)
        .withProperty(POWERED, false), 3);
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

  /** The other half goes with this one; an upper half broken alone breaks the lower. */
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

  // --- the roof light ------------------------------------------------------------------------

  @Override
  @SuppressWarnings("deprecation")
  public int getLightValue(@Nonnull IBlockState state) {
    return state.getValue(UPPER) && state.getValue(LIT) ? LIGHT : 0;
  }

  @Override
  public int getLightValue(@Nonnull IBlockState state, IBlockAccess world,
      @Nonnull BlockPos pos) {
    return getLightValue(state);
  }

  /** A click with an empty hand switches the shelter's light. */
  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (hand != EnumHand.MAIN_HAND || player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      BlockPos top = state.getValue(UPPER) ? pos : pos.up();
      IBlockState upper = world.getBlockState(top);
      boolean lit = upper.getBlock() == this && !upper.getValue(LIT);
      switchShelter(world, top, lit);
      world.playSound(null, pos, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.BLOCKS,
          0.3F, lit ? 0.6F : 0.5F);
    }
    return true;
  }

  /**
   * A change of redstone power at either half switches the shelter: on when power comes, off
   * when it goes. The lower half remembers the power it last saw.
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World world, BlockPos pos, Block block,
      BlockPos fromPos) {
    super.neighborChanged(state, world, pos, block, fromPos);
    if (world.isRemote) {
      return;
    }
    BlockPos lowerPos = state.getValue(UPPER) ? pos.down() : pos;
    IBlockState lower = world.getBlockState(lowerPos);
    if (lower.getBlock() != this || lower.getValue(UPPER)) {
      return;
    }
    boolean powered = world.isBlockPowered(lowerPos) || world.isBlockPowered(lowerPos.up());
    if (powered != lower.getValue(POWERED)) {
      world.setBlockState(lowerPos, lower.withProperty(POWERED, powered), 2);
      switchShelter(world, lowerPos.up(), powered);
    }
  }

  /**
   * Sets the light of every upper half of the shelter that {@code top} is part of: the same
   * block, facing the same way, joined side to side or front to back, up to {@link #REACH}.
   */
  private void switchShelter(World world, BlockPos top, boolean lit) {
    IBlockState start = world.getBlockState(top);
    if (start.getBlock() != this || !start.getValue(UPPER)) {
      return;
    }
    EnumFacing facing = start.getValue(FACING);
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> open = new ArrayDeque<>();
    open.add(top);
    seen.add(top);
    while (!open.isEmpty() && seen.size() <= REACH) {
      BlockPos at = open.poll();
      IBlockState here = world.getBlockState(at);
      if (here.getValue(LIT) != lit) {
        world.setBlockState(at, here.withProperty(LIT, lit), 3);
      }
      for (EnumFacing side : EnumFacing.HORIZONTALS) {
        BlockPos next = at.offset(side);
        if (!seen.contains(next) && world.isBlockLoaded(next)
            && continues(world, next, facing, true)) {
          seen.add(next);
          open.add(next);
        }
      }
    }
  }

  // --- shape -------------------------------------------------------------------------------

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  /** The walls, bench, posts and roof of this half, as {@link BusShelterStyle} lists them. */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState actual = isActualState ? state : state.getActualState(world, pos);
    EnumFacing facing = actual.getValue(FACING);
    List<AxisAlignedBB> boxes = new ArrayList<>(style.collision(actual.getValue(UPPER),
        actual.getValue(LEFT), actual.getValue(RIGHT), actual.getValue(AHEAD),
        actual.getValue(BEHIND)));
    for (AxisAlignedBB box : boxes) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          RotationUtils.rotateBoundingBoxByFacing(box, facing));
    }
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos,
      EnumFacing face) {
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return false;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return style != null ? style.getLayer() : BlockRenderLayer.TRANSLUCENT;
  }
}
