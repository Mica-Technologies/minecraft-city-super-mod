package com.micatechnologies.minecraft.csm.trafficsigns;

/**
 * A concrete, parameterized traffic sign block. Replaces the hundreds of single-method subclasses
 * that previously only overrode {@link #getBlockRegistryName()}.
 */
public class BlockTrafficSign extends AbstractBlockSign {

  /**
   * ThreadLocal used to pass the registry name to the superclass constructor. The AbstractBlock
   * constructor calls getBlockRegistryName() before subclass fields are initialized, so we
   * store the name here before calling super() and read it in getBlockRegistryName().
   */
  private static final ThreadLocal<String> PENDING_REGISTRY_NAME = new ThreadLocal<>();

  private final String registryName;

  public BlockTrafficSign(String registryName) {
    this(initRegistryName(registryName), registryName);
  }

  private BlockTrafficSign(Void ignored, String registryName) {
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
