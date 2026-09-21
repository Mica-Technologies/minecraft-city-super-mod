package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;

/**
 * A self-luminous exit sign, photoluminescent or tritium: a thin panel that needs no power, so it
 * ignores redstone and glows faintly all the time.
 *
 * @since 2026.9
 */
public class BlockExitSignPhotoluminescent extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .mounts(Mount.WALL, Mount.CEILING)
      .heads(Heads.NONE)
      .unpowered()
      .lightValue(4)
      .preset(Letters.GREEN, Housing.WHITE)
      .preset(Letters.RED, Housing.WHITE)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_photoluminescent";
  }
}
