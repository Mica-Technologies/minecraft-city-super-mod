package com.micatechnologies.minecraft.csm.parks.landscape;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A landscape block with nothing to decide at run time: its look is the JSON model its blockstate
 * names, and the constructor says how big it is, whether it can be walked through and how it
 * draws. One class, constructed by name, for the tree grates and pits, ground covers, shrubs,
 * ornamental grasses and flower beds (see {@code gen_park_plantings.py}).
 *
 * @since 2026.9
 */
public class BlockParkProp extends AbstractBlock {

  /** What a prop is, which decides everything but its size. */
  public enum Kind {
    /** A full ground block: tree grates and pits. Solid, opaque, a tree stands on it. */
    GROUND(Material.ROCK, SoundType.STONE, "pickaxe", 1.5F, BlockRenderLayer.CUTOUT, true, true),
    /** A thin layer on the ground: mulch, gravel, turf. Walkable, like a carpet. */
    COVER(Material.GROUND, SoundType.GROUND, "shovel", 0.4F, BlockRenderLayer.CUTOUT, true,
        false),
    /** A planter box: solid to its own size, soil on top. */
    PLANTER(Material.ROCK, SoundType.STONE, "pickaxe", 1.5F, BlockRenderLayer.CUTOUT, true,
        false),
    /** A shrub: solid, but only its own size. */
    SHRUB(Material.LEAVES, SoundType.PLANT, null, 0.3F, BlockRenderLayer.CUTOUT_MIPPED, true,
        false),
    /** A plant to walk through: grasses and flower beds. */
    PLANT(Material.PLANTS, SoundType.PLANT, null, 0.1F, BlockRenderLayer.CUTOUT, false, false);

    final Material material;
    final SoundType sound;
    final String tool;
    final float hardness;
    final BlockRenderLayer layer;
    final boolean collides;
    final boolean full;

    Kind(Material material, SoundType sound, String tool, float hardness, BlockRenderLayer layer,
        boolean collides, boolean full) {
      this.material = material;
      this.sound = sound;
      this.tool = tool;
      this.hardness = hardness;
      this.layer = layer;
      this.collides = collides;
      this.full = full;
    }
  }

  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final Kind kind;
  private final AxisAlignedBB box;

  /**
   * Constructs a prop.
   *
   * @param registryName its registry name
   * @param kind         what it is
   * @param height       its height in sixteenths (16 for a ground block)
   * @param inset        how far its box stands in from each side, in sixteenths
   */
  public BlockParkProp(String registryName, Kind kind, int height, int inset) {
    super(stash(registryName, kind), kind.sound, kind.tool, 0, kind.hardness, kind.hardness * 2,
        0.0F, kind.full ? 255 : 0);
    this.registryName = registryName;
    this.kind = kind;
    this.box = new AxisAlignedBB(inset / 16.0, 0, inset / 16.0, 1 - inset / 16.0, height / 16.0,
        1 - inset / 16.0);
    PENDING.remove();
  }

  private static Material stash(String registryName, Kind kind) {
    PENDING.set(new Object[]{registryName, kind});
    return kind.material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : (String) PENDING.get()[0];
  }

  /** The kind; also answers during the Block constructor, which asks before the field is set. */
  public Kind getKind() {
    return kind != null ? kind : (Kind) PENDING.get()[1];
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return box;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return getKind().collides ? box : NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return !getKind().collides;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return getKind().full;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return getKind().full;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return getKind().full ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return getKind().layer;
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return getKind() == Kind.SHRUB || getKind() == Kind.PLANT ? 60 : 0;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return getKind() == Kind.SHRUB || getKind() == Kind.PLANT ? 30 : 0;
  }
}
