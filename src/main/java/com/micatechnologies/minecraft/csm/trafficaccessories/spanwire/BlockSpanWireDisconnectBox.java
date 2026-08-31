package com.micatechnologies.minecraft.csm.trafficaccessories.spanwire;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * The disconnect box hung on a span, where the lashed conductors break out to feed the signals.
 *
 * <p>Every span of any size carries one — it is the pale box conspicuous in photographs, usually
 * a little away from the signals themselves. Without it a span reads as signals fed by nothing.
 *
 * <p>An <b>attachment</b> rather than a payload: it clamps to the messenger in its own right and
 * gets a mast and clamp like any mount, rather than hanging from one. That is also what real
 * ones do, and it means the box can sit on a stretch of span with no signal near it.
 */
public class BlockSpanWireDisconnectBox extends BlockSpanWireHangerMount {

  public BlockSpanWireDisconnectBox(String registryName, AxisAlignedBB boundingBox,
      BlockRenderLayer renderLayer, float hardness, boolean fullCube) {
    super(registryName, boundingBox, renderLayer, hardness, fullCube);
  }

  /**
   * Keeps its model when linked. Unlike the bracket mounts, this block's model <em>is</em> the
   * thing hanging on the wire -- hiding it would delete the disconnect box rather than tidy away
   * a duplicate bracket.
   */
  @Override
  protected boolean hidesModelWhenLinked() {
    return false;
  }

  @Override
  public Class<? extends TileEntity> getTileEntityClass() {
    return TileEntitySpanWireDisconnectBox.class;
  }

  @Override
  public String getTileEntityName() {
    return "tileentityspanwiredisconnectbox";
  }

  @Override
  public TileEntity createNewTileEntity(World worldIn, int meta) {
    return new TileEntitySpanWireDisconnectBox();
  }
}
