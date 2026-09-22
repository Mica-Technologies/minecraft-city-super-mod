package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import javax.annotation.Nonnull;
import net.minecraft.util.BlockRenderLayer;

/**
 * A vandal-resistant, wet-location exit sign: a sign inside a thick clear polycarbonate shield on
 * a gasketed back, for garages, stairwells and outdoors. Mounted to a wall or ceiling only.
 *
 * @since 2026.9
 */
public class BlockExitSignVandalResistant extends AbstractBlockExitSign {

  /** Package-private so {@code ExitSignSpecTest} can check its state count. */
  static final ExitSignSpec SPEC = ExitSignSpec.builder()
      .mounts(Mount.WALL, Mount.CEILING)
      .heads(Heads.NONE)
      .preset(Letters.RED, Housing.WHITE)
      .preset(Letters.GREEN, Housing.BLACK)
      .build();

  @Override
  public ExitSignSpec getSpec() {
    return SPEC;
  }

  /** The shield stands three quarters of a pixel out round the sign. */
  @Override
  protected double getHousingMargin() {
    return 0.75;
  }

  @Override
  protected double[] getBodyDepth(boolean wall) {
    return wall ? new double[]{12.5, 16} : new double[]{5.5, 10.5};
  }

  /** Translucent, for the clear shield; the sign under it is cutout and draws fine here too. */
  @Nonnull
  @Override
  public BlockRenderLayer getBlockRenderLayer() {
    return BlockRenderLayer.TRANSLUCENT;
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_vandal_resistant";
  }
}
