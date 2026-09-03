package com.micatechnologies.minecraft.csm.trafficsigns;

/**
 * A concrete, parameterized redstone-powered two-state traffic sign block. Mirrors
 * {@link BlockTrafficSign} but for {@link AbstractBlockPoweredSign}: the on/off face swap is
 * expressed entirely in the blockstate JSON, so a single class covers every powered sign and
 * instances differ only by their registry name.
 */
public class BlockPoweredTrafficSign extends AbstractBlockPoweredSign {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  public BlockPoweredTrafficSign(String registryName) {
    this(initRegistryName(registryName), registryName);
  }

  private BlockPoweredTrafficSign(Void ignored, String registryName) {
    this.registryName = registryName;
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
}
