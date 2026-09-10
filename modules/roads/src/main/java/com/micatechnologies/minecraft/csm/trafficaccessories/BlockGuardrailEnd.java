package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurfaceRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * How a guardrail run stops: a flared shoe, an impact head, an energy-absorbing terminal, or the
 * rail simply turned down into the ground.
 *
 * <p>Each is its own block placed in the cell PAST the last rail and turned to face into the run.
 * They are blocks rather than a property on the rail for two reasons: every style is completely
 * different geometry, and most of them reach outside a single cell, which a property on the rail
 * could not do without the rail owning a shape it does not have.</p>
 *
 * <p>An end carries the same {@link ICsmGuardrailRail#getRailKind() rail kind} as the run it
 * terminates, so the rail beside it joins to it — but {@link #isRunEnd()} is true, because a run
 * does not continue out the far side of a flared shoe.</p>
 *
 * @version 1.0
 * @see BlockGuardrail
 * @since 2026.9
 */
public class BlockGuardrailEnd extends AbstractBlockRoadSurfaceRotatableHZEight
    implements ICsmGuardrailRail, ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /**
   * Whether this end is the mirror image of itself.
   *
   * <p>An end treatment is chiral: a flared shoe curves away to one side, and the shoe that
   * finishes the right-hand end of a run is not the same shape as the one that finishes the left.
   * Turning it round does not fix that, because facing on a guardrail means the way the rail LOOKS
   * — turn an end through 180 degrees and its rail ends up on the far side of the cell, pointing
   * away from the road.</p>
   *
   * <p>So the mirror is a reflection about the run axis, and it is DERIVED rather than set: the
   * block looks for the run and mirrors itself if the rail is on its right instead of its left.
   * There is no wrong way to place one, which is the same bargain every other join in this tab
   * makes.</p>
   *
   * @since 1.0
   */
  public static final PropertyBool MIRRORED = PropertyBool.create("mirrored");

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final String railKind;

  /**
   * Constructs a W-beam end treatment.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   *
   * @since 1.0
   */
  public BlockGuardrailEnd(String registryName, AxisAlignedBB boundingBox) {
    this(registryName, boundingBox, "w_beam");
  }

  /**
   * Constructs an end treatment for a named rail section.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   * @param railKind     the rail section this terminates
   *
   * @since 1.0
   */
  public BlockGuardrailEnd(String registryName, AxisAlignedBB boundingBox, String railKind) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 0, 1.2F, 6F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.railKind = railKind;
    // Set explicitly: PropertyBool's first allowed value is TRUE, so a base state left alone would
    // default every end treatment to its mirror image.
    setDefaultState(this.blockState.getBaseState().withProperty(MIRRORED, false));
  }

  /**
   * Stashes the registry name for {@link #getBlockRegistryName()} and returns the material.
   *
   * @param name the registry name of the block
   *
   * @return the material of the block
   *
   * @since 1.0
   */
  protected static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public String getRailKind() {
    return railKind;
  }

  @Override
  public boolean isRunEnd() {
    return true;
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, MIRRORED);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Meta carries the facing alone. The mirror is resolved from the run every time it is asked
   * for, so a shoe put down before its guardrail turns itself the right way round the moment the
   * rail arrives beside it.</p>
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    return GuardrailJoins.resolveEnd(this, super.getActualState(state, access, pos), access, pos);
  }

  /**
   * {@inheritDoc}
   *
   * <p>The box is reflected along with the model. The generator measures one box, off the
   * unmirrored mesh, and a mirrored shoe occupies the opposite half of its cell — so handing back
   * the same box either way would leave the clickable volume beside the shape rather than on it,
   * which is the sort of fault nobody sees until they try to break one.</p>
   */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (!isMirrored(state, source, pos)) {
      return boundingBox;
    }
    return new AxisAlignedBB(1.0 - boundingBox.maxX, boundingBox.minY, boundingBox.minZ,
        1.0 - boundingBox.minX, boundingBox.maxY, boundingBox.maxZ);
  }

  /**
   * Whether this end is drawn mirrored, resolved from the run rather than read off the stored
   * state — which does not carry it, because the mirror is derived.
   */
  private boolean isMirrored(IBlockState state, IBlockAccess source, BlockPos pos) {
    if (state.getBlock() != this || source == null || pos == null) {
      return false;
    }
    IBlockState actual = getActualState(state, source, pos);
    return actual.getPropertyKeys().contains(MIRRORED) && actual.getValue(MIRRORED);
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

  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.CUTOUT_MIPPED;
  }
}
