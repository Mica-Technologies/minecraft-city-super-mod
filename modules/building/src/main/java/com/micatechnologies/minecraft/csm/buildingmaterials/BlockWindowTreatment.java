package com.micatechnologies.minecraft.csm.buildingmaterials;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A window blind, shade or curtain: venetian blinds, roller shades, vertical blinds and curtains,
 * one class constructed by registry name.
 *
 * <p>It is its own thin block, hung in the cell on the room side of a window -- our glazing,
 * vanilla glass, or any opening -- against the window ({@link #FACING}, the way to the window, is
 * taken from the face it is placed against). Blinds of the same kind hung the same way join into
 * one: a headrail, rod or cassette only along the top course, a bottom rail only along the bottom,
 * and a curtain bunches at the two ends of the whole run.</p>
 *
 * <p>Right-click cycles the whole joined blind through its states (see {@link Kind}); a redstone
 * signal closes it and taking the signal away opens it, as a motorised shade does. A closed blind
 * or curtain takes a little of the light through the window; a blackout shade takes all of it.</p>
 *
 * <p>Stored: the facing and the state (four bits). Which neighbours are the same blind is actual
 * state, in the blind's own frame: {@link #LEFT} and {@link #RIGHT} as seen from inside, looking
 * at the window. The models come from {@code dev-env-utils/scripts/gen_window_treatments.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockWindowTreatment extends AbstractBlock {

  public static final PropertyDirection FACING = BlockHorizontal.FACING;
  public static final PropertyInteger STATE = PropertyInteger.create("state", 0, 3);
  public static final PropertyBool LEFT = PropertyBool.create("left");
  public static final PropertyBool RIGHT = PropertyBool.create("right");
  public static final PropertyBool UP = PropertyBool.create("up");
  public static final PropertyBool DOWN = PropertyBool.create("down");

  /** The most blocks one click or signal moves together. */
  private static final int MAX_GROUP = 512;

  /** Which blind blocks were last seen powered, per world, to act only when the signal changes. */
  private static final Map<World, Set<BlockPos>> POWERED =
      Collections.synchronizedMap(new WeakHashMap<>());

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  /**
   * The kinds, by registry-name prefix, and what their states are.
   *
   * @since 1.0
   */
  public enum Kind {
    /** 0 open, 1 tilted, 2 closed, 3 raised. */
    VENETIAN("blind_venetian", 4, 2, 0, new int[]{0, 1, 3, 0}),
    /** 0 down, 1 half, 2 up. */
    ROLLER("shade_roller", 3, 0, 2, new int[]{3, 1, 0, 0}),
    /** 0 open, 1 closed, 2 drawn aside. */
    VERTICAL("blind_vertical", 3, 1, 0, new int[]{0, 3, 0, 0}),
    /** 0 closed, 1 open. */
    CURTAIN("curtain", 2, 0, 1, new int[]{3, 0, 0, 0});

    private final String prefix;
    private final int states;
    private final int closed;
    private final int open;
    private final int[] opacity;

    Kind(String prefix, int states, int closed, int open, int[] opacity) {
      this.prefix = prefix;
      this.states = states;
      this.closed = closed;
      this.open = open;
      this.opacity = opacity;
    }

    static Kind of(String registryName) {
      for (Kind k : values()) {
        if (registryName.startsWith(k.prefix)) {
          return k;
        }
      }
      throw new IllegalArgumentException("No window treatment kind for " + registryName);
    }
  }

  private final String registryName;

  /**
   * Constructs a {@link BlockWindowTreatment}.
   *
   * @param registryName which blind: {@code blind_venetian_*}, {@code shade_roller_*} (a
   *                     {@code blackout} one blocks all light when down), {@code blind_vertical_*}
   *                     or {@code curtain_*} (a {@code sheer} one is translucent)
   *
   * @since 1.0
   */
  public BlockWindowTreatment(String registryName) {
    super(pendingMaterial(registryName), SoundType.CLOTH, "axe", 0, 0.3F, 1F, 0F, 0);
    this.registryName = registryName;
    setDefaultState(blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH)
        .withProperty(STATE, 0));
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName) {
    PENDING_REGISTRY_NAME.set(registryName);
    return Material.CLOTH;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  private Kind kind() {
    return Kind.of(getBlockRegistryName());
  }

  private boolean isBlackout() {
    return getBlockRegistryName().contains("blackout");
  }

  private boolean isSheer() {
    return getBlockRegistryName().contains("sheer");
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, STATE, LEFT, RIGHT, UP, DOWN);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3))
        .withProperty(STATE, (meta >> 2) & 3);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(FACING).getHorizontalIndex() | (state.getValue(STATE) << 2);
  }

  /**
   * Hangs against the block it was placed on; placed on a floor or ceiling, against whatever is
   * ahead of the player. Starts closed, the state a new blind comes out of its box in.
   *
   * @since 1.0
   */
  @Override
  @Nonnull
  public IBlockState getStateForPlacement(World worldIn, BlockPos pos, EnumFacing facing,
      float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
    EnumFacing window = facing.getAxis().isHorizontal() ? facing.getOpposite()
        : placer.getHorizontalFacing();
    return getDefaultState().withProperty(FACING, window).withProperty(STATE, kind().closed);
  }

  private boolean same(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    IBlockState other = world.getBlockState(pos);
    return other.getBlock() == this && other.getValue(FACING) == facing;
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess worldIn,
      @Nonnull BlockPos pos) {
    EnumFacing f = state.getValue(FACING);
    return state.withProperty(LEFT, same(worldIn, pos.offset(f.rotateYCCW()), f))
        .withProperty(RIGHT, same(worldIn, pos.offset(f.rotateY()), f))
        .withProperty(UP, same(worldIn, pos.up(), f))
        .withProperty(DOWN, same(worldIn, pos.down(), f));
  }

  /** Every block of the joined blind {@code pos} belongs to. */
  private Set<BlockPos> group(World world, BlockPos pos, EnumFacing f) {
    Set<BlockPos> seen = new HashSet<>();
    Deque<BlockPos> todo = new ArrayDeque<>();
    todo.add(pos);
    seen.add(pos);
    EnumFacing[] ways = {f.rotateY(), f.rotateYCCW(), EnumFacing.UP, EnumFacing.DOWN};
    while (!todo.isEmpty() && seen.size() < MAX_GROUP) {
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

  private void setGroup(World world, BlockPos pos, IBlockState state, int value) {
    for (BlockPos p : group(world, pos, state.getValue(FACING))) {
      IBlockState s = world.getBlockState(p);
      if (s.getValue(STATE) != value) {
        world.setBlockState(p, s.withProperty(STATE, value), 3);
      }
    }
  }

  /**
   * Cycles the whole joined blind to its next state.
   *
   * @since 1.0
   */
  @Override
  public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state,
      EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY,
      float hitZ) {
    if (playerIn.isSneaking()) {
      return false;
    }
    if (!worldIn.isRemote) {
      int next = (state.getValue(STATE) + 1) % kind().states;
      setGroup(worldIn, pos, state, next);
      worldIn.playSound(null, pos, SoundEvents.BLOCK_CLOTH_STEP, SoundCategory.BLOCKS, 0.8F, 1.2F);
    }
    return true;
  }

  /**
   * Closes the joined blind when a signal arrives and opens it when the signal goes, acting only
   * on a change, so a blind closed by hand is not opened by a neighbour being placed. What was
   * powered is remembered only while the world is loaded; after a restart a blind left closed by
   * a signal that has since gone stays closed until clicked.
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
      setGroup(worldIn, pos, state, kind().closed);
    } else if (!powered && was) {
      seen.remove(pos);
      setGroup(worldIn, pos, state, kind().open);
    }
  }

  /**
   * How much light a blind in this state takes: a little when closed, none when open, all of it
   * for a closed blackout shade.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public int getLightOpacity(IBlockState state) {
    int value = state.getValue(STATE);
    int o = kind().opacity[value];
    if (isBlackout() && o > 0) {
      return value == kind().closed ? 15 : 7;
    }
    return isSheer() ? Math.min(o, 1) : o;
  }

  /**
   * Lit by its neighbours, since a closed blackout shade has no light of its own.
   *
   * @since 1.0
   */
  @Override
  @SuppressWarnings("deprecation")
  public boolean getUseNeighborBrightness(IBlockState state) {
    return true;
  }

  @Override
  @Nonnull
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    double d = 2.5 / 16.0;
    switch (state.getValue(FACING)) {
      case SOUTH:
        return new AxisAlignedBB(0, 0, 1 - d, 1, 1, 1);
      case EAST:
        return new AxisAlignedBB(1 - d, 0, 0, 1, 1, 1);
      case WEST:
        return new AxisAlignedBB(0, 0, 0, d, 1, 1);
      default:
        return new AxisAlignedBB(0, 0, 0, 1, 1, d);
    }
  }

  @Override
  @SuppressWarnings("deprecation")
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(IBlockState blockState,
      @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
    return NULL_AABB;
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
    return isSheer() ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
  }
}
