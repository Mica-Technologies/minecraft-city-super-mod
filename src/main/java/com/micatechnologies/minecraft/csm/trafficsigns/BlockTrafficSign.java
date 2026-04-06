package com.micatechnologies.minecraft.csm.trafficsigns;

/**
 * A concrete, parameterized traffic sign block. Replaces the hundreds of single-method subclasses
 * that previously only overrode {@link #getBlockRegistryName()}.
 */
public class BlockTrafficSign extends AbstractBlockSign {

  private final String registryName;

  public BlockTrafficSign(String registryName) {
    this.registryName = registryName;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName;
  }
}
