package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurfaceRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmTrafficPoleIgnored;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * One cell of a crash cushion: the impact attenuator at the end of a barrier or in a gore point.
 *
 * <p>Laid the way a guardrail run is, because a real cushion is twenty to thirty feet long and a
 * single block would read as a toy beside the rail it terminates. A nose block goes at the front
 * and as many bays behind it as the site wants, and the last bay reads straight into whatever run
 * it protects.</p>
 *
 * <p>The arrangement is deliberately ONE WAY:</p>
 *
 * <pre>    nose --&gt; bay --&gt; bay --&gt; any rail</pre>
 *
 * <p>A bay presents {@code crash_cushion} at both ends, accepts only that on its LEFT, and accepts
 * any rail at all on its RIGHT — which is what lets one cushion terminate a W-beam, a thrie beam, a
 * box beam or a cable run without four different backups. The nose presents something nothing
 * accepts at its impact face, so no run can read into a cushion from the wrong end.</p>
 *
 * <p>Its own class rather than a {@link BlockGuardrail} subclass because a cushion has no post.
 * That block keeps the post in meta and lets {@link ItemGuardrailTool} toggle it; a state bit
 * nothing draws is a state bit somebody eventually toggles by accident, and the tool passes over
 * this block for the same reason.</p>
 *
 * @version 1.0
 * @see GuardrailJoins
 * @since 2026.9
 */
public class BlockCrashCushion extends AbstractBlockRoadSurfaceRotatableHZEight
    implements ICsmGuardrailRail, ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored {

  /** The section a cushion presents to its own kind, at both ends of a bay. */
  public static final String CUSHION = "crash_cushion";

  /** What the nose presents at its impact face. Nothing accepts it, which is the point. */
  public static final String NOSE = "crash_cushion_nose";

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final boolean nose;

  /**
   * Constructs a crash cushion bay.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   *
   * @since 1.0
   */
  public BlockCrashCushion(String registryName, AxisAlignedBB boundingBox) {
    this(registryName, boundingBox, false);
  }

  /**
   * Constructs a crash cushion cell.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   * @param nose         true for the front cell, whose left-hand end is the impact face
   *
   * @since 1.0
   */
  public BlockCrashCushion(String registryName, AxisAlignedBB boundingBox, boolean nose) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 0, 1.2F, 6F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.nose = nose;
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, DirectionEight.N)
        .withProperty(WorkZoneJoins.CONNECT_LEFT, false)
        .withProperty(WorkZoneJoins.CONNECT_RIGHT, false)
        .withProperty(WorkZoneJoins.DIAG_FILL, false)
        .withProperty(GuardrailJoins.SLOPE, GuardrailSlope.FLAT));
  }

  private static Material initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  public String getRailKind() {
    return CUSHION;
  }

  /**
   * {@inheritDoc}
   *
   * <p>The nose's left-hand end is the impact face, so it presents something no rail carries and
   * no block accepts. That is the whole mechanism stopping a run reading into a cushion backwards:
   * there is nothing to say no to, because there is nothing to match.</p>
   */
  @Override
  public String getRailKindOnLeft() {
    return nose ? NOSE : CUSHION;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Any rail at all on the RIGHT, and only a cushion on the LEFT.</p>
   *
   * <p>The permissive right-hand end is what lets one cushion terminate every rail family, and it
   * costs nothing: {@link GuardrailJoins} asks both blocks whether either accepts the other, so a
   * W-beam that has never heard of a cushion still joins one. The one thing refused there is
   * another cushion's NOSE, which would be a cushion laid back to front.</p>
   */
  @Override
  public boolean acceptsRail(String kind, boolean onLeftEnd) {
    if (onLeftEnd) {
      return CUSHION.equals(kind);
    }
    return !NOSE.equals(kind);
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING,
        WorkZoneJoins.CONNECT_LEFT, WorkZoneJoins.CONNECT_RIGHT, WorkZoneJoins.DIAG_FILL,
        GuardrailJoins.SLOPE);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Resolved from the neighbours every time rather than stored, so cutting a bay out of the
   * middle of a cushion closes the two halves up and re-grading the ground under one re-ramps it,
   * without anything having to be notified.</p>
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    return GuardrailJoins.resolve(this, super.getActualState(state, access, pos), access, pos);
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  /**
   * {@inheritDoc}
   *
   * <p>A cushion is a steel box on a frame, so it stops the player at the same height the rails it
   * terminates do rather than at the height of its own panels. See
   * {@link GuardrailJoins#standTall}.</p>
   */
  @Override
  @Nullable
  public AxisAlignedBB getCollisionBoundingBox(@Nonnull IBlockState state,
      @Nonnull IBlockAccess source, @Nonnull BlockPos pos) {
    return GuardrailJoins.standTall(getBoundingBox(state, source, pos));
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
