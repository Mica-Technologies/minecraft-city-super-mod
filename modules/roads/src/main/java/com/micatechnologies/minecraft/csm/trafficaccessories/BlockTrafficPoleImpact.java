package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.ICsmModelStateRules;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * An impact resistant traffic pole: the thick pole's heavier sibling, a tube the full width of
 * its block, for the corner that keeps getting hit (GitHub #347).
 *
 * <p>It is a painted steel pole like the thin and thick ones in every way that matters to what
 * hangs on it: stacked to any height, a member of the {@code AbstractBlockTrafficPole} family
 * (so signals, arms and signs treat it as a pole), and footed, so it stands on the same choice
 * of bolted bases ({@link AbstractBlockTrafficPoleFooted}).
 *
 * <p><b>No mount stubs.</b> The other poles grow a stub from their skin to the face of their
 * block toward anything mounted beside them. This tube's skin <em>is</em> the face of its
 * block, so there is nothing to bridge, and the four mount properties never change what it
 * draws. They are still in the state, because the family's actual state sets them, but they are
 * left out of the model locations ({@link ICsmModelStateRules}): 10 variants a pole
 * instead of 160.
 *
 * <p><b>Accessories cut for a pole meet it at the thick pole's fit.</b> {@link #getPoleRadius}
 * reports 8, which {@code CsmPoleFit} rounds to its widest fit, the 12-across pole's. Geometry
 * cut for that pole ends two sixteenths inside this one, which hides the joint the way a saddle
 * cut would.
 *
 * <p>The model and the blockstates come from {@code dev-env-utils/scripts/gen_pole_bases.py}.
 *
 * @since 2026.11
 */
public class BlockTrafficPoleImpact extends AbstractBlockTrafficPoleFooted {

  /** The tube fills its block from side to side. */
  private static final AxisAlignedBB BOUNDING_BOX = new AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);

  /**
   * Carries the constructor arguments past {@code super()}: {@link #getBlockRegistryName()} is
   * called from the superclass constructor, before this class's fields exist. Same trick as
   * {@link BlockTrafficPolePedestal}.
   */
  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final TRAFFIC_POLE_COLOR color;

  /**
   * Constructs an impact resistant pole block.
   *
   * @param registryName the block's registry name
   * @param color        the pole's finish, reported to accessories that inherit a pole colour
   */
  public BlockTrafficPoleImpact(String registryName, TRAFFIC_POLE_COLOR color) {
    super(stash(registryName));
    this.registryName = registryName;
    this.color = color;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.ROCK;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public Class<?>[] getIgnoreBlock() {
    return null;
  }

  @Override
  public TRAFFIC_POLE_COLOR getTrafficPoleColor() {
    return color;
  }

  /** The tube is 16 across ({@code WIDTHS} in {@code gen_pole_bases.py}). */
  @Override
  public double getPoleRadius() {
    return 8.0D;
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return BOUNDING_BOX;
  }

  @Override
  public IProperty<?>[] getModelIgnoredProperties() {
    return new IProperty<?>[]{MOUNT_EAST, MOUNT_WEST, MOUNT_UP, MOUNT_DOWN};
  }
}
