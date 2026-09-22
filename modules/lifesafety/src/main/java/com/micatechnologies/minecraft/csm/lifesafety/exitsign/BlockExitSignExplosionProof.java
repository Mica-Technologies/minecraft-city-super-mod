package com.micatechnologies.minecraft.csm.lifesafety.exitsign;

import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Heads;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Housing;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Letters;
import com.micatechnologies.minecraft.csm.lifesafety.exitsign.ExitSignConfig.Mount;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

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

  /** A pixel lower than a plain sign, so its frame and conduit hub fit under the ceiling. */
  @Override
  protected double getFaceBottom() {
    return 3.5;
  }

  /** The cast frame is a pixel wide all round. */
  @Override
  protected double getHousingMargin() {
    return 1;
  }

  /** The deep cast body, its frame standing proud of the face and its bolts proud of the frame. */
  @Override
  protected double[] getBodyDepth(boolean wall) {
    return wall ? new double[]{11, 16} : new double[]{5, 11};
  }

  /** The conduit hub reaches the top of the block, whether the sign hangs from it or not. */
  @Override
  public AxisAlignedBB getBlockBoundingBox(IBlockState state, IBlockAccess source,
      BlockPos pos) {
    AxisAlignedBB box = super.getBlockBoundingBox(state, source, pos);
    return new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX, 1, box.maxZ);
  }

  @Override
  public String getBlockRegistryName() {
    return "exit_sign_explosion_proof";
  }
}
