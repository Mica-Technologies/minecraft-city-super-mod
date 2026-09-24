package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.appliance.ApplianceSpec;
import com.micatechnologies.minecraft.csm.furniture.residential.BlockBuiltInAppliance;
import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A backyard grill that cooks ({@link OutdoorAppliances}): the gas grill on its cart and the
 * charcoal kettle grill. It works as the kitchen's range does -- right-click opens its screen,
 * food in cooks out, hoppers feed it -- standing free in no run. While it cooks
 * ({@link #RUNNING}) the seam under its lid glows, it sizzles, and smoke curls up from its vent.
 *
 * @since 2026.9
 */
public class BlockOutdoorGrill extends BlockBuiltInAppliance {

  /** Where the smoke leaves the lid, facing north, in sixteenths: {x, y, z}. */
  private final double[] vent;

  /**
   * Constructs a grill.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param spec         what it cooks and how
   * @param vent         where its smoke rises from, facing north, in sixteenths: {x, y, z}
   */
  public BlockOutdoorGrill(String registryName, int[] box, ApplianceSpec spec, double[] vent) {
    super(registryName, box, spec, null);
    this.vent = vent.clone();
  }

  /** Smoke from the vent while it cooks. */
  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    if (!state.getValue(RUNNING)) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    double[] w = BounceCastleLayout.point(vent[0], vent[2], facing);
    for (int i = 0; i < 4; i++) {
      double x = pos.getX() + (w[0] + (rand.nextDouble() - 0.5) * 2.5) / 16.0;
      double z = pos.getZ() + (w[1] + (rand.nextDouble() - 0.5) * 2.5) / 16.0;
      double y = pos.getY() + (vent[1] + rand.nextDouble()) / 16.0;
      world.spawnParticle(i == 0 ? EnumParticleTypes.SMOKE_LARGE : EnumParticleTypes.SMOKE_NORMAL,
          x, y, z, 0.0, 0.03 + rand.nextDouble() * 0.02, 0.0);
    }
  }
}
