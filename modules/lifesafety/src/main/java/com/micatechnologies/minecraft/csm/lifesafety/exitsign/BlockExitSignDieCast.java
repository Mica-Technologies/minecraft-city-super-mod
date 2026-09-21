package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;

/**
 * A die-cast aluminium exit sign, the heavier metal fixture of older and higher-end buildings: a
 * cast body and a stencil-cut face in brushed aluminium, black or white.
 *
 * @since 2026.9
 */
public class BlockExitSignDieCast extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .housings(Housing.BRUSHED, Housing.BLACK, Housing.WHITE)
      .heads(Heads.NONE)
      .preset(Letters.RED, Housing.BRUSHED)
      .preset(Letters.GREEN, Housing.BRUSHED)
      .preset(Letters.RED, Housing.BLACK)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_diecast";
  }
}
