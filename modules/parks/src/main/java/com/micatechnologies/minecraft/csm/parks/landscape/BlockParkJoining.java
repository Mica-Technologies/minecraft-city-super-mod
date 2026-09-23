package com.micatechnologies.minecraft.csm.parks.landscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A landscape block that joins its neighbours of the same family in the four horizontal
 * directions, as a fence does: hedges, the hoop fence round a tree pit, and raised planting beds
 * (which draw their walls only on the outside of a run, so a row of them is one long bed).
 *
 * <p>The four sides are actual state, read from the neighbours and never stored, and the
 * multipart blockstate picks what to draw from them ({@code gen_park_plantings.py}).</p>
 *
 * @since 2026.9
 */
public class BlockParkJoining extends AbstractBlock {

  public static final PropertyBool NORTH = PropertyBool.create("north");
  public static final PropertyBool EAST = PropertyBool.create("east");
  public static final PropertyBool SOUTH = PropertyBool.create("south");
  public static final PropertyBool WEST = PropertyBool.create("west");

  /** What a joining block is, which decides its material and how it joins. */
  public enum Kind {
    /** A clipped hedge: a leafy wall, joins hedges of any species. */
    HEDGE(Material.LEAVES, SoundType.PLANT, null, 0.4F, BlockRenderLayer.CUTOUT_MIPPED),
    /** A low steel hoop fence, joins its own kind. */
    FENCE(Material.IRON, SoundType.METAL, "pickaxe", 2.0F, BlockRenderLayer.CUTOUT),
    /** A raised bed or fountain basin, joins its own block only. */
    BED(Material.ROCK, SoundType.STONE, "pickaxe", 1.5F, BlockRenderLayer.CUTOUT),
    /** A pergola's roof of beams and rafters: above head height, joins its own block only. */
    PERGOLA(Material.WOOD, SoundType.WOOD, "axe", 2.0F, BlockRenderLayer.CUTOUT);

    final Material material;
    final SoundType sound;
    final String tool;
    final float hardness;
    final BlockRenderLayer layer;

    Kind(Material material, SoundType sound, String tool, float hardness,
        BlockRenderLayer layer) {
      this.material = material;
      this.sound = sound;
      this.tool = tool;
      this.hardness = hardness;
      this.layer = layer;
    }
  }

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final Kind kind;
  private final double height;
  private final double half;

  /**
   * Constructs a joining block.
   *
   * @param registryName its registry name
   * @param kind         what it is
   * @param height       its height in sixteenths
   * @param width        how wide a run of it is, in sixteenths (16 for a bed, which fills its cell)
   */
  public BlockParkJoining(String registryName, Kind kind, int height, int width) {
    super(stash(registryName, kind), kind.sound, kind.tool, 0, kind.hardness,
        kind.hardness * 2, 0.0F, 0);
    this.registryName = registryName;
    this.kind = kind;
    this.height = height / 16.0;
    this.half = width / 32.0;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(NORTH, false)
        .withProperty(EAST, false).withProperty(SOUTH, false).withProperty(WEST, false));
  }

  private static Material stash(String registryName, Kind kind) {
    PENDING.set(registryName);
    return kind.material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  public Kind getKind() {
    return kind;
  }

  // --- state ---

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, NORTH, EAST, SOUTH, WEST);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return 0;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    return getDefaultState();
  }

  /** Whether this joins the block on the given side. */
  public boolean joins(IBlockAccess world, BlockPos pos, EnumFacing side) {
    IBlockState other = world.getBlockState(pos.offset(side));
    if (kind == Kind.BED && getBlockRegistryName().equals("fountain_basin")) {
      // A basin runs up to a fountain standing in it, so the fountain stands in the water.
      String name = other.getBlock().getRegistryName() == null ? ""
          : other.getBlock().getRegistryName().getPath();
      if (name.startsWith("fountain_")) {
        return true;
      }
    }
    if (!(other.getBlock() instanceof BlockParkJoining)) {
      return false;
    }
    BlockParkJoining o = (BlockParkJoining) other.getBlock();
    return kind == Kind.BED || kind == Kind.PERGOLA ? o == this : o.kind == kind;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return state.withProperty(NORTH, joins(world, pos, EnumFacing.NORTH))
        .withProperty(EAST, joins(world, pos, EnumFacing.EAST))
        .withProperty(SOUTH, joins(world, pos, EnumFacing.SOUTH))
        .withProperty(WEST, joins(world, pos, EnumFacing.WEST));
  }

  // --- shape ---

  /** Where it starts: a pergola roof sits on its posts, clear of the heads beneath it. */
  private double bottom() {
    return kind == Kind.PERGOLA ? 0.625 : 0;
  }

  private AxisAlignedBB post() {
    return new AxisAlignedBB(0.5 - half, bottom(), 0.5 - half, 0.5 + half, height, 0.5 + half);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return post();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    IBlockState s = getActualState(state, source, pos);
    double x0 = s.getValue(WEST) ? 0 : 0.5 - half;
    double x1 = s.getValue(EAST) ? 1 : 0.5 + half;
    double z0 = s.getValue(NORTH) ? 0 : 0.5 - half;
    double z1 = s.getValue(SOUTH) ? 1 : 0.5 + half;
    return new AxisAlignedBB(x0, bottom(), z0, x1, height, z1);
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos,
      AxisAlignedBB entityBox, List<AxisAlignedBB> boxes, @Nullable Entity entity,
      boolean isActualState) {
    IBlockState s = isActualState ? state : getActualState(state, world, pos);
    // A fence is jumped like a fence; a bed or hedge is climbed on like a slab or a wall.
    double top = kind == Kind.FENCE ? 1.5 : height;
    double b = bottom();
    addCollisionBoxToList(pos, entityBox, boxes,
        new AxisAlignedBB(0.5 - half, b, 0.5 - half, 0.5 + half, top, 0.5 + half));
    if (s.getValue(NORTH)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          new AxisAlignedBB(0.5 - half, b, 0, 0.5 + half, top, 0.5));
    }
    if (s.getValue(SOUTH)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          new AxisAlignedBB(0.5 - half, b, 0.5, 0.5 + half, top, 1));
    }
    if (s.getValue(WEST)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          new AxisAlignedBB(0, b, 0.5 - half, 0.5, top, 0.5 + half));
    }
    if (s.getValue(EAST)) {
      addCollisionBoxToList(pos, entityBox, boxes,
          new AxisAlignedBB(0.5, b, 0.5 - half, 1, top, 0.5 + half));
    }
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
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return face == EnumFacing.DOWN && kind != Kind.PERGOLA ? BlockFaceShape.SOLID
        : BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return kind.layer;
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return kind == Kind.HEDGE ? 60 : 0;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return kind == Kind.HEDGE ? 30 : 0;
  }
}
