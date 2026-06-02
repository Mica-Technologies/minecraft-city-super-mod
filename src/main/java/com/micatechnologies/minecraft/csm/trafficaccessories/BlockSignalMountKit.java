package com.micatechnologies.minecraft.csm.trafficaccessories;

import java.util.Random;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A {@link BlockTrafficAccessoryNSEW} for the signal mount kit brackets (vertical / horizontal /
 * tall / 90-degree variants) that hold a signal head one block in front of them.
 *
 * <p>On top of the NSEW behaviour (facing + color cycling) this adds a lightweight random tick that
 * auto-corrects the kit's facing toward an adjacent signal head. This recovers the orientation of
 * kits placed before the NSEWUD&rarr;NSEW metadata port (commit d93c3231) shifted their stored
 * facing. See {@link SignalMountKitOrientation} for the details and why only the facing is fixed.
 *
 * <p>{@code signalpolemount2} stays on the plain {@link BlockTrafficAccessoryNSEW}: it is a
 * pole clamp with a different geometry, not a signal-holding bracket, so it must not self-orient.
 */
public class BlockSignalMountKit extends BlockTrafficAccessoryNSEW {

  public BlockSignalMountKit(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
    // Enables randomTick; correction is rare and cheap (a handful of neighbor TE lookups).
    // TODO (remove after Dec 2026): transitional migration aid — see SignalMountKitOrientation.
    setTickRandomly(true);
  }

  @Override
  public void randomTick(World worldIn, BlockPos pos, IBlockState state, Random random) {
    super.randomTick(worldIn, pos, state, random);
    if (!worldIn.isRemote) {
      SignalMountKitOrientation.correctFacing(worldIn, pos, state, FACING);
    }
  }
}
