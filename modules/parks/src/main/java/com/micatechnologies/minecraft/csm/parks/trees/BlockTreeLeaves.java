package com.micatechnologies.minecraft.csm.parks.trees;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A tree leaves block: one species (its texture) and one {@link TreeLeafType} (its shape).
 *
 * <p>Drawn as leaf cards with a fringe past every open face ({@link TreeLeavesGeometry}), so a
 * crown of these reads as foliage and a one-wide column as a columnar tree. It never decays --
 * a street tree that vanished would wreck a build -- and has no collision, so a canopy over a
 * sidewalk does not block it; its selection box is the full block, so it is still easy to break.
 * The open faces and a per-position variant reach the model through {@link #SHAPE} in the extended
 * state; nothing is a listed property.</p>
 *
 * @since 2026.9
 */
public class BlockTreeLeaves extends AbstractBlock implements ICsmTreeLeaves {

  /** {@link TreeLeavesGeometry#key}: open faces and variant, for the client model. */
  public static final IUnlistedProperty<Integer> SHAPE = new IUnlistedProperty<Integer>() {
    @Override
    public String getName() {
      return "shape";
    }

    @Override
    public boolean isValid(Integer value) {
      return value != null;
    }

    @Override
    public Class<Integer> getType() {
      return Integer.class;
    }

    @Override
    public String valueToString(Integer value) {
      return Integer.toString(value);
    }
  };

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final TreeLeafType type;
  private final String texture;

  /**
   * Constructs a leaves block.
   *
   * @param registryName its registry name ({@code tree_leaves_<species>[_<season>]})
   * @param type         how its cards are arranged
   * @param texture      its leaf-cluster sprite, e.g. {@code csm:blocks/parks/leaves_elm}
   */
  public BlockTreeLeaves(String registryName, TreeLeafType type, String texture) {
    super(stash(registryName), SoundType.PLANT, null, 0, 0.2F, 1.0F, 0.0F, 1);
    this.registryName = registryName;
    this.type = type;
    this.texture = texture;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.LEAVES;
  }

  public TreeLeafType getLeafType() {
    return type;
  }

  public String getTexture() {
    return texture;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  // --- state ---

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[0],
        new IUnlistedProperty[]{SHAPE});
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    int key = shape(world, pos);
    if (type.isPalm()) {
      key &= ~63; // a crown's fronds do not depend on its neighbours; only the variant counts
    }
    return ((IExtendedBlockState) state).withProperty(SHAPE, key);
  }

  /** The shape key where this block stands. */
  public static int shape(IBlockAccess world, BlockPos pos) {
    int open = 0;
    for (EnumFacing f : EnumFacing.values()) {
      BlockPos p = pos.offset(f);
      IBlockState s = world.getBlockState(p);
      // Open: no leaves beyond, and nothing solid for the fringe to poke into.
      if (!TreeLogConnections.isLeaves(s.getBlock()) && !s.isOpaqueCube()) {
        open |= 1 << f.getIndex();
      }
    }
    int variant = (int) ((MathHelper.getPositionRandom(pos) >> 16) & 3L);
    return TreeLeavesGeometry.key(open, variant);
  }

  // --- behaviour ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return FULL_BLOCK_AABB;
  }

  @Override
  @Nullable
  @SuppressWarnings("deprecation")
  public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    return NULL_AABB;
  }

  @Override
  public boolean isPassable(IBlockAccess world, BlockPos pos) {
    return true;
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
    return BlockFaceShape.UNDEFINED;
  }

  @Override
  @SuppressWarnings("deprecation")
  public boolean causesSuffocation(IBlockState state) {
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
    return BlockRenderLayer.CUTOUT_MIPPED;
  }

  @Override
  public boolean isLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public boolean isFoliage(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 60;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 30;
  }
}
