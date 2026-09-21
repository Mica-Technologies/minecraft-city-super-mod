package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;

/**
 * A compact exit sign and emergency light in one: the sign always carries a lamp head on each
 * end, square LED or round, which lights when mains power is lost.
 *
 * @since 2026.9
 */
public class BlockExitSignCombo extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .heads(Heads.SQUARE, Heads.ROUND)
      .preset(Letters.RED, Housing.WHITE, Heads.ROUND)
      .preset(Letters.RED, Housing.BLACK, Heads.SQUARE)
      .preset(Letters.GREEN, Housing.WHITE, Heads.ROUND)
      .preset(Letters.GREEN, Housing.BLACK, Heads.SQUARE)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_combo_compact";
  }
}
