package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;

/**
 * An explosion-proof exit sign for hazardous locations: a heavy cast body with a guarded lens and
 * a threaded conduit hub. Mounted to a wall or hung from a ceiling only.
 *
 * @since 2026.9
 */
public class BlockExitSignExplosionProof extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .housings(Housing.BRUSHED)
      .mounts(Mount.WALL, Mount.CEILING)
      .heads(Heads.NONE)
      .preset(Letters.RED, Housing.BRUSHED)
      .preset(Letters.GREEN, Housing.BRUSHED)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_explosion_proof";
  }
}
