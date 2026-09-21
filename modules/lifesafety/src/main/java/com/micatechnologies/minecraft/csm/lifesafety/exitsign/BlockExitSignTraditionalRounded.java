package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;

/**
 * A thermoplastic exit sign with rounded corners and a slightly domed face, in white or black,
 * with every mount and optional emergency heads.
 *
 * @since 2026.9
 */
public class BlockExitSignTraditionalRounded extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .preset(Letters.RED, Housing.WHITE)
      .preset(Letters.GREEN, Housing.WHITE)
      .preset(Letters.RED, Housing.BLACK)
      .preset(Letters.GREEN, Housing.BLACK)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_traditional_rounded";
  }
}
