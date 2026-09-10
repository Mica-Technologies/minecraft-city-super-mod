package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * The piece that carries one rail section into another: W-beam at its left-hand end, thrie beam at
 * its right.
 *
 * <p>Real runs do not butt a W-beam against a thrie beam. The two sections are different depths, so
 * meeting them directly leaves a step, and highway hardware has a transition piece for exactly this
 * — a length whose corrugations morph from one profile into the other.</p>
 *
 * <p>It is the reason {@link ICsmGuardrailRail} asks which rail a block presents at each END rather
 * than what rail it is. A guardrail joins on the rail, so a block that is two different rails would
 * otherwise match neither neighbour and a run would stop dead at it — which is the very thing the
 * transition exists to prevent.</p>
 *
 * @version 1.0
 * @see ICsmGuardrailRail#getRailKindOnLeft()
 * @since 2026.9
 */
public class BlockGuardrailTransition extends BlockGuardrail {

  private final String leftKind;
  private final String rightKind;

  /**
   * Constructs a transition between two rail sections.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   * @param leftKind     the rail presented at the left-hand end
   * @param rightKind    the rail presented at the right-hand end
   *
   * @since 1.0
   */
  public BlockGuardrailTransition(String registryName, AxisAlignedBB boundingBox, String leftKind,
      String rightKind) {
    // The inherited rail kind is the LEFT one, so anything that asks the old single-kind question
    // gets a sensible answer rather than null. Nothing in the join path asks it any more.
    super(registryName, boundingBox, leftKind);
    this.leftKind = leftKind;
    this.rightKind = rightKind;
    // The superclass set every property it knows about, but not this one, and PropertyBool's first
    // allowed value is TRUE — so without this every transition would default to its mirror image.
    setDefaultState(getDefaultState().withProperty(MIRRORED, false));
  }

  @Override
  public String getRailKindOnLeft() {
    return leftKind;
  }

  @Override
  public String getRailKindOnRight() {
    return rightKind;
  }

  /**
   * {@inheritDoc}
   *
   * <p>Either rail, at either end. A transition serves a run going W-to-thrie and one going
   * thrie-to-W alike; which way round it is drawn is {@link #MIRRORED}, resolved from where the
   * two rails actually are.</p>
   */
  @Override
  public boolean acceptsRail(String kind, boolean onLeftEnd) {
    return leftKind.equals(kind) || rightKind.equals(kind);
  }

  /**
   * Whether the transition is drawn the other way round.
   *
   * <p>True when the run reads right-to-left through it — the rail this piece was drawn to end
   * with is on its LEFT. Derived rather than set, like the end treatments' mirror, so there is no
   * wrong way to place one.</p>
   *
   * @since 1.0
   */
  public static final PropertyBool MIRRORED = BlockGuardrailEnd.MIRRORED;

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POST, MIRRORED,
        WorkZoneJoins.CONNECT_LEFT, WorkZoneJoins.CONNECT_RIGHT, WorkZoneJoins.DIAG_FILL,
        GuardrailJoins.SLOPE);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Adds the mirror on top of everything a rail resolves: the transition is drawn reversed when
   * the rail it ends with is found on its left rather than its right.</p>
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    IBlockState resolved = super.getActualState(state, access, pos);
    return resolved.withProperty(MIRRORED,
        GuardrailJoins.presentsRailOnLeft(rightKind, resolved, access, pos));
  }
}
