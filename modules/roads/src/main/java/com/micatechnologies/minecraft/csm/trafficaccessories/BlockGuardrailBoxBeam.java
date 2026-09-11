package com.micatechnologies.minecraft.csm.trafficaccessories;

import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A box beam guardrail cell: {@link BlockGuardrail}, plus whether it is standing on another rail.
 *
 * <p>Box beam is the one rail whose post stands on a visible bolted BASE PLATE. Every guardrail now
 * tops out at the top of its cell, so a guardrail stacked on another continues its post — and a
 * base plate half way up a post reads as a splice nobody would build. This cell therefore resolves
 * {@link #STACKED} from the block under it, and the blockstate leaves the plate off when it is set.
 * That is what lets a two-tube box beam on a one-tube one read as a single three-tube bridge
 * rail.</p>
 *
 * <p>Its own class rather than a property on every guardrail, because none of the others has a
 * plate to leave off, and a property multiplies the block-state count of whatever carries it.</p>
 *
 * @version 1.0
 * @see BlockGuardrail
 * @since 2026.9
 */
public class BlockGuardrailBoxBeam extends BlockGuardrail {

  /**
   * Whether this cell stands on a guardrail cell that has a post, so its own post continues that
   * one and carries no base plate. Derived in {@link #getActualState}, never stored.
   *
   * @since 1.0
   */
  public static final PropertyBool STACKED = PropertyBool.create("stacked");

  /**
   * Constructs a box beam guardrail block.
   *
   * @param registryName the registry name of the block
   * @param boundingBox  its bounding box at the default facing, before it settles
   * @param railKind     the rail section; two guardrails join only if these match
   *
   * @since 1.0
   */
  public BlockGuardrailBoxBeam(String registryName, AxisAlignedBB boundingBox, String railKind) {
    super(registryName, boundingBox, railKind);
    // PropertyBool's first allowed value is TRUE, so the base state would otherwise be stacked.
    setDefaultState(getDefaultState().withProperty(STACKED, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, POST,
        WorkZoneJoins.CONNECT_LEFT, WorkZoneJoins.CONNECT_RIGHT, WorkZoneJoins.DIAG_FILL,
        GuardrailJoins.SLOPE, STACKED);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Adds {@link #STACKED}: set when the block below is a guardrail cell with a post. A cell
   * below without one leaves nothing for this post to continue, so the plate stays.</p>
   */
  @Override
  @Nonnull
  public IBlockState getActualState(@Nonnull IBlockState state, @Nonnull IBlockAccess access,
      @Nonnull BlockPos pos) {
    IBlockState actual = super.getActualState(state, access, pos);
    IBlockState below = access.getBlockState(pos.down());
    boolean stacked = below.getBlock() instanceof BlockGuardrail && below.getValue(POST);
    return actual.withProperty(STACKED, stacked);
  }
}
