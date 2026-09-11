package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockRoadSurfaceRotatableHZEight;
import com.micatechnologies.minecraft.csm.codeutils.DirectionEight;
import com.micatechnologies.minecraft.csm.codeutils.ICsmNoSnowAccumulation;
import com.micatechnologies.minecraft.csm.codeutils.ICsmPostPassesThrough;
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
 * One cell of guardrail: the rail, its block-out, and a post that may or may not be there.
 *
 * <p>One class covers every rail block, the way {@link BlockWorkZoneDevice} covers every plain work
 * zone device — the variants differ in their MODEL, not their behaviour. Post material and whether
 * the rail is on one side of the post or both are separate blocks rather than state on one block,
 * because a guardrail run is long and a tile entity per cell puts thousands of them on a highway.
 * The configuration tool swaps a block for its sibling, which costs nothing to store.</p>
 *
 * <p>Only the post rides in meta, in the one bit eight facings leave over. Everything else is
 * derived from the neighbours in {@link #getActualState}: the two connections, the diagonal gap
 * filler, and the slope. See {@link GuardrailJoins} for why a guardrail joins on the rail rather
 * than on block identity, and why a neighbour one block up still counts.</p>
 *
 * @version 1.0
 * @see GuardrailJoins
 * @since 2026.9
 */
public class BlockGuardrail extends AbstractBlockRoadSurfaceRotatableHZEight
    implements ICsmGuardrailRail, ICsmNoSnowAccumulation, ICsmTrafficPoleIgnored,
    ICsmPostPassesThrough {

  /**
   * Whether this cell carries a post.
   *
   * <p>The only option that is stored rather than derived, and the only one there is room for:
   * eight facings take three of meta's four bits. Real posts stand about every other cell rather
   * than at every one, so this is the option a builder reaches for most.</p>
   *
   * @since 1.0
   */
  public static final PropertyBool POST = PropertyBool.create("post");

  /** Meta bit the post occupies, above the three the facing needs. */
  private static final int POST_BIT = 0b1000;

  /** Meta bits the facing occupies. */
  private static final int FACING_MASK = 0b0111;

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final String railKind;

  /**
   * Constructs a guardrail block.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   *
   * @since 1.0
   */
  public BlockGuardrail(String registryName, AxisAlignedBB boundingBox) {
    this(registryName, boundingBox, "w_beam");
  }

  /**
   * Constructs a guardrail block carrying a named rail section.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   * @param railKind     the rail section; two guardrails join only if these match
   *
   * @since 1.0
   */
  public BlockGuardrail(String registryName, AxisAlignedBB boundingBox, String railKind) {
    super(initRegistryName(registryName), SoundType.METAL, "pickaxe", 0, 1.2F, 6F, 0F, 0);
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.railKind = railKind;
    setDefaultState(this.blockState.getBaseState()
        .withProperty(FACING, DirectionEight.N)
        .withProperty(POST, true)
        .withProperty(WorkZoneJoins.CONNECT_LEFT, false)
        .withProperty(WorkZoneJoins.CONNECT_RIGHT, false)
        .withProperty(WorkZoneJoins.DIAG_FILL, false)
        .withProperty(GuardrailJoins.SLOPE, GuardrailSlope.FLAT));
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
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POST,
        WorkZoneJoins.CONNECT_LEFT, WorkZoneJoins.CONNECT_RIGHT, WorkZoneJoins.DIAG_FILL,
        GuardrailJoins.SLOPE);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Facing in the low three bits, post in the fourth. The superclass packs the facing alone, so
   * both halves of the encoding are overridden here rather than one — a block that writes a bit it
   * cannot read back loses the post every time its chunk is unloaded.</p>
   */
  @Override
  public int getMetaFromState(IBlockState state) {
    int meta = state.getValue(FACING).getIndex() & FACING_MASK;
    return state.getValue(POST) ? meta | POST_BIT : meta;
  }

  @Override
  @Nonnull
  public IBlockState getStateFromMeta(int meta) {
    DirectionEight[] directions = DirectionEight.values();
    int index = meta & FACING_MASK;
    DirectionEight facing = index < directions.length ? directions[index] : DirectionEight.N;
    return getDefaultState()
        .withProperty(FACING, facing)
        .withProperty(POST, (meta & POST_BIT) != 0);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Resolved from the neighbours every time rather than stored, so cutting a guardrail out of
   * the middle of a run closes the two halves up, and re-grading the ground under one re-ramps the
   * rail, without anything having to be notified.</p>
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
   * <p>Taller than the rail is drawn, which is the same thing a vanilla fence does and for the
   * same reason: a barrier the player can hop over is not a barrier. A guardrail's rail tops out
   * around three quarters of a block, well inside a standing jump, so the box carries on up past
   * where anything is drawn.</p>
   *
   * <p>Measured from the box's own FLOOR rather than from the cell's, so a run settled onto a snow
   * layer or a sloped road stands its full height above the ground the player is actually walking
   * on instead of losing that much of it.</p>
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
