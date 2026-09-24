package com.micatechnologies.minecraft.csm.furniture.outdoor;

import com.micatechnologies.minecraft.csm.furniture.residential.LampSwitching;
import java.util.Random;
import javax.annotation.Nonnull;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * A brick chimney stack, stacked a block at a time as high as the roof needs: only the top block
 * of a stack ({@link #UP} false, actual state) is drawn with its concrete crown and two clay pots.
 * Lit ({@link BlockFirePit#LIT}, by a click with an empty hand or flint and steel on any block of
 * the stack, or by a change of redstone power to any of them, so a fireplace's light switch can
 * work it too) the whole stack is lit, and smoke rises from the pots. It gives no light.
 *
 * @since 2026.9
 */
public class BlockChimney extends BlockFirePit {

  /** Another chimney block stands on this one. */
  public static final PropertyBool UP = PropertyBool.create("up");

  /** The tallest stack a click or a change of power runs through. */
  private static final int MAX_STACK = 32;
  /** The pots' mouths, facing north, in sixteenths: {x, y, z}. */
  private static final double[][] POTS = {{5, 19.8, 8}, {11, 19.8, 8}};

  /**
   * Constructs a chimney stack.
   *
   * @param registryName its registry name, ending in its finish
   * @param box          its box facing north, in sixteenths
   */
  public BlockChimney(String registryName, int[] box) {
    super(registryName, box, false, 0, 0);
    setDefaultState(getDefaultState().withProperty(UP, false));
  }

  @Override
  @Nonnull
  protected BlockStateContainer createBlockState() {
    return new BlockStateContainer(this, FACING, LIT,
        LampSwitching.POWERED, UP);
  }

  @Override
  @Nonnull
  @SuppressWarnings("deprecation")
  public IBlockState getActualState(@Nonnull IBlockState state, IBlockAccess world, BlockPos pos) {
    return super.getActualState(state, world, pos)
        .withProperty(UP, world.getBlockState(pos.up()).getBlock() == this);
  }

  /** Lights or puts out the whole stack the block is in. */
  @Override
  protected void setLit(World world, BlockPos pos, IBlockState state, boolean lit) {
    BlockPos bottom = pos;
    for (int i = 0; i < MAX_STACK && world.getBlockState(bottom.down()).getBlock() == this; i++) {
      bottom = bottom.down();
    }
    BlockPos at = bottom;
    for (int i = 0; i < MAX_STACK; i++) {
      IBlockState s = at.equals(pos) ? state : world.getBlockState(at);
      if (s.getBlock() != this) {
        break;
      }
      if (s.getValue(LIT) != lit || at.equals(pos)) {
        world.setBlockState(at, s.withProperty(LIT, lit), 3);
      }
      at = at.up();
    }
  }

  /** Smoke from the pots of a lit stack's top block. */
  @Override
  @SideOnly(Side.CLIENT)
  public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
    if (!state.getValue(LIT) || world.getBlockState(pos.up()).getBlock() == this) {
      return;
    }
    EnumFacing facing = state.getValue(FACING);
    for (double[] pot : POTS) {
      double[] w = BounceCastleLayout.point(pot[0], pot[2], facing);
      for (int i = 0; i < 3; i++) {
        double x = pos.getX() + (w[0] + (rand.nextDouble() - 0.5) * 1.5) / 16.0;
        double z = pos.getZ() + (w[1] + (rand.nextDouble() - 0.5) * 1.5) / 16.0;
        double y = pos.getY() + (pot[1] + rand.nextDouble() * 2) / 16.0;
        world.spawnParticle(EnumParticleTypes.SMOKE_LARGE, x, y, z,
            (rand.nextDouble() - 0.5) * 0.01, 0.05 + rand.nextDouble() * 0.03,
            (rand.nextDouble() - 0.5) * 0.01);
      }
    }
  }
}
