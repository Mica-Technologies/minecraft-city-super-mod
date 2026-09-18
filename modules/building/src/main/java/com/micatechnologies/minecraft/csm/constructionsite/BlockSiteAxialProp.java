package com.micatechnologies.minecraft.csm.constructionsite;

import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.util.math.AxisAlignedBB;

/**
 * A construction-site prop that lies along one horizontal axis and has no other state: the
 * material pallets, lumber stack, pipe and conduit bundles, insulation rolls and the wire spool.
 *
 * <p>They differ in nothing but a registry name, a model, a box and what they are made of, so
 * there is one class, constructed with those, as {@link BlockSiteProp} is for the props that do
 * not turn. The name is handed across on the thread for the same reason. The models come from
 * {@code dev-env-utils/scripts/gen_logistics.py}.</p>
 *
 * @version 1.0
 * @since 2026.9
 */
public class BlockSiteAxialProp extends AbstractBlockSiteAxial {

  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;
  private final AxisAlignedBB runBox;

  /**
   * Constructs a {@link BlockSiteAxialProp} that runs along the placer's line of sight, the way a
   * load is set down in front of you.
   *
   * @param registryName the registry name
   * @param material     the material
   * @param soundType    the sound type
   * @param tool         the harvest tool class
   * @param runBox       the box, running along x, in block units
   *
   * @since 1.0
   */
  public BlockSiteAxialProp(String registryName, Material material, SoundType soundType,
      String tool, AxisAlignedBB runBox) {
    super(pendingMaterial(registryName, material), soundType, tool);
    this.registryName = registryName;
    this.runBox = runBox;
    PENDING_REGISTRY_NAME.remove();
  }

  private static Material pendingMaterial(String registryName, Material material) {
    PENDING_REGISTRY_NAME.set(registryName);
    return material;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING_REGISTRY_NAME.get();
  }

  @Override
  protected AxisAlignedBB getRunBox() {
    return runBox;
  }

  @Override
  protected boolean runsAlongLook() {
    return true;
  }
}
