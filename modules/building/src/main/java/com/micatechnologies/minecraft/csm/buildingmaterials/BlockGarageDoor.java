package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTileEntityProvider;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A garage door built to the size of its opening: a sectional overhead door, a coiling roll-up
 * door or a security grille, one class constructed by registry name.
 *
 * <p>Fill an opening with door blocks of one kind, hung the same way, and they are one door: side
 * tracks or guides along its two sides, the roll-up's hood or the sectional door's overhead track
 * along the top, each drawn only on the block at that edge of the whole door. Right-click any block
 * of it, or give it a redstone signal, and the whole door opens or closes.</p>
 *
 * <h3>Why it costs nothing at rest</h3>
 *
 * <p>An open or closed door is nothing but baked block models -- no tile entity at all -- so a
 * street of garage doors draws like a street of walls. Only while a door is moving does one block
 * of it, the {@link Motion#ANCHOR anchor}, carry a {@link TileEntityGarageDoor}, whose renderer
 * draws the travelling curtain or panels for the few seconds the move takes; every other block of
 * the door is {@link Motion#MOVING}, which draws only what does not move (the tracks and the
 * hood). When the move is over the anchor sets the whole door to its new state, and its tile entity
 * goes with the state that asked for it ({@link #hasTileEntity(IBlockState)}).</p>
 *
 * <p>Stored: facing (the way to the inside, where the tracks and hood are) and {@link #MOTION}:
 * the four metadata bits. Which neighbours are the same door is actual state, in the model's frame
 * ({@link #CCW} and {@link #CW} are the sides a quarter turn anticlockwise and clockwise from the
 * facing). A sectional door also has {@link #DEPTH}, actual state on its top course: the door's
 * height, which is how far back along the ceiling its track runs and an open door's panels lie. The
 * models come from {@code dev-env-utils/scripts/gen_garage_doors.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockGarageDoor extends AbstractBlock implements ICsmTileEntityProvider {

  /**
   * Where a door is: at rest closed or open, or moving -- the anchor being the one block of a
   * moving door that animates it.
   *
   * @since 1.0
   */
  public enum Motion implements IStringSerializable {
    CLOSED, OPEN, MOVING, ANCHOR;

    @Override
    @Nonnull
    public String getName() {
      return name().toLowerCase(Locale.ROOT);
    }

    boolean moving() {
      return this == MOVING || this == ANCHOR;
    }
  }

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyEnum<Motion> MOTION = PropertyEnum.create("motion", Motion.class);
  public static final PropertyBool CCW = PropertyBool.create("ccw");
  public static final PropertyBool CW = PropertyBool.create("cw");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The tallest sectional door whose track and open panels reach its full height back. */
  public static final int MAX_DEPTH = 8;
  public static final PropertyInteger DEPTH = PropertyInteger.create("depth", 1, MAX_DEPTH);

  /** The largest door one click moves: sixteen by sixteen blocks. */
  static final int MAX_BLOCKS = 256;

  /** Steel that comes down by hand, as the scaffold's does. */
  private static final Material DOOR_STEEL = new Material(MapColor.IRON);

  /** The door's plane, with the inside to the north: just behind the opening's inside face. */
  private static final AxisAlignedBB PLANE_NORTH =
      new AxisAlignedBB(0, 0, 1 / 16.0, 1, 1, 3 / 16.0);
  /** What can be clicked on an open door: a strip along the top of its top course. */
  private static final AxisAlignedBB HEAD_NORTH =
      new AxisAlignedBB(0, 13 / 16.0, 0, 1, 1, 3 / 16.0);

  /** Which door blocks were last seen powered, per world, to act only on a rising edge. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The kinds of door, by registry-name prefix.
   *
   * @since 1.0
   */
  public enum Kind {
    SECTIONAL("garage_door_sectional"),
    ROLLUP("garage_door_rollup"),
    GRILLE("garage_door_grille");

    private final String prefix;

    Kind(String prefix) {
      this.prefix = prefix;
    }

    static Kind of(String registryName) {
      for (Kind k : values()) {
        if (registryName.startsWith(k.prefix)) {
          return k;
        }
      }
      throw new IllegalArgumentException("No garage door kind for " + registryName);
    }
  }

  private final String registryName;
  private final Kind kind;

  /**
   * Constructs a {@link BlockGarageDoor}.
   *
   * @param registryName {@code garage_door_sectional_*}, {@code garage_door_rollup_*} or
   *                     {@code garage_door_grille}
   *
   * @since 1.0
   */
  public BlockGarageDoor(String registryName) {
    super(pendingMaterial(registryName), SoundType.METAL, "pickaxe", 0, 2F, 10F, 0F, 0);
    this.registryName = registryName;
    this.kind = Kind.of(registryName);
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(MOTION, Motion.CLOSED));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return DOOR_STEEL;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  /**
   * The kind of door.
   *
   * @return the kind
   *
   * @since 1.0
   */
  public Kind kind() {
    return kind != null ? kind : Kind.of(getBlockRegistryName());
  }

  /**
   * Only a sectional door has a depth: a coiling door's curtain goes up into its hood.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    // Runs inside the super constructor, before the fields are set: kind() reads the pending name.
    if (kind() == Kind.SECTIONAL) {
      return new BlockStateContainer(this, FACING, MOTION, CCW, CW, UP, DOWN, DEPTH);
    }
    return new BlockStateContainer(this, FACING, MOTION, CCW, CW, UP, DOWN);
  }

  private boolean hasDepth() {
    return kind() == Kind.SECTIONAL;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(MOTION, Motion.values()[(meta >> 2) & 3]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(MOTION).ordinal() << 2);
  }

  /**
   * Faces the way the player looks: a door is placed from outside, looking at the building, and
   * its tracks and hood go on the inside, away from the player.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    // A block added to a door takes the door's state, so an open door stays one door.
    for (EnumFacing side : EnumFacing.values()) {
      IBlockState other = worldIn.getBlockState(pos.offset(side));
      if (other.getBlock() == this && other.getValue(FACING) == placer.getHorizontalFacing()
          && !other.getValue(MOTION).moving()) {
        return getDefaultState().withProperty(FACING, other.getValue(FACING))
            .withProperty(MOTION, other.getValue(MOTION));
      }
    }
    return getDefaultState().withProperty(FACING, placer.getHorizontalFacing());
  }

  /** Whether {@code pos} holds a block of this same door hung the same way. */
  boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    boolean up = same(worldIn, pos.up(), f);
    state = state.withProperty(CCW, same(worldIn, pos.offset(f.rotateYCCW()), f))
        .withProperty(CW, same(worldIn, pos.offset(f.rotateY()), f))
        .withProperty(UP, up)
        .withProperty(DOWN, same(worldIn, pos.down(), f));
    if (hasDepth()) {
      int depth = 1;
      if (!up) {
        while (depth < MAX_DEPTH && same(worldIn, pos.down(depth), f)) {
          depth++;
        }
      }
      state = state.withProperty(DEPTH, depth);
    }
    return state;
  }

  /**
   * Every block of the door {@code pos} belongs to, capped at {@link #MAX_BLOCKS}.
   *
   * @param world the world
   * @param pos   a block of the door
   * @param f     its facing
   *
   * @return the door's blocks
   *
   * @since 1.0
   */
  Set<BlockPos> door(World world, BlockPos pos, EnumFacing f) {
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    todo.add(pos.toImmutable());
    seen.add(pos.toImmutable());
    EnumFacing[] ways = {f.rotateY(), f.rotateYCCW(), EnumFacing.UP, EnumFacing.DOWN};
    while (!todo.isEmpty() && seen.size() < MAX_BLOCKS) {
      BlockPos p = todo.poll();
      for (EnumFacing way : ways) {
        BlockPos q = p.offset(way);
        if (!seen.contains(q) && same(world, q, f)) {
          seen.add(q);
          todo.add(q);
        }
      }
    }
    return seen;
  }

  /**
   * Starts the whole door moving to the other state: every block of it goes {@link Motion#MOVING}
   * and the anchor -- its lowest block furthest anticlockwise -- {@link Motion#ANCHOR}, which
   * gives it the tile entity that animates the door and finishes the move. Nothing happens while it
   * is already moving.
   *
   * @param world the world
   * @param pos   a block of the door
   *
   * @since 1.0
   */
  void toggle(World world, BlockPos pos) {
    IBlockState state = world.getBlockState(pos);
    if (state.getValue(MOTION).moving()) {
      return;
    }
    EnumFacing f = state.getValue(FACING);
    Set<BlockPos> blocks = door(world, pos, f);
    boolean opening = state.getValue(MOTION) == Motion.CLOSED;
    // The anchor: lowest, then furthest anticlockwise. The extent is measured from it.
    EnumFacing ccw = f.rotateYCCW();
    BlockPos anchor = null;
    int anchorSide = 0;
    int minY = Integer.MAX_VALUE;
    int maxY = Integer.MIN_VALUE;
    int minSide = Integer.MAX_VALUE;
    int maxSide = Integer.MIN_VALUE;
    for (BlockPos p : blocks) {
      int side = p.getX() * ccw.getXOffset() + p.getZ() * ccw.getZOffset();
      minY = Math.min(minY, p.getY());
      maxY = Math.max(maxY, p.getY());
      minSide = Math.min(minSide, side);
      maxSide = Math.max(maxSide, side);
      if (anchor == null || p.getY() < anchor.getY()
          || (p.getY() == anchor.getY() && side > anchorSide)) {
        anchor = p;
        anchorSide = side;
      }
    }
    int width = maxSide - minSide + 1;
    int height = maxY - minY + 1;
    for (BlockPos p : blocks) {
      if (!p.equals(anchor)) {
        world.setBlockState(p, world.getBlockState(p).withProperty(MOTION, Motion.MOVING), 2);
      }
    }
    world.setBlockState(anchor, world.getBlockState(anchor).withProperty(MOTION, Motion.ANCHOR),
        3);
    TileEntity te = world.getTileEntity(anchor);
    if (te instanceof TileEntityGarageDoor) {
      ((TileEntityGarageDoor) te).start(width, height, opening, world.getTotalWorldTime());
    }
    world.playSound(null, anchor, kind() == Kind.SECTIONAL ? SoundEvents.BLOCK_PISTON_EXTEND
        : SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.6F, 0.5F);
  }

  /**
   * Ends a move: every block of the door, as measured when it started, set to its new state and
   * no longer moving. Called by the anchor's tile entity when the time is up, and when the anchor
   * is broken mid-move.
   *
   * @param world   the world
   * @param anchor  the anchor
   * @param f       the door's facing
   * @param width   the door's width in blocks
   * @param height  its height
   * @param opening whether it was opening
   *
   * @since 1.0
   */
  void finish(World world, BlockPos anchor, EnumFacing f, int width, int height,
      boolean opening) {
    EnumFacing cw = f.rotateY();
    Motion to = opening ? Motion.OPEN : Motion.CLOSED;
    for (int i = 0; i < width; i++) {
      for (int j = 0; j < height; j++) {
        BlockPos p = anchor.offset(cw, i).up(j);
        IBlockState s = world.getBlockState(p);
        if (s.getBlock() == this && s.getValue(FACING) == f && s.getValue(MOTION).moving()) {
          world.setBlockState(p, s.withProperty(MOTION, to), 3);
        }
      }
    }
    world.playSound(null, anchor, SoundEvents.BLOCK_IRON_DOOR_CLOSE, SoundCategory.BLOCKS, 0.5F,
        0.5F);
  }

  /**
   * Breaking the anchor mid-move ends the move for the rest of the door at once, so nothing is
   * left stuck moving with no one to finish it.
   *
   * @since 1.0
   */
  @Override
  public void breakBlock(World worldIn, BlockPos pos, IBlockState state) {
    if (state.getValue(MOTION) == Motion.ANCHOR) {
      TileEntity te = worldIn.getTileEntity(pos);
      if (te instanceof TileEntityGarageDoor) {
        TileEntityGarageDoor door = (TileEntityGarageDoor) te;
        super.breakBlock(worldIn, pos, state);
        finish(worldIn, pos, state.getValue(FACING), door.getWidth(), door.getHeight(),
            door.isOpening());
        return;
      }
    }
    super.breakBlock(worldIn, pos, state);
  }

  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (playerIn.isSneaking()) {
      return false;
    }
    if (!worldIn.isRemote) {
      toggle(worldIn, pos);
    }
    return true;
  }

  /**
   * A redstone signal arriving -- the rising edge only -- moves the door, so a button's pulse
   * opens it rather than opening and at once closing it, and a lever moves it on every flip.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void neighborChanged(IBlockState state, World worldIn, BlockPos pos, Block blockIn,
      BlockPos fromPos) {
    if (worldIn.isRemote) {
      return;
    }
    boolean powered = worldIn.isBlockPowered(pos);
    Set<BlockPos> seen = POWERED.computeIfAbsent(worldIn, w -> new HashSet<>());
    boolean was = seen.contains(pos);
    if (powered && !was) {
      seen.add(pos.toImmutable());
      toggle(worldIn, pos);
    } else if (!powered && was) {
      seen.remove(pos);
    }
  }

  // --- tile entity: only the anchor, only while moving ------------------------------------------

  @Override
  public boolean hasTileEntity(IBlockState state) {
    return state.getValue(MOTION) == Motion.ANCHOR;
  }

  @Nullable
  @Override
  public TileEntity createNewTileEntity(@Nonnull World worldIn, int meta) {
    return ((meta >> 2) & 3) == Motion.ANCHOR.ordinal() ? new TileEntityGarageDoor() : null;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntityGarageDoor.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentitygaragedoor";
  }

  // --- light --------------------------------------------------------------------------------------

  /**
   * A closed or moving door keeps the light out, as a wall would; a grille, and an open door, let
   * it through.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public int getLightOpacity(IBlockState state) {
    return kind() != Kind.GRILLE && state.getValue(MOTION) != Motion.OPEN ? 15 : 0;
  }

  /**
   * Lit by its neighbours, or a door that keeps the light out would draw itself black.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean getUseNeighborBrightness(IBlockState state) {
    return true;
  }

  // --- shape ------------------------------------------------------------------------------------

  /** A box drawn with the inside to the north, turned to the facing. */
  static AxisAlignedBB turn(AxisAlignedBB box, EnumFacing facing) {
    switch (facing) {
      case SOUTH:
        return new AxisAlignedBB(1 - box.maxX, box.minY, 1 - box.maxZ, 1 - box.minX, box.maxY,
            1 - box.minZ);
      case EAST:
        return new AxisAlignedBB(1 - box.maxZ, box.minY, box.minX, 1 - box.minZ, box.maxY,
            box.maxX);
      case WEST:
        return new AxisAlignedBB(box.minZ, box.minY, 1 - box.maxX, box.maxZ, box.maxY,
            1 - box.minX);
      default:
        return box;
    }
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return turn(state.getValue(MOTION) == Motion.OPEN ? HEAD_NORTH : PLANE_NORTH, f);
  }

  /**
   * An open door can be clicked only along the top of its top course: the rest of the opening is
   * clear, and a player must be able to reach through it.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public RayTraceResult collisionRayTrace(IBlockState blockState, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull Vec3d start, @Nonnull Vec3d end) {
    if (blockState.getValue(MOTION) == Motion.OPEN
        && same(worldIn, pos.up(), blockState.getValue(FACING))) {
      return null;
    }
    return super.collisionRayTrace(blockState, worldIn, pos, start, end);
  }

  /**
   * Solid unless it is open; a moving door is solid, and no one walks into a closing door.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, @Nonnull World worldIn,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entityIn,
      boolean isActualState) {
    if (state.getValue(MOTION) != Motion.OPEN) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes,
          turn(PLANE_NORTH, state.getValue(FACING)));
    }
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
    return BlockRenderLayer.CUTOUT;
  }

}
