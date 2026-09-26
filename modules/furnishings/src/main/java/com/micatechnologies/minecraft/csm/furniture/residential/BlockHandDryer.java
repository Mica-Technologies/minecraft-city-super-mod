package com.micatechnologies.minecraft.csm.furniture.residential;

import com.micatechnologies.minecraft.csm.codeutils.ICsmSound;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * A commercial restroom's hand dryer on the wall: the classic warm-air dryer with its push
 * button and a nozzle turned down, or the blade dryer the hands are held in. A right-click
 * runs it: its synthesised run sound and a gust of air from its outlet. Nothing is stored and
 * nothing ticks.
 *
 * @since 2026.9
 */
public class BlockHandDryer extends BlockBathroomFixture {

  private final double[] outlet;

  /**
   * Constructs a hand dryer.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   * @param runSound     the sound of it running
   * @param outlet       where the air comes out, {x, y, z} facing north in sixteenths
   */
  public BlockHandDryer(String registryName, int[] box, ICsmSound runSound, double[] outlet) {
    super(registryName, box, FixtureMaterial.METAL, runSound, 1.0F);
    this.outlet = outlet.clone();
  }

  @Override
  public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
      EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    boolean used = super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY,
        hitZ);
    if (used && world instanceof WorldServer) {
      double x = outlet[0] / 16.0 - 0.5;
      double z = outlet[2] / 16.0 - 0.5;
      // Turn (x, z) from north to the block's facing, as the blockstate's y does.
      for (int i = 0; i < (state.getValue(FACING).getHorizontalIndex() + 2) % 4; i++) {
        double t = x;
        x = -z;
        z = t;
      }
      ((WorldServer) world).spawnParticle(EnumParticleTypes.CLOUD, pos.getX() + 0.5 + x,
          pos.getY() + outlet[1] / 16.0, pos.getZ() + 0.5 + z, 8, 0.06, 0.04, 0.06, 0.01);
    }
    return used;
  }
}
