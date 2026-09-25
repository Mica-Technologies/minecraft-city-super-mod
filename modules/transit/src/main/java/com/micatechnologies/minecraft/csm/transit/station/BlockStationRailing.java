package com.micatechnologies.minecraft.csm.transit.station;

import com.micatechnologies.minecraft.csm.codeutils.AbstractBlockFence;
import com.micatechnologies.minecraft.csm.transit.fare.BlockFareGate;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

/**
 * A station railing: a vanilla fence in behaviour -- it joins other iron railings, the service
 * gate (a fence gate), the globe lamp's post and the solid side of a block, stands a fence's
 * height to jump, and takes a lead -- drawn as a post with, toward each neighbour it joins, rails
 * and balusters ({@code gen_transit_stations.py}). Two styles: the stair entrance's painted cast
 * iron with ball finials, and the fare line's stainless at the fare gates' cabinet height.
 *
 * <p>A fare line railing also joins the fare gates. A gate stands one block up with its cabinet
 * drawn down into the floor-level cell, so the railing looks past the air there: it joins a gate
 * whose box reaches down over the next cell, one above it or up to two further along (the ADA
 * gates are wider than their cell), as long as the gate's lane runs across the railing rather
 * than along it.</p>
 *
 * @since 2026.9
 */
public class BlockStationRailing extends AbstractBlockFence {

  private static final ThreadLocal<String> PENDING = new ThreadLocal<>();

  private final String registryName;
  private final boolean fareLine;

  /**
   * Constructs a railing.
   *
   * @param registryName its registry name
   * @param fareLine     whether it is the fare line's, which also joins the fare gates
   */
  public BlockStationRailing(String registryName, boolean fareLine) {
    super(stash(registryName), SoundType.METAL, "pickaxe", 1, 3.0F, 10.0F, 0.0F, 0);
    this.registryName = registryName;
    this.fareLine = fareLine;
    PENDING.remove();
  }

  private static Material stash(String registryName) {
    PENDING.set(registryName);
    return Material.IRON;
  }

  @Override
  public String getBlockRegistryName() {
    return registryName != null ? registryName : PENDING.get();
  }

  @Override
  public boolean canConnectTo(IBlockAccess world, BlockPos pos, EnumFacing facing) {
    if (super.canConnectTo(world, pos, facing)) {
      return true;
    }
    return fareLine && reachesGate(world, pos, facing.getOpposite());
  }

  /**
   * Whether a fare gate's cabinet fills {@code cell}, the floor-level cell beside the railing in
   * direction {@code dir}, with its lane running across the railing.
   */
  private static boolean reachesGate(IBlockAccess world, BlockPos cell, EnumFacing dir) {
    for (int k = 0; k <= 2; k++) {
      BlockPos at = cell.offset(dir, k).up();
      IBlockState state = world.getBlockState(at);
      if (!(state.getBlock() instanceof BlockFareGate)) {
        continue;
      }
      if (state.getValue(BlockFareGate.FACING).getAxis() == dir.getAxis()) {
        return false;
      }
      AxisAlignedBB box = state.getBoundingBox(world, at).offset(at);
      double x = cell.getX() + 0.5;
      double y = cell.getY() + 0.5;
      double z = cell.getZ() + 0.5;
      return box.minX < x && x < box.maxX && box.minY < y && y < box.maxY
          && box.minZ < z && z < box.maxZ;
    }
    return false;
  }

  @Override
  public boolean getBlockIsOpaqueCube(IBlockState state) {
    return false;
  }
}
