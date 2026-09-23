package com.micatechnologies.minecraft.csm.parks.trees;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlock;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

/**
 * A tree log: one wood, one width. Trees are built from these and leaves block by block, as
 * vanilla trees are.
 *
 * <p>A log connects to the logs and leaves around it and draws itself from what it finds
 * ({@link TreeLogConnections}, {@link TreeLogGeometry}): arms to face neighbours, a bridge to
 * each edge-diagonal log so a trunk stepped the vanilla way (- T / T -) reads as one leaning
 * trunk, a taper toward a thinner log, a flare onto the ground. The only stored state is the
 * axis it was placed along, used when it has no neighbours at all; everything else reaches the
 * client model through {@link #CONNECTIONS} in the extended state, which Forge does not
 * enumerate.</p>
 *
 * <p>One class for every wood and width; the constructor arguments ride past {@code super()} the
 * way {@code BlockTrafficPolePedestal}'s do, because the registry name is read from the
 * superclass constructor.</p>
 *
 * @since 2026.9
 */
public class BlockTreeLog extends AbstractBlock {

  /** The axis the log was placed along (meta 0-2). */
  public static final PropertyEnum<EnumFacing.Axis> AXIS = PropertyEnum.create("axis",
      EnumFacing.Axis.class);

  /** The connection mask ({@link TreeLogConnections}), for the client model. */
  public static final IUnlistedProperty<Long> CONNECTIONS = new IUnlistedProperty<Long>() {
    @Override
    public String getName() {
      return "connections";
    }

    @Override
    public boolean isValid(Long value) {
      return value != null;
    }

    @Override
    public Class<Long> getType() {
      return Long.class;
    }

    @Override
    public String valueToString(Long value) {
      return Long.toHexString(value);
    }
  };

  private static final ThreadLocal<Object[]> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final TreeWood wood;
  private final TreeLogWidth width;

  /**
   * Constructs a log.
   *
   * @param registryName the registry name, which must be {@link #name}(wood, width): it is spelled
   *                     out in the tab so the tools that read tab sources can find the block
   * @param wood         the wood
   * @param width        the width
   */
  public BlockTreeLog(String registryName, TreeWood wood, TreeLogWidth width) {
    super(stash(registryName, wood, width), SoundType.WOOD, "axe", 0, 2.0F, 5.0F, 0.0F,
        width == TreeLogWidth.FULL ? 255 : 0);
    this.wood = wood;
    this.width = width;
    this.registryName = registryName;
    PENDING.remove();
    setDefaultState(blockState.getBaseState().withProperty(AXIS, EnumFacing.Axis.Y));
  }

  private static Material stash(String registryName, TreeWood wood, TreeLogWidth width) {
    if (!registryName.equals(name(wood, width))) {
      throw new IllegalArgumentException(
          registryName + " is not the registry name of " + wood + " " + width);
    }
    PENDING.set(new Object[]{registryName});
    return Material.WOOD;
  }

  /** The registry name of a wood's log of a width: {@code tree_log_<wood>_<width>}. */
  public static String name(TreeWood wood, TreeLogWidth width) {
    return "tree_log_" + wood.getId() + "_" + width.getId();
  }

  public TreeWood getWood() {
    return wood;
  }

  public TreeLogWidth getWidth() {
    return width;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return (String) PENDING.get()[0];
  }

  // --- state ---

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new ExtendedBlockState(this, new net.minecraft.block.properties.IProperty[]{AXIS},
        new IUnlistedProperty[]{CONNECTIONS});
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateFromMeta(int meta) {
    EnumFacing.Axis[] axes = EnumFacing.Axis.values();
    return getDefaultState().withProperty(AXIS, axes[Math.min(meta, axes.length - 1)]);
  }

  @Override
  public int getMetaFromState(IBlockState state) {
    return state.getValue(AXIS).ordinal();
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getStateForPlacement(@Nonnull World world, @Nonnull BlockPos pos,
      @Nonnull EnumFacing facing, float hitX, float hitY, float hitZ, int meta,
      @Nonnull EntityLivingBase placer) {
    return getDefaultState().withProperty(AXIS, facing.getAxis());
  }

  @Override
  @Nonnull
  public IBlockState getExtendedState(@Nonnull IBlockState state, IBlockAccess world,
      BlockPos pos) {
    if (!(state instanceof IExtendedBlockState)) {
      return state;
    }
    return ((IExtendedBlockState) state).withProperty(CONNECTIONS, mask(state, world, pos));
  }

  /** The connection mask for this log where it stands. */
  public long mask(IBlockState state, IBlockAccess world, BlockPos pos) {
    return TreeLogConnections.compute(world, pos, state.getValue(AXIS));
  }

  // --- shape ---

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    AxisAlignedBB union = null;
    for (AxisAlignedBB box : TreeLogGeometry.boxes(width, mask(state, source, pos))) {
      union = union == null ? box : union.union(box);
    }
    return union == null ? FULL_BLOCK_AABB : union;
  }

  @Override
  @SuppressWarnings("deprecation")
  public void addCollisionBoxToList(@Nonnull IBlockState state, @Nonnull World world,
      @Nonnull BlockPos pos, @Nonnull AxisAlignedBB entityBox,
      @Nonnull List<AxisAlignedBB> collidingBoxes, @Nullable Entity entity, boolean actual) {
    for (AxisAlignedBB box : TreeLogGeometry.boxes(width, mask(state, world, pos))) {
      addCollisionBoxToList(pos, entityBox, collidingBoxes, box);
    }
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return width == TreeLogWidth.FULL;
  }

  @Override
  public boolean getBlockIsFullCube(IBlockState state) {
    return width == TreeLogWidth.FULL;
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public BlockFaceShape getBlockFaceShape(@Nonnull IBlockAccess world, @Nonnull IBlockState state,
      @Nonnull BlockPos pos, @Nonnull EnumFacing face) {
    return width == TreeLogWidth.FULL ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
  }

  @Override
  public boolean getBlockConnectsRedstone(IBlockState state, IBlockAccess access, BlockPos pos,
      @Nullable EnumFacing facing) {
    return false;
  }

  @Override
  @Nonnull
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.SOLID;
  }

  // --- wood ---

  @Override
  public boolean isWood(IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public boolean canSustainLeaves(IBlockState state, IBlockAccess world, BlockPos pos) {
    return true;
  }

  @Override
  public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 5;
  }

  @Override
  public int getFireSpreadSpeed(IBlockAccess world, BlockPos pos, EnumFacing face) {
    return 5;
  }
}
