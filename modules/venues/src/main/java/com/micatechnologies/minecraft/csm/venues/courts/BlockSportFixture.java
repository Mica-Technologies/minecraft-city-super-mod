package com.micatechnologies.minecraft.csm.venues.courts;

import com.micatechnologies.minecraft.csm.codeutils.CsmBlockStateContainer;
import com.micatechnologies.minecraft.csm.venues.BlockVenue;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A one-block fixture: the tennis ball machine, a hurdle (its height a variant), a starting
 * block, the shot put toe board; a pool's wall target and touch pad tiles, lane rope and ladder;
 * golf's flagstick, tee markers, yardage marker, ball washer, the driving range's mat, divider,
 * ball bucket and distance flag, mini-golf's hump and scorecard stand. Decor: baked models,
 * nothing ticks. A kind whose variants have names steps them on a sneak-click with an empty
 * hand.
 *
 * <p><b>Metadata</b>: bits 0-1 the facing (S 0, W 1, N 2, E 3): the way a ball machine shoots,
 * the way the runners go over a hurdle or out of the blocks, the way the shot is put; for the
 * hurdle bits 2-3 its height (0 0.762 m, the women's 400 m; 1 0.838 m, the 100 m; 2 0.914 m, the
 * men's 400 m; 3 1.067 m, the 110 m), which a sneak-click with an empty hand steps. The toe board
 * stands on the circle's centre cell and is drawn on the circle's front edge.</p>
 *
 * <p>The lane rope's facing says where it runs, not which way it faces: north along z through
 * the middle of the block, east along x through the middle, south along z on the block's west
 * edge, west along x on its north edge (a rope between 2.5 m lanes falls on a block's middle or
 * on its edge in turn). Its model hangs into the water block below, its floats at the water
 * line, so it is placed in the air above the water.</p>
 *
 * @since 2026.10
 */
public class BlockSportFixture extends BlockVenue {

  /** Which fixture. */
  public enum Kind {
    BALL_MACHINE(1, Material.IRON, SoundType.METAL, box(3, 0, 1.5, 13, 15, 14.5), true),
    HURDLE(4, Material.IRON, SoundType.METAL, box(0, 0, 4, 16, 12, 15), true,
        "csm.venues.track.hurdle."),
    STARTING_BLOCK(1, Material.IRON, SoundType.METAL, box(4.5, 0, 1, 11.5, 3, 15), false),
    TOE_BOARD(1, Material.WOOD, SoundType.WOOD, box(5, 0, 5, 11, 1, 11), false),
    /** A pool's end wall tile with the lane's target: bit 2 the stem 12 px from the left edge
     * rather than 4, bit 3 the top tile (the cross bar and the stem below it). */
    WALL_TARGET(4, Material.ROCK, SoundType.STONE, box(0, 0, 0, 16, 16, 16), true, null, true),
    /** A touch pad on a pool's end wall: bits 2-3 the piece as the swimmer sees it (0 whole,
     * 1 a gap in the middle, 2 a gap at the left edge, 3 a gap at the right edge). */
    TOUCHPAD(4, Material.ROCK, SoundType.STONE, box(0, 0, 0, 16, 16, 16), true, null, true),
    /** A lane rope's length at the water line: bits 2-3 its colour (0 blue, 1 green, 2 yellow,
     * 3 red); the facing says where it runs (see the class comment). */
    LANE_ROPE(4, Material.CLOTH, SoundType.CLOTH, box(0, 0, 0, 16, 1, 16), false,
        "csm.venues.pool.rope."),
    /** A pool ladder (variant 0) or grab rails alone (1), standing on the deck at the edge. */
    POOL_LADDER(2, Material.IRON, SoundType.METAL, box(1, 0, 0, 15, 14, 4), false),
    /** A golf flagstick in the cup below: bits 2-3 the flag (0 red, 1 white, 2 blue, 3
     * yellow). */
    FLAGSTICK(4, Material.WOOD, SoundType.WOOD, box(7, 0, 7, 9, 16, 9), false,
        "csm.venues.golf.pin."),
    /** A pair of tee markers: bits 2-3 the tee (0 red, 1 white, 2 blue, 3 gold). */
    TEE_MARKER(4, Material.WOOD, SoundType.WOOD, box(1, 0, 6, 15, 4, 10), false,
        "csm.venues.golf.tee."),
    /** A yardage marker: bits 2-3 0 100 yd (red), 1 150 (white), 2 200 (blue), 3 250
     * (yellow). */
    YARDAGE_MARKER(4, Material.WOOD, SoundType.WOOD, box(5, 0, 5, 11, 12, 11), false,
        "csm.venues.golf.yards."),
    BALL_WASHER(1, Material.IRON, SoundType.METAL, box(5, 0, 5, 11, 15, 11), true),
    /** A driving range bay's hitting mat. */
    RANGE_MAT(1, Material.CLOTH, SoundType.CLOTH, box(0, 0, 0, 16, 1.5, 16), true),
    /** The partition between two driving range bays. */
    RANGE_DIVIDER(1, Material.WOOD, SoundType.WOOD, box(7, 0, 0, 9, 16, 16), true),
    /** A bucket of range balls. */
    RANGE_BUCKET(1, Material.IRON, SoundType.METAL, box(5, 0, 5, 11, 6, 11), false),
    /** A driving range's distance flag: bits 2-3 0 50 yd, 1 100, 2 150, 3 200. */
    RANGE_FLAG(4, Material.WOOD, SoundType.WOOD, box(6, 0, 6, 10, 16, 10), false,
        "csm.venues.golf.range."),
    /** A mini-golf hump: a carpeted rise across the lane. */
    MINIGOLF_HUMP(1, Material.CLOTH, SoundType.CLOTH, box(0, 0, 0, 16, 3, 16), true),
    /** A mini-golf scorecard and pencil stand. */
    SCORECARD_STAND(1, Material.WOOD, SoundType.WOOD, box(3, 0, 5, 13, 16, 11), true);

    final int variants;
    final Material material;
    final SoundType sound;
    final AxisAlignedBB box;
    final boolean solid;
    /** The lang key prefix a sneak-click's message names a variant by, or null for no
     * click. */
    final String clickKey;
    /** A whole opaque block (a wall tile). */
    final boolean full;

    Kind(int variants, Material material, SoundType sound, AxisAlignedBB box, boolean solid) {
      this(variants, material, sound, box, solid, null, false);
    }

    Kind(int variants, Material material, SoundType sound, AxisAlignedBB box, boolean solid,
        String clickKey) {
      this(variants, material, sound, box, solid, clickKey, false);
    }

    Kind(int variants, Material material, SoundType sound, AxisAlignedBB box, boolean solid,
        String clickKey, boolean full) {
      this.variants = variants;
      this.material = material;
      this.sound = sound;
      this.box = box;
      this.solid = solid;
      this.clickKey = clickKey;
      this.full = full;
    }
  }

  /** The hurdle's heights, in pixels. */
  static final double[] HURDLE_PX = {12.19, 13.41, 14.62, 17.07};

  private static final ThreadLocal<Kind> PENDING = new ThreadLocal<>();

  private PropertyInteger variant;
  private final Kind kind;

  public BlockSportFixture(String registryName, Kind kind) {
    super(stash(registryName, kind), kind.material, kind.sound, 1.0F);
    this.kind = kind;
    PENDING.remove();
    if (kind.full) {
      fullBlock = true;
      setLightOpacity(255);
    }
    IBlockState s = blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH);
    if (variant != null) {
      s = s.withProperty(variant, 0);
    }
    setDefaultState(s);
  }

  private static String stash(String registryName, Kind kind) {
    PENDING.set(kind);
    return registryName;
  }

  private static AxisAlignedBB box(double x0, double y0, double z0, double x1, double y1,
      double z1) {
    return new AxisAlignedBB(x0 / 16.0, y0 / 16.0, z0 / 16.0, x1 / 16.0, y1 / 16.0, z1 / 16.0);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    Kind k = PENDING.get();
    if (k != null && k.variants > 1) {
      variant = PropertyInteger.create("variant", 0, k.variants - 1);
      return new CsmBlockStateContainer(this, FACING, variant);
    }
    return new CsmBlockStateContainer(this, FACING);
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    IBlockState s = getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3));
    if (variant != null) {
      s = s.withProperty(variant, Math.min(kind.variants - 1, (meta >> 2) & 3));
    }
    return s;
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getHorizontalIndex();
    return variant == null ? meta : meta | state.getValue(variant) << 2;
  }

  @Override
  public int damageDropped(IBlockState state) {
    return 0;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return kind != null && kind.full;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return kind != null && kind.full;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return kind != null && kind.full ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return kind != null && kind.full ? BlockRenderLayer.SOLID : BlockRenderLayer.CUTOUT;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (kind == Kind.HURDLE && variant != null) {
      double h = HURDLE_PX[state.getValue(variant)] / 16.0;
      return new AxisAlignedBB(0, 0, kind.box.minZ, 1, Math.min(1.0, h + 0.05), kind.box.maxZ);
    }
    return kind.box;
  }

  @Nullable
  @Override
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return kind.solid ? super.getCollisionBoundingBox(state, source, pos) : NULL_AABB;
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    if (variant == null || kind.clickKey == null || !player.isSneaking()
        || !player.getHeldItem(hand).isEmpty()) {
      return false;
    }
    if (!world.isRemote) {
      int v = (state.getValue(variant) + 1) % kind.variants;
      world.setBlockState(pos, state.withProperty(variant, v), 3);
      player.sendStatusMessage(new TextComponentTranslation(kind.clickKey + v), true);
    }
    return true;
  }
}
