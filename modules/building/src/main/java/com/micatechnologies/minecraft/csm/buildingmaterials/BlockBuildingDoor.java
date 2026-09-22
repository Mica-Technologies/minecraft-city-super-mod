package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A door: interior wood, hollow metal, fire, exit, storefront or residential. One class,
 * constructed by registry name ({@code door_<style>_<colour>}).
 *
 * <p>Two blocks, a lower and an upper half, as a vanilla door is, and the state is split between
 * them the same way: the lower half stores the facing (the way to the inside: the way the player
 * faced when placing it, from outside) and whether it is open; the upper half stores the hinge
 * side, whether a {@link ItemDoorCloser door closer} is fitted, and whether the door is
 * {@link #REVERSED reversed} ({@link DoorSwingDirection}). Each half reads the rest from the other
 * as actual state. Most kinds hang as a vanilla door does, the leaf along the outside face of the
 * cell, and swing inward; the exit, storefront and fire doors swing outward, as real ones do, with
 * the leaf along the inside face ({@link #outswing()}); any placed door may be reversed from its
 * kind's default ({@link #outward}), by sneaking as it is placed or with the
 * {@link ItemDoorSwingTool door swing tool}.</p>
 *
 * <ul>
 *   <li><b>At rest</b> a door is baked models: no tile entity. Only while it swings does its
 *   upper half have one, {@link TileEntityDoorSwing}, on the clients only, whose renderer turns
 *   the closed model about the hinge and which takes itself away when the swing is over. Whether
 *   the swing is drawn or the door snaps is each client's own choice ({@code animateDoors}).</li>
 *   <li><b>Pairs.</b> A door placed beside another hinges on its far side, turning a shut
 *   neighbour round if need be, so the two meet at the latch; a pair opens and closes
 *   together.</li>
 *   <li><b>Redstone</b> holds a door open while it is powered.</li>
 *   <li><b>A door closer</b>, fitted with the item, shuts the door three seconds after it is
 *   opened. Sneak-clicking the door with an empty hand takes it off again.</li>
 *   <li><b>A keypad</b> linked to the door locks it: it then opens from outside only with the
 *   code, and from inside as ever. Locks are kept in {@link DoorLocks}, since no state bit is
 *   left.</li>
 * </ul>
 *
 * <p>The models come from {@code dev-env-utils/scripts/gen_doors.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockBuildingDoor extends AbstractBlock implements ICsmTileEntityProvider {

  /**
   * Which half of the door a block is.
   *
   * @since 1.0
   */
  public enum Half implements IStringSerializable {
    LOWER, UPPER;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  /**
   * Which side the hinges are on, seen from outside.
   *
   * @since 1.0
   */
  public enum Hinge implements IStringSerializable {
    LEFT, RIGHT;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }
  }

  public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);
  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyBool OPEN = PropertyBool.create("open");
  public static final PropertyEnum<Hinge> HINGE = PropertyEnum.create("hinge", Hinge.class);
  public static final PropertyBool CLOSER = PropertyBool.create("closer");
  /**
   * Whether the door is swinging. Actual state only, never stored: it is whether the upper half
   * has a {@link TileEntityDoorSwing}, and the models draw nothing while it is true.
   */
  public static final PropertyBool SWING = PropertyBool.create("swing");
  /**
   * Whether the door swings the other way from its kind's default: out for most doors, in for the
   * exit, storefront and fire doors. Stored in the upper half, in the bit that used to say it was
   * swinging.
   */
  public static final PropertyBool REVERSED = PropertyBool.create("reversed");

  /** How long a swing takes, in ticks. SHARED with {@link TileEntityDoorSwingRenderer}. */
  static final int SWING_TICKS = 8;
  /** How long a door closer holds the door open, in ticks. */
  private static final int CLOSER_TICKS = 60;
  /** The block event that tells every client a door has started to swing (param 1 opening). */
  private static final int EVENT_SWING = 2;

  /** SHARED with gen_doors: the leaf's thickness, and so its plane, in blocks. */
  private static final double LEAF = 1.75 / 16;
  private static final AxisAlignedBB CLOSED_NORTH = new AxisAlignedBB(0, 0, 1 - LEAF, 1, 1, 1);
  private static final AxisAlignedBB CLOSED_OUT_NORTH = new AxisAlignedBB(0, 0, 0, 1, 1, LEAF);
  private static final AxisAlignedBB OPEN_LEFT_NORTH = new AxisAlignedBB(0, 0, 0, LEAF, 1, 1);
  private static final AxisAlignedBB OPEN_RIGHT_NORTH =
      new AxisAlignedBB(1 - LEAF, 0, 0, 1, 1, 1);

  /** Which doors were last seen powered, per world, to act only when the signal changes. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();
  private static final ThreadLocal<Hinge> PENDING_HINGE = new ThreadLocal<>();
  private static final ThreadLocal<Boolean> PENDING_REVERSED = new ThreadLocal<>();
  /** A neighbour to turn round to pair with the door being placed, or null. */
  private static final ThreadLocal<BlockPos> PENDING_FLIP = new ThreadLocal<>();

  private final String registryName;

  /**
   * Constructs a {@link BlockBuildingDoor}.
   *
   * @param registryName {@code door_<style>_<colour>}
   *
   * @since 1.0
   */
  public BlockBuildingDoor(String registryName) {
    super(pendingMaterial(registryName), metal(registryName) ? SoundType.METAL : SoundType.WOOD,
        metal(registryName) ? "pickaxe" : "axe", 0, 3F, 15F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(HALF, Half.LOWER)
        .withProperty(FACING, EnumFacing.NORTH).withProperty(OPEN, false)
        .withProperty(HINGE, Hinge.LEFT).withProperty(CLOSER, false).withProperty(SWING, false)
        .withProperty(REVERSED, false));
    PENDING_REGISTRY_NAME.remove();
  }

  private static boolean metal(String registryName) {
    return registryName.contains("metal") || registryName.contains("storefront")
        || registryName.contains("trailer");
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return metal(registryName) ? Material.IRON : Material.WOOD;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /**
   * Whether this kind of door swings out, toward the outside, unless a placed one is
   * {@link #REVERSED reversed}. Which way a particular door swings is {@link #outward}.
   *
   * @return whether the kind swings out by default
   *
   * @since 1.0
   */
  public boolean outswing() {
    return DoorSwingDirection.outswingKind(getBlockRegistryName());
  }

  /**
   * Whether this placed door swings out, toward the outside, rather than in: its kind's default,
   * the other way if it is reversed. A door that swings out hangs along the inside face of the
   * cell -- the depth mirror of one that swings in -- and turns outward about a pivot there, so the
   * open leaf lies along the jamb inside its own cell either way. Nothing else changes: the facing
   * is still the way to the inside, and the inside is still the side a keypad lock lets out freely.
   *
   * @param door the whole door's state ({@link #whole}), which carries the upper half's bit
   *
   * @return whether the door swings out
   *
   * @since 1.1
   */
  public boolean outward(IBlockState door) {
    return DoorSwingDirection.outward(outswing(), door.getValue(REVERSED));
  }

  /**
   * Whether a door of this kind can be made to swing the other way. A custom door's movement is
   * set in the Door Workshop instead.
   *
   * @return whether the direction can be reversed
   *
   * @since 1.1
   */
  protected boolean reversible() {
    return true;
  }

  protected boolean glazed() {
    String n = getBlockRegistryName();
    return n.contains("lite") || n.contains("fire") || n.contains("storefront")
        || n.contains("glass") || n.contains("trailer");
  }

  // --- state --------------------------------------------------------------------------------------

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, HALF, FACING, OPEN, HINGE, CLOSER, SWING, REVERSED);
  }

  /**
   * The upper half's bit 4 is {@link #REVERSED}; it said "swinging" before any door could be
   * reversed, and {@link TileEntityDoorSwing#update} puts right the rare door saved mid-swing by
   * that version. {@link #SWING} is never stored.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    if (DoorSwingDirection.isUpper(meta)) {
      return getDefaultState().withProperty(HALF, Half.UPPER)
          .withProperty(HINGE, DoorSwingDirection.hingeRight(meta) ? Hinge.RIGHT : Hinge.LEFT)
          .withProperty(CLOSER, DoorSwingDirection.closer(meta))
          .withProperty(REVERSED, DoorSwingDirection.reversed(meta));
    }
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(OPEN, (meta & 4) != 0);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    if (state.getValue(HALF) == Half.UPPER) {
      return DoorSwingDirection.upperMeta(state.getValue(HINGE) == Hinge.RIGHT,
          state.getValue(CLOSER), state.getValue(REVERSED));
    }
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(OPEN) ? 4 : 0);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    BlockPos upperPos = pos;
    if (state.getValue(HALF) == Half.LOWER) {
      upperPos = pos.up();
      IBlockState up = worldIn.getBlockState(upperPos);
      if (up.getBlock() == this && up.getValue(HALF) == Half.UPPER) {
        state = state.withProperty(HINGE, up.getValue(HINGE))
            .withProperty(CLOSER, up.getValue(CLOSER))
            .withProperty(REVERSED, up.getValue(REVERSED));
      }
    } else {
      IBlockState down = worldIn.getBlockState(pos.down());
      if (down.getBlock() == this && down.getValue(HALF) == Half.LOWER) {
        state = state.withProperty(FACING, down.getValue(FACING))
            .withProperty(OPEN, down.getValue(OPEN));
      }
    }
    // A chunk being built reads through a ChunkCache, whose getTileEntity only looks; a world's
    // would try to make one, which for a door is a no-op (createNewTileEntity makes none).
    return state.withProperty(SWING,
        worldIn.getTileEntity(upperPos) instanceof TileEntityDoorSwing);
  }

  /**
   * The whole door's state, read from wherever {@code pos} is in it. If there is no door at
   * {@code pos} -- the other half of one that has lost a half -- the default state: a shut door,
   * which nothing then finds to open.
   */
  protected IBlockState whole(IBlockAccess world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    return state.getBlock() == this ? getActualState(state, world, pos) : getDefaultState();
  }

  protected static BlockPos lower(IBlockState state, BlockPos pos) {
    return state.getValue(HALF) == Half.LOWER ? pos : pos.down();
  }

  // --- placing and breaking -------------------------------------------------------------------------

  @Override
  public boolean canPlaceBlockAt(World worldIn, @Nonnull BlockPos pos) {
    return pos.getY() < worldIn.getHeight() - 1 && super.canPlaceBlockAt(worldIn, pos)
        && worldIn.getBlockState(pos.up()).getBlock().isReplaceable(worldIn, pos.up());
  }

  /**
   * The lower half, facing the way the player looks -- a door is hung from outside, looking in.
   * The hinge and the swing are decided here and handed to {@link #onBlockPlacedBy}, which puts up
   * the upper half that stores them.
   *
   * <p>A door placed beside another facing the same way, not already one of a pair, makes a pair
   * with it: it hinges on its far side, so the two latches meet in the middle. A neighbour hinged
   * on the shared side is turned round to match, if it is shut -- otherwise the two would stand
   * hinge to hinge, opening apart, their handles on the outer edges. A neighbour already hinged on
   * its far side is preferred, since it needs no change. With no neighbour to pair with, the hinge
   * goes on the side of the opening the player clicked. The door swings its kind's way, or the
   * other way if the player is sneaking -- unless it makes a pair, when it swings the way its
   * partner does, since a pair opens together.</p>
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing f = placer.getHorizontalFacing();
    EnumFacing left = f.rotateYCCW();
    BlockPos onLeft = pos.offset(left);
    BlockPos onRight = pos.offset(left.getOpposite());
    Hinge hinge;
    boolean reversed = reversible() && placer.isSneaking();
    BlockPos flip = null;
    // A partner on the left leaves this door hinged right, and one on the right hinged left.
    BlockPos mate = pairable(worldIn, onLeft, f, Hinge.LEFT, false) ? onLeft
        : pairable(worldIn, onRight, f, Hinge.RIGHT, false) ? onRight
            : pairable(worldIn, onLeft, f, Hinge.LEFT, true) ? onLeft
                : pairable(worldIn, onRight, f, Hinge.RIGHT, true) ? onRight : null;
    if (mate != null) {
      hinge = mate == onLeft ? Hinge.RIGHT : Hinge.LEFT;
      IBlockState door = whole(worldIn, mate);
      reversed = door.getValue(REVERSED);
      if (door.getValue(HINGE) == hinge) {
        flip = mate;
      }
    } else {
      double along = (hitX - 0.5) * left.getXOffset() + (hitZ - 0.5) * left.getZOffset();
      hinge = along > 0 ? Hinge.LEFT : Hinge.RIGHT;
    }
    PENDING_HINGE.set(hinge);
    PENDING_REVERSED.set(reversed);
    PENDING_FLIP.set(flip);
    return getDefaultState().withProperty(FACING, f);
  }

  /**
   * Whether the door whose lower half is at {@code pos} could pair with one placed beside it.
   *
   * @param world     the world
   * @param pos       where a neighbour's lower half may be
   * @param f         the facing of the door being placed
   * @param farHinge  the neighbour's hinge that keeps it on the far side of the new door
   * @param allowFlip whether a neighbour hinged the other way counts, to be turned round
   *
   * @return whether it pairs
   */
  private boolean pairable(World world, BlockPos pos, EnumFacing f, Hinge farHinge,
      boolean allowFlip) {
    IBlockState state = world.getBlockState(pos);
    if (state.getBlock() != this || state.getValue(HALF) != Half.LOWER) {
      return false;
    }
    IBlockState door = whole(world, pos);
    if (door.getValue(FACING) != f || partner(world, pos, door) != null) {
      return false;
    }
    return door.getValue(HINGE) == farHinge || allowFlip && !door.getValue(OPEN)
        && world.getBlockState(pos.up()).getBlock() == this;
  }

  @Override
  public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state,
      EntityLivingBase placer, ItemStack stack) {
    Hinge hinge = PENDING_HINGE.get();
    Boolean reversed = PENDING_REVERSED.get();
    BlockPos flip = PENDING_FLIP.get();
    PENDING_HINGE.remove();
    PENDING_REVERSED.remove();
    PENDING_FLIP.remove();
    worldIn.setBlockState(pos.up(), getDefaultState().withProperty(HALF, Half.UPPER)
        .withProperty(HINGE, hinge == null ? Hinge.LEFT : hinge)
        .withProperty(REVERSED, reversed != null && reversed), 2);
    if (flip != null) {
      IBlockState upper = worldIn.getBlockState(flip.up());
      if (upper.getBlock() == this && upper.getValue(HALF) == Half.UPPER) {
        // Flag 3: a client redraws round the change, so the lower half's model follows.
        worldIn.setBlockState(flip.up(), upper.withProperty(HINGE,
            upper.getValue(HINGE) == Hinge.LEFT ? Hinge.RIGHT : Hinge.LEFT), 3);
      }
    }
  }

  /**
   * Either half going takes the other with it; the item drops once, from the lower half.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (state.getValue(HALF) == Half.UPPER) {
      IBlockState down = worldIn.getBlockState(pos.down());
      if (down.getBlock() != this) {
        worldIn.setBlockToAir(pos);
      } else if (blockIn != this) {
        neighborChanged(down, worldIn, pos.down(), blockIn, fromPos);
      }
      return;
    }
    IBlockState up = worldIn.getBlockState(pos.up());
    if (up.getBlock() != this) {
      worldIn.setBlockToAir(pos);
      if (!worldIn.isRemote) {
        dropBlockAsItem(worldIn, pos, state, 0);
      }
      return;
    }
    if (worldIn.isRemote || blockIn == this) {
      return;
    }
    boolean powered = worldIn.isBlockPowered(pos) || worldIn.isBlockPowered(pos.up());
    Set<BlockPos> seen = POWERED.computeIfAbsent(worldIn, w -> new HashSet<>());
    if (powered != seen.contains(pos)) {
      if (powered) {
        seen.add(pos.toImmutable());
      } else {
        seen.remove(pos);
      }
      onPower(worldIn, pos, powered);
    }
  }

  /**
   * The redstone signal at a door changed: it opens while powered and shuts when the signal goes.
   *
   * @param world    the world
   * @param lowerPos the door's lower half
   * @param powered  whether it is powered now
   *
   * @since 1.0
   */
  protected void onPower(World world, BlockPos lowerPos, boolean powered) {
    setOpen(world, lowerPos, powered, false);
  }

  /** Breaking the upper half in creative takes the lower away first, so nothing drops. */
  @Override
  public void onBlockHarvested(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer player) {
    if (state.getValue(HALF) == Half.UPPER && player.capabilities.isCreativeMode
        && worldIn.getBlockState(pos.down()).getBlock() == this) {
      worldIn.setBlockToAir(pos.down());
    }
  }

  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    if (state.getValue(HALF) == Half.LOWER && !worldIn.isRemote) {
      DoorLocks.get(worldIn).unlock(pos);
    }
    if (state.getValue(HALF) == Half.UPPER && state.getValue(CLOSER) && !worldIn.isRemote) {
      spawnAsEntity(worldIn, pos, new ItemStack(ItemDoorCloser.instance()));
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  @Nonnull
  public Item getItemDropped(IBlockState state, Random rand, int fortune) {
    return state.getValue(HALF) == Half.UPPER ? Items.AIR : super.getItemDropped(state, rand,
        fortune);
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public EnumPushReaction getPushReaction(IBlockState state) {
    return EnumPushReaction.DESTROY;
  }

  // --- opening and closing --------------------------------------------------------------------------

  protected SoundEvent sound(boolean open) {
    if (metal(getBlockRegistryName())) {
      return open ? SoundEvents.BLOCK_IRON_DOOR_OPEN : SoundEvents.BLOCK_IRON_DOOR_CLOSE;
    }
    return open ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE;
  }

  /** The other door of a pair: beside this one on its latch side, hinged the other way. */
  @Nullable
  protected BlockPos partner(World world, BlockPos lowerPos, IBlockState door) {
    EnumFacing f = door.getValue(FACING);
    EnumFacing latch = door.getValue(HINGE) == Hinge.LEFT ? f.rotateY() : f.rotateYCCW();
    BlockPos p = lowerPos.offset(latch);
    IBlockState other = world.getBlockState(p);
    if (other.getBlock() != this || other.getValue(HALF) != Half.LOWER) {
      return null;
    }
    IBlockState whole = whole(world, p);
    return whole.getValue(FACING) == f && whole.getValue(HINGE) != door.getValue(HINGE) ? p
        : null;
  }

  /**
   * Opens or shuts the door at {@code lowerPos}, and its pair.
   *
   * @param world    the world
   * @param lowerPos its lower half
   * @param open     whether to open it
   * @param byHand   whether a player or a keypad did it, which starts a fitted closer
   *
   * @since 1.0
   */
  void setOpen(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    swing(world, lowerPos, open, byHand);
    BlockPos p = partner(world, lowerPos, whole(world, lowerPos));
    if (p != null) {
      swing(world, p, open, byHand);
    }
  }

  protected void swing(World world, BlockPos lowerPos, boolean open, boolean byHand) {
    IBlockState lower = world.getBlockState(lowerPos);
    IBlockState upper = world.getBlockState(lowerPos.up());
    if (lower.getBlock() != this || upper.getBlock() != this) {
      return;
    }
    if (lower.getValue(OPEN) != open) {
      world.setBlockState(lowerPos, lower.withProperty(OPEN, open), 10);
      // Every client near enough to see it makes the swing's tile entity itself (eventReceived);
      // the server keeps only a pending tick on the upper half, so it knows the door is moving.
      world.addBlockEvent(lowerPos.up(), this, EVENT_SWING, open ? 1 : 0);
      world.scheduleUpdate(lowerPos.up(), this, SWING_TICKS);
      world.playSound(null, lowerPos, sound(open), SoundCategory.BLOCKS, 1.0F,
          world.rand.nextFloat() * 0.1F + 0.9F);
    }
    if (open && byHand && upper.getValue(CLOSER)) {
      world.scheduleUpdate(lowerPos, this, CLOSER_TICKS);
    }
  }

  /**
   * A door has started to swing. On a client, the upper half gets a {@link TileEntityDoorSwing}
   * for the length of the swing, which draws it and then takes itself away; the server makes none.
   *
   * @since 1.1
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean eventReceived(IBlockState state, World worldIn, BlockPos pos, int id,
      int param) {
    if (id != EVENT_SWING) {
      return false;
    }
    if (worldIn.isRemote && state.getBlock() == this && state.getValue(HALF) == Half.UPPER) {
      TileEntityDoorSwing te = new TileEntityDoorSwing();
      te.begin(param == 1, worldIn.getTotalWorldTime());
      worldIn.setTileEntity(pos, te);
      TileEntityDoorSwing.redraw(worldIn, pos);
    }
    return true;
  }

  /**
   * Whether the door at {@code lowerPos} is still swinging, as the server sees it: its upper half's
   * end-of-swing tick has not come yet.
   */
  protected boolean swinging(World world, BlockPos lowerPos) {
    return world.isUpdateScheduled(lowerPos.up(), this);
  }

  /**
   * The upper half: the swing is over. The lower half: a fitted closer shuts the door, unless
   * something is holding it open with redstone.
   *
   * @since 1.0
   */
  @Override
  public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
    if (state.getValue(HALF) == Half.UPPER) {
      // Nothing to do: the pending tick was only there to say the door is moving. A world saved
      // mid-swing by a version where this tick ended the swing brings the tick back with it.
      TileEntityDoorSwing.putRightLegacySwing(worldIn, pos);
      return;
    }
    IBlockState door = whole(worldIn, pos);
    if (door.getValue(OPEN) && door.getValue(CLOSER) && !worldIn.isBlockPowered(pos)
        && !worldIn.isBlockPowered(pos.up())) {
      setOpen(worldIn, pos, false, false);
    }
  }

  /**
   * Whether a player is on the inside -- the side the door faces, where an inswing door swings to
   * and an outswing door swings from.
   */
  protected static boolean inside(EntityPlayer player, BlockPos pos, EnumFacing f) {
    return (player.posX - (pos.getX() + 0.5)) * f.getXOffset()
        + (player.posZ - (pos.getZ() + 0.5)) * f.getZOffset() > 0;
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (hand != EnumHand.MAIN_HAND) {
      return false;
    }
    BlockPos lowerPos = lower(state, pos);
    IBlockState door = whole(worldIn, lowerPos);
    if (playerIn.isSneaking()) {
      // Linking a keypad or control to this door, or else taking off its closer.
      if (GarageDoorLinks.finish(playerIn, worldIn, lowerPos)) {
        return true;
      }
      if (door.getValue(CLOSER)) {
        if (!worldIn.isRemote) {
          IBlockState upper = worldIn.getBlockState(lowerPos.up());
          worldIn.setBlockState(lowerPos.up(), upper.withProperty(CLOSER, false), 3);
          if (!playerIn.capabilities.isCreativeMode) {
            ItemStack item = new ItemStack(ItemDoorCloser.instance());
            if (!playerIn.inventory.addItemStackToInventory(item)) {
              spawnAsEntity(worldIn, pos, item);
            }
          }
        }
        return true;
      }
      return false;
    }
    if (!worldIn.isRemote) {
      if (!door.getValue(OPEN) && DoorLocks.get(worldIn).isLocked(lowerPos)
          && !inside(playerIn, lowerPos, door.getValue(FACING))) {
        playerIn.sendStatusMessage(new TextComponentTranslation("gui.csm.door.locked"), true);
        worldIn.playSound(null, lowerPos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE,
            SoundCategory.BLOCKS, 0.4F, 1.6F);
        return true;
      }
      setOpen(worldIn, lowerPos, !door.getValue(OPEN), true);
    }
    return true;
  }

  /**
   * Fits a door closer.
   *
   * @return whether one was fitted (there was none)
   *
   * @since 1.0
   */
  boolean fitCloser(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    BlockPos upperPos = lower(state, pos).up();
    IBlockState upper = world.getBlockState(upperPos);
    if (upper.getBlock() != this || upper.getValue(CLOSER)) {
      return false;
    }
    world.setBlockState(upperPos, upper.withProperty(CLOSER, true), 3);
    return true;
  }

  /** The outcome of {@link #flipSwing}. */
  enum FlipResult {
    /** Flipped; the door (and its pair) now swings out. */
    OUT,
    /** Flipped; the door (and its pair) now swings in. */
    IN,
    /** Not flipped: the door or its pair is open or still swinging. */
    BUSY,
    /** Not flipped: this door's movement is not a swing that can be reversed. */
    FIXED
  }

  /**
   * Makes a door swing the other way, and its pair with it, so a pair always swings the same way.
   * Only a shut door that is not moving: flipping one mid-swing or open would jump its leaf across
   * the cell.
   *
   * @param world the world
   * @param pos   either half of the door
   *
   * @return what happened
   *
   * @since 1.1
   */
  FlipResult flipSwing(World world, BlockPos pos) {
    if (!reversible()) {
      return FlipResult.FIXED;
    }
    BlockPos lowerPos = lower(world.getBlockState(pos), pos);
    IBlockState door = whole(world, lowerPos);
    BlockPos p = partner(world, lowerPos, door);
    if (door.getValue(OPEN) || swinging(world, lowerPos)
        || p != null && (whole(world, p).getValue(OPEN) || swinging(world, p))) {
      return FlipResult.BUSY;
    }
    boolean reversed = !door.getValue(REVERSED);
    setReversed(world, lowerPos, reversed);
    if (p != null) {
      setReversed(world, p, reversed);
    }
    return DoorSwingDirection.outward(outswing(), reversed) ? FlipResult.OUT : FlipResult.IN;
  }

  private void setReversed(World world, BlockPos lowerPos, boolean reversed) {
    IBlockState upper = world.getBlockState(lowerPos.up());
    if (upper.getBlock() == this && upper.getValue(HALF) == Half.UPPER) {
      // A client redraws the blocks round a change, so the lower half's models follow.
      world.setBlockState(lowerPos.up(), upper.withProperty(REVERSED, reversed), 3);
    }
  }

  /**
   * A command from a linked control: a keypad's right code opens the door (lock or no lock), a
   * button toggles it, a station opens or closes it.
   *
   * @since 1.0
   */
  void command(World world, BlockPos pos, BlockGarageDoor.Command command) {
    BlockPos lowerPos = lower(world.getBlockState(pos), pos);
    boolean open = whole(world, lowerPos).getValue(OPEN);
    switch (command) {
      case OPEN:
        setOpen(world, lowerPos, true, true);
        break;
      case CLOSE:
        setOpen(world, lowerPos, false, false);
        break;
      case TOGGLE:
        setOpen(world, lowerPos, !open, true);
        break;
      default:
        break;
    }
  }

  // --- the swing's tile entity ----------------------------------------------------------------------

  /**
   * The upper half may hold a {@link TileEntityDoorSwing}, and must say so for every state: a chunk
   * refuses a tile entity whose block says it has none, and nothing in the stored state says the
   * door is swinging. But it never makes one itself ({@link #createNewTileEntity} makes none), so
   * a door at rest has none: only {@link #eventReceived} puts one there, on a client, for a swing.
   *
   * @since 1.0
   */
  @Override
  public boolean hasTileEntity(IBlockState state) {
    return state.getValue(HALF) == Half.UPPER;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return null;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityDoorSwing.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitydoorswing";
  }

  // --- shape ------------------------------------------------------------------------------------

  /** The leaf's box for a door that swings in (a custom door always does). */
  static AxisAlignedBB leafBox(IBlockState door) {
    return leafBox(door, false);
  }

  /**
   * The leaf's box: shut, along the outside face of the cell, or the inside face for a door that
   * swings out; open, along the hinge jamb either way.
   */
  static AxisAlignedBB leafBox(IBlockState door, boolean outswing) {
    AxisAlignedBB north = !door.getValue(OPEN) ? (outswing ? CLOSED_OUT_NORTH : CLOSED_NORTH)
        : door.getValue(HINGE) == Hinge.LEFT ? OPEN_LEFT_NORTH : OPEN_RIGHT_NORTH;
    return BlockGarageDoor.turn(north, door.getValue(FACING));
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState door = getActualState(state, source, pos);
    return leafBox(door, outward(door));
  }

  @Override
  @Nonnull
  public BlockFaceShape getBlockFaceShape(IBlockAccess worldIn, IBlockState state, BlockPos pos,
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
    return glazed() ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}
