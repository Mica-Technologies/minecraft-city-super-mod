package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * A wire mount that carries a cluster of two to four signals from one point on the span.
 *
 * <p>A separate block rather than an option on the existing mounts: those two are single-signal
 * mounts by shape and by registry name, and existing builds rely on that.
 *
 * <p>Everything about taking part in a span it inherits from {@link BlockSpanWireHangerMount},
 * including the configuration screen -- which grows a cluster width row when the mount it opens
 * on is one of these.
 */
public class BlockSpanWireClusterMount extends BlockSpanWireHangerMount {

  public BlockSpanWireClusterMount(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpanWireClusterMount.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspanwireclustermount";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpanWireClusterMount();
  }
}
