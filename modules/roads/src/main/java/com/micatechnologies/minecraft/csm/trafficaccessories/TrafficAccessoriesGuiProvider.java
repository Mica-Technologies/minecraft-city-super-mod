package com.micatechnologies.minecraft.csm.trafficaccessories;

import com.micatechnologies.minecraft.csm.codeutils.gui.ICsmGuiProvider;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.BlockSpanWireHangerMount;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.SpanWireMountConfigGui;
import com.micatechnologies.minecraft.csm.trafficaccessories.spanwire.TileEntitySpanWireHanger;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Supplies the traffic accessories package's GUI screens: the message signs, the variable speed
 * limits, the lane control signal, the dynamic guide and street signs and the span wire hanger
 * mount.
 *
 * @version 1.0
 * @since 2026.9
 */
public class TrafficAccessoriesGuiProvider implements ICsmGuiProvider {

  /**
   * {@inheritDoc}
   *
   * @since 1.0
   */
  @Nullable
  @Override
  public Object getClientGuiElement(int id, EntityPlayer player, World world, BlockPos pos) {
    TileEntity tileEntity = world.getTileEntity(pos);
    Object returnValue = null;
    if (id == 9 && tileEntity instanceof TileEntityPortableMessageSign) {
      returnValue = new BlockPortableMessageSignGui((TileEntityPortableMessageSign) tileEntity);
    } else if (id == 10 && tileEntity instanceof TileEntityOverheadMessageSign) {
      returnValue = new BlockOverheadMessageSignGui((TileEntityOverheadMessageSign) tileEntity);
    } else if (id == 11 && tileEntity instanceof TileEntityVariableSpeedLimit) {
      returnValue = new BlockPortableSpeedLimitGui((TileEntityVariableSpeedLimit) tileEntity);
    } else if (id == 12 && tileEntity instanceof TileEntityOverheadSpeedLimit) {
      returnValue = new BlockOverheadSpeedLimitGui((TileEntityOverheadSpeedLimit) tileEntity);
    } else if (id == 13 && tileEntity instanceof TileEntityLaneControlSignal) {
      returnValue = new LaneControlSignalConfigGui((TileEntityLaneControlSignal) tileEntity);
    } else if (id == 14 && tileEntity instanceof TileEntityDynamicGuideSign) {
      returnValue = new DynamicGuideSignGui((TileEntityDynamicGuideSign) tileEntity);
    } else if (id == BlockDynamicStreetSign.GUI_ID
        && tileEntity instanceof TileEntityDynamicStreetSign) {
      returnValue = new DynamicStreetSignGui((TileEntityDynamicStreetSign) tileEntity);
    } else if (id == BlockSpanWireHangerMount.GUI_ID
        && tileEntity instanceof TileEntitySpanWireHanger) {
      returnValue = new SpanWireMountConfigGui((TileEntitySpanWireHanger) tileEntity);
    } else if (id == 18 && tileEntity instanceof TileEntityPoleMountSpeedLimit) {
      returnValue = new BlockPoleMountSpeedLimitGui((TileEntityPoleMountSpeedLimit) tileEntity);
    }
    return returnValue;
  }
}
