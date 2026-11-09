package com.micatechnologies.minecraft.csm.lighting;

import com.micatechnologies.minecraft.csm.codeutils.ICsmPostTopFixture;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * Factory for creating {@link AbstractBrightLight} blocks that only differ in registry name,
 * bounding box, and X offset. Eliminates the need for a separate class file per light fixture.
 *
 * @since 2026.4
 */
public class BlockBrightLightFactory extends AbstractBrightLight {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB boundingBox;
  private final int brightLightXOffset;

  public BlockBrightLightFactory(String registryName, AxisAlignedBB boundingBox,
      int brightLightXOffset) {
    this(initRegistryName(registryName), registryName, boundingBox, brightLightXOffset);
  }

  private BlockBrightLightFactory(Void ignored, String registryName, AxisAlignedBB boundingBox,
      int brightLightXOffset) {
    this.registryName = registryName;
    this.boundingBox = boundingBox;
    this.brightLightXOffset = brightLightXOffset;
  }

  private static Void initRegistryName(String name) {
    PENDING_REGISTRY_NAME.set(name);
    return null;
  }

  @Override
  public String getBlockRegistryName() {
    if (registryName != null) {
      return registryName;
    }
    return PENDING_REGISTRY_NAME.get();
  }

  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
    return boundingBox;
  }

  @Override
  public int getBrightLightXOffset() {
    return brightLightXOffset;
  }

  /**
   * A light that sits on a pole top, slipped over its tenon: a concrete pole under it shows its
   * collar and tenon rather than a cap.
   *
   * <p>A nested flavour rather than a constructor flag for the reason {@link ICsmPostTopFixture}
   * gives: it is a fact about the block's type.
   *
   * @see ICsmPostTopFixture
   * @since 2026.9
   */
  public static class PostTop extends BlockBrightLightFactory implements ICsmPostTopFixture {

    public PostTop(String registryName, AxisAlignedBB boundingBox, int brightLightXOffset) {
      super(registryName, boundingBox, brightLightXOffset);
    }
  }

  /**
   * A post-top light whose head is cantilevered to one side of its pole (the GE Evolve EALS on its
   * pole-top knuckle): its offset runs forward, the way it faces, so the light falls under the
   * head whichever way the block is turned. The symmetric post tops ({@link PostTop}) keep their
   * world-X offset, so their placed lights stay where they are.
   *
   * @see AbstractBrightLight#isBrightLightOffsetFacingRelative()
   * @since 2026.11
   */
  public static class PostTopAimed extends PostTop {

    public PostTopAimed(String registryName, AxisAlignedBB boundingBox, int forwardOffset) {
      super(registryName, boundingBox, forwardOffset);
    }

    @Override
    public boolean isBrightLightOffsetFacingRelative() {
      return true;
    }
  }
}
